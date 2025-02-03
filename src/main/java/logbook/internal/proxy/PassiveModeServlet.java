package logbook.internal.proxy;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import logbook.internal.LoggerHolder;
import logbook.internal.ThreadManager;
import logbook.plugin.PluginServices;
import logbook.proxy.ContentListenerSpi;
import logbook.proxy.RequestMetaData;
import logbook.proxy.ResponseMetaData;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * 外部からのHTTP POSTリクエストを受け付けてListenerを呼び出すサーブレット
 * mitmproxyのようなSSL/TLS証明書をサポートするプロキシがフックしたデータをこちらに送信することを想定している
 */
public final class PassiveModeServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = -7501576705342327540L;

    public static final String PATH_SPEC = "/pasv/*";
    private static final String PATH_PREFIX = "/pasv/";

    private static final String REQUEST_METHOD_HEADER = "X-Pasv-Request-Method";
    private static final String REQUEST_CONTENT_TYPE_HEADER = "X-Pasv-Request-Content-Type";
    private static final String REQUEST_BODY_HEADER = "X-Pasv-Request-Body";

    /**
     * リスナー
     */
    private transient List<ContentListenerSpi> listeners = null;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RequestParsingResult result = new RequestParser().parse(req);

        if (this.invoke(result.getRequestMetaDataWrapper(), result.getResponseMetaDataWrapper())) {
            resp.setStatus(200);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().println("OK");
        } else {
            resp.sendError(500);
        }
    }

    private boolean invoke(RequestMetaDataWrapper req, ResponseMetaDataWrapper res) {
        try {
            if (this.listeners == null) {
                this.listeners = PluginServices.instances(ContentListenerSpi.class).collect(Collectors.toList());
            }
            for (ContentListenerSpi listener : this.listeners) {
                if (listener.test(req)) {
                    Runnable task = () -> {
                        try {
                            listener.accept(req, res);
                        } catch (Exception e) {
                            LoggerHolder.get().warn("パッシブモード サーブレットで例外が発生", e);
                        }
                    };
                    ThreadManager.getExecutorService().submit(task);
                }
            }
            return true;
        } catch (Exception e) {
            LoggerHolder.get().warn("パッシブモード サーブレットで例外が発生 req=" + req.getRequestURI(), e);
            return false;
        }
    }

    static final class RequestParser {
        RequestParser() {
        }

        RequestParsingResult parse(HttpServletRequest req) throws IOException, RuntimeException {
            return new RequestParsingResult(this.createRequestMetaDataWrapper(req), this.createResponseMetaDataWrapper(req));
        }

        private RequestMetaDataWrapper createRequestMetaDataWrapper(HttpServletRequest req) throws RuntimeException {
            RequestMetaDataWrapper wrapper = new RequestMetaDataWrapper();

            wrapper.setContentType(req.getHeader(REQUEST_CONTENT_TYPE_HEADER));
            wrapper.setMethod(req.getHeader(REQUEST_METHOD_HEADER));
            wrapper.setQueryString(req.getQueryString());

            String requestUri = req.getRequestURI();
            if (requestUri.startsWith(PATH_PREFIX)) {
                wrapper.setRequestURI(requestUri.substring(PATH_PREFIX.length()-1));
            } else {
                throw new RuntimeException("Unexpected request URI: " + requestUri);
            }

            String b64body = req.getHeader(REQUEST_BODY_HEADER);
            if (b64body != null) {
                byte[] body = Base64.getDecoder().decode(b64body);
                wrapper.set(new ByteArrayInputStream(body));
            } else {
                // set(InputStream body) をスキップして直接に空のデータをセット
                wrapper.setParameterMap(new HashMap<>());
                wrapper.setRequestBody(Optional.empty());
            }

            return wrapper;
        }

        private ResponseMetaDataWrapper createResponseMetaDataWrapper(HttpServletRequest req) throws IOException {
            ResponseMetaDataWrapper wrapper = new ResponseMetaDataWrapper();

            // Listenerで見てないから200固定にしておく
            wrapper.setStatus(200);
            // Request BodyのContent-Typeそのまま
            wrapper.setContentType(req.getContentType());

            // Request Bodyのデータを取得
            // inputをそのままsetすると、"java.io.IOException: mark/reset not supported" が発生するので
            // 一度bytes[]に読み込んでからsetする
            InputStream input = req.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            // Content-Encoding: gzipの場合はset()内で自動的に展開される
            wrapper.set(new ByteArrayInputStream(baos.toByteArray()));

            return wrapper;
        }
    }

    static final class RequestParsingResult {
        private final RequestMetaDataWrapper req;
        private final ResponseMetaDataWrapper res;

        RequestParsingResult(RequestMetaDataWrapper req, ResponseMetaDataWrapper res) {
            this.req = req;
            this.res = res;
        }

        RequestMetaDataWrapper getRequestMetaDataWrapper() {
            return this.req;
        }

        ResponseMetaDataWrapper getResponseMetaDataWrapper() {
            return this.res;
        }
    }

    static class RequestMetaDataWrapper implements RequestMetaData, Cloneable {

        private String contentType;

        private String method;

        private Map<String, List<String>> parameterMap;

        private String queryString;

        private String requestURI;

        private Optional<InputStream> requestBody;

        @Override
        public String getContentType() {
            return this.contentType;
        }

        void setContentType(String contentType) {
            this.contentType = contentType;
        }

        @Override
        public String getMethod() {
            return this.method;
        }

        void setMethod(String method) {
            this.method = method;
        }

        @Override
        public Map<String, List<String>> getParameterMap() {
            return this.parameterMap;
        }

        void setParameterMap(Map<String, List<String>> parameterMap) {
            this.parameterMap = parameterMap;
        }

        @Override
        public String getQueryString() {
            return this.queryString;
        }

        void setQueryString(String queryString) {
            this.queryString = queryString;
        }

        @Override
        public String getRequestURI() {
            return this.requestURI;
        }

        void setRequestURI(String requestURI) {
            this.requestURI = requestURI;
        }

        @Override
        public Optional<InputStream> getRequestBody() {
            return this.requestBody;
        }

        void setRequestBody(Optional<InputStream> requestBody) {
            this.requestBody = requestBody;
        }

        void set(HttpServletRequest req) {
            this.setContentType(req.getContentType());
            this.setMethod(req.getMethod().toString());
            this.setQueryString(req.getQueryString());
            this.setRequestURI(req.getRequestURI());
        }

        void set(InputStream body) {
            String bodystr;
            try (Reader reader = new InputStreamReader(body, StandardCharsets.UTF_8)) {
                int len;
                char[] cbuf = new char[128];
                StringBuilder sb = new StringBuilder();
                while ((len = reader.read(cbuf)) > 0) {
                    sb.append(cbuf, 0, len);
                }
                bodystr = URLDecoder.decode(sb.toString(), "UTF-8");
            } catch (IOException e) {
                bodystr = "";
            }
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (String part : bodystr.split("&")) {
                String key;
                String value;
                int idx = part.indexOf('=');
                if (idx > 0) {
                    key = part.substring(0, idx);
                    value = part.substring(idx + 1);
                } else {
                    key = part;
                    value = null;
                }
                map.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(value);
            }
            this.setParameterMap(map);
            this.setRequestBody(Optional.of(body));
        }

        @Override
        public RequestMetaDataWrapper clone() {
            RequestMetaDataWrapper clone = new RequestMetaDataWrapper();
            clone.setContentType(this.getContentType());
            clone.setMethod(this.getMethod());
            clone.setQueryString(this.getQueryString());
            clone.setRequestURI(this.getRequestURI());
            clone.setParameterMap(this.getParameterMap());
            clone.setRequestBody(this.getRequestBody());
            return clone;
        }
    }

    static class ResponseMetaDataWrapper implements ResponseMetaData, Cloneable {

        private int status;

        private String contentType;

        private Optional<InputStream> responseBody;

        @Override
        public int getStatus() {
            return this.status;
        }

        void setStatus(int status) {
            this.status = status;
        }

        @Override
        public String getContentType() {
            return this.contentType;
        }

        void setContentType(String contentType) {
            this.contentType = contentType;
        }

        @Override
        public Optional<InputStream> getResponseBody() {
            return this.responseBody;
        }

        void setResponseBody(Optional<InputStream> responseBody) {
            this.responseBody = responseBody;
        }

        void set(HttpServletResponse res) {
            this.setStatus(res.getStatus());
            this.setContentType(res.getContentType());
        }

        void set(InputStream body) throws IOException {
            this.setResponseBody(Optional.of(ungzip(body)));
        }

        @Override
        public ResponseMetaDataWrapper clone() {
            ResponseMetaDataWrapper clone = new ResponseMetaDataWrapper();
            clone.setStatus(this.getStatus());
            clone.setContentType(this.getContentType());
            clone.setResponseBody(this.getResponseBody());
            return clone;
        }

        private static InputStream ungzip(InputStream body) throws IOException {
            body.mark(Short.BYTES);
            int magicbyte = body.read() << 8 ^ body.read();
            body.reset();
            if (magicbyte == 0x1f8b) {
                return new GZIPInputStream(body);
            }
            return body;
        }
    }
}
