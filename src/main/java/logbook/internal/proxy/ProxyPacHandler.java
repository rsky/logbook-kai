package logbook.internal.proxy;

import logbook.bean.AppConfig;
import logbook.internal.LoggerHolder;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * proxy.pac をホスティングするハンドラ
 */
final public class ProxyPacHandler extends Handler.Abstract {
    private static final String PROXY_PAC_PATH = "/logbook-kai/proxy.pac";

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        if (!shouldHandle(request)) {
            return false;
        }

        try {
            final String pacScript = ProxyPacGenerator.getPacScript(AppConfig.get().getListenPort());
            response.setStatus(200);
            response.getHeaders().add("Content-Type", "application/x-ns-proxy-autoconfig");
            response.write(true, StandardCharsets.UTF_8.encode(pacScript), callback);
        } catch (IOException e) {
            LoggerHolder.get().error("Failed to generate proxy.pac", e);
            Response.writeError(request, response, callback, HttpStatus.INTERNAL_SERVER_ERROR_500);
        }

        return true;
    }

    private static boolean shouldHandle(Request req) {
        return HttpMethod.GET.is(req.getMethod()) && req.getHttpURI().getPath().equals(PROXY_PAC_PATH);
    }
}
