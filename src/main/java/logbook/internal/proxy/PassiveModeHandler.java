package logbook.internal.proxy;

import logbook.internal.LoggerHolder;
import logbook.internal.ThreadManager;
import logbook.plugin.PluginServices;
import logbook.proxy.ContentListenerSpi;
import lombok.Getter;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 外部からのHTTP POSTリクエストを受け付けてListenerを呼び出すハンドラ
 * mitmproxyのようなSSL/TLS証明書をサポートするプロキシがフックしたデータをこちらに送信することを想定している
 */
final public class PassiveModeHandler extends Handler.Abstract {
    private static final String PATH_PREFIX = "/pasv/";
    private static final String PID_PATH_PREFIX = "/pasv/";

    private static final String REQUEST_METHOD_HEADER = "X-Pasv-Request-Method";
    private static final String REQUEST_CONTENT_TYPE_HEADER = "X-Pasv-Request-Content-Type";
    private static final String REQUEST_BODY_HEADER = "X-Pasv-Request-Body";

    /**
     * リスナー
     */
    private List<ContentListenerSpi> listeners = null;

    /**
     * mitmdumpのプロセスID
     */
    @Getter
    private int mitmPid = -1;

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (shouldUpdateMitmPid(request)) {
            this.updateMitmPid(request.getHttpURI().getPath());
            return true;
        }

        if (!shouldHandle(request)) {
            return false;
        }

        RequestParsingResult result = new RequestParser().parse(request);
        if (this.invoke(result.getRequestMetaDataWrapper(), result.getResponseMetaDataWrapper())) {
            response.setStatus(200);
            callback.succeeded();
        } else {
            response.setStatus(500);
            callback.failed(new Exception("Internal Server Error"));
        }
        return true;
    }

    private static boolean shouldHandle(Request req) {
        return HttpMethod.POST.is(req.getMethod()) && req.getHttpURI().getPath().startsWith(PATH_PREFIX);
    }

    private static boolean shouldUpdateMitmPid(Request req) {
        return HttpMethod.PUT.is(req.getMethod()) && req.getHttpURI().getPath().startsWith(PID_PATH_PREFIX);
    }

    private void updateMitmPid(String pathContainsPid) {
        Pattern pattern = Pattern.compile("/pid/(\\d+)");
        Matcher matcher = pattern.matcher(pathContainsPid);

        if (matcher.find()) {
            this.mitmPid = Integer.parseInt(matcher.group(1));
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
                            LoggerHolder.get().warn("PassiveModeHandlerで例外が発生", e);
                        }
                    };
                    ThreadManager.getExecutorService().submit(task);
                }
            }
            return true;
        } catch (Exception e) {
            LoggerHolder.get().warn("PassiveModeHandlerで例外が発生 req=" + req.getRequestURI(), e);
            return false;
        }
    }

    private static final class RequestParser {
        RequestParser() {
        }

        RequestParsingResult parse(Request req) throws IOException, RuntimeException {
            return new RequestParsingResult(this.createRequestMetaDataWrapper(req), this.createResponseMetaDataWrapper(req));
        }

        private RequestMetaDataWrapper createRequestMetaDataWrapper(Request req) throws RuntimeException {
            RequestMetaDataWrapper wrapper = new RequestMetaDataWrapper();

            wrapper.setContentType(req.getHeaders().get(REQUEST_CONTENT_TYPE_HEADER));
            wrapper.setMethod(req.getHeaders().get(REQUEST_METHOD_HEADER));
            wrapper.setRequestURI(req.getHttpURI().getPath().substring(PATH_PREFIX.length() - 1));
            wrapper.setQueryString(req.getHttpURI().getQuery());

            String b64body = req.getHeaders().get(REQUEST_BODY_HEADER);
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

        private ResponseMetaDataWrapper createResponseMetaDataWrapper(Request req) throws IOException {
            ResponseMetaDataWrapper wrapper = new ResponseMetaDataWrapper();

            // Listenerで見てないから200固定にしておく
            wrapper.setStatus(200);
            // Request BodyのContent-Typeそのまま
            wrapper.setContentType(req.getHeaders().get(HttpHeader.CONTENT_TYPE));

            // Request Bodyのデータを取得
            // inputをそのままsetすると、"java.io.IOException: mark/reset not supported" が発生するので
            // 一度bytes[]に読み込んでからsetする
            try (InputStream input = Request.asInputStream(req)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = input.read(buffer)) > -1) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();
                // Content-Encoding: gzipの場合はset()内で自動的に展開される
                wrapper.set(new ByteArrayInputStream(baos.toByteArray()));
            }

            return wrapper;
        }
    }

    private record RequestParsingResult(RequestMetaDataWrapper req, ResponseMetaDataWrapper res) {

        RequestMetaDataWrapper getRequestMetaDataWrapper() {
            return this.req;
        }

        ResponseMetaDataWrapper getResponseMetaDataWrapper() {
            return this.res;
        }
    }
}
