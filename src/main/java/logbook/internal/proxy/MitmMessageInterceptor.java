package logbook.internal.proxy;

import io.appium.mitmproxy.InterceptedMessage;
import logbook.internal.LoggerHolder;
import logbook.internal.ThreadManager;
import logbook.plugin.PluginServices;
import logbook.proxy.ContentListenerSpi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * mitmproxy-javaのinterceptorとしてlistenerに通信内容を通知する
 */
final public class MitmMessageInterceptor {
    /**
     * リスナー
     */
    private List<ContentListenerSpi> listeners = null;

    public void intercept(InterceptedMessage m) {
        try {
            if (this.listeners == null) {
                this.listeners = PluginServices.instances(ContentListenerSpi.class).collect(Collectors.toList());
            }

            RequestMetaDataWrapper req = createRequestMetaDataWrapper(m.getRequest());
            ResponseMetaDataWrapper res = createResponseMetaDataWrapper(m.getResponse());

            for (ContentListenerSpi listener : this.listeners) {
                if (listener.test(req)) {
                    Runnable task = () -> {
                        try {
                            listener.accept(req, res);
                        } catch (Exception e) {
                            LoggerHolder.get().warn("MitmMessageInterceptorで例外が発生", e);
                        }
                    };
                    ThreadManager.getExecutorService().submit(task);
                }
            }
        } catch (Exception e) {
            LoggerHolder.get().warn("MitmMessageInterceptorで例外が発生 req=" + m.getRequest().getUrl(), e);
        }
    }

    private static RequestMetaDataWrapper createRequestMetaDataWrapper(InterceptedMessage.Request req) throws RuntimeException {
        RequestMetaDataWrapper wrapper = new RequestMetaDataWrapper();

        URI uri = URI.create(req.getUrl());

        wrapper.setContentType(getContentType(req.getHeaders()));
        wrapper.setMethod(req.getMethod());
        wrapper.setRequestURI(uri.getPath());
        wrapper.setQueryString(uri.getQuery());

        byte[] body = req.getBody();
        if (body != null) {
            wrapper.set(new ByteArrayInputStream(body));
        } else {
            // set(InputStream body) をスキップして直接に空のデータをセット
            wrapper.setParameterMap(new HashMap<>());
            wrapper.setRequestBody(Optional.empty());
        }

        return wrapper;
    }

    private static ResponseMetaDataWrapper createResponseMetaDataWrapper(InterceptedMessage.Response res) throws IOException {
        ResponseMetaDataWrapper wrapper = new ResponseMetaDataWrapper();

        wrapper.setStatus(res.getStatusCode());
        wrapper.setContentType(getContentType(res.getHeaders()));

        byte[] body = res.getBody();
        wrapper.set(new ByteArrayInputStream(body));

        return wrapper;
    }

    private static String getContentType(List<String[]> headers) {
        for (String[] header : headers) {
            if (header.length == 2) {
                if (header[0].equalsIgnoreCase("content-type")) {
                    return header[1];
                }
            }

        }

        return null;
    }
}
