package logbook.internal.proxy;

import logbook.plugin.PluginServices;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

final public class ProxyPacGenerator {
    public static String getPacScript(int listenPort) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = PluginServices.getResourceAsStream("logbook/proxy.pac")) {
            Objects.requireNonNull(in).transferTo(out);
        }
        return out.toString().replace("{port}", String.valueOf(listenPort));
    }
}
