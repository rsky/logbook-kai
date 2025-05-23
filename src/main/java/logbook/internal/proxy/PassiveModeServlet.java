package logbook.internal.proxy;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import logbook.internal.LoggerHolder;
import logbook.internal.ThreadManager;
import logbook.internal.proxy.ReverseProxyServlet.RequestMetaDataWrapper;
import logbook.internal.proxy.ReverseProxyServlet.ResponseMetaDataWrapper;
import logbook.plugin.PluginServices;
import logbook.proxy.ContentListenerSpi;

import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
}
