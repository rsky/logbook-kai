package logbook.internal.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

final public class MitmLauncher {
    private final String mitmdumpPath;
    private final int listenPort;
    private final String listenHost;
    private final int logbookPort;
    private Process process = null;

    public MitmLauncher(String mitmdumpPath, int listenPort, String listenHost, int logbookPort) {
        this.mitmdumpPath = mitmdumpPath;
        this.listenPort = listenPort;
        this.listenHost = listenHost;
        this.logbookPort = logbookPort;
    }

    public void start() throws IOException {
        // python script file is zipped inside our jar. extract it into a temporary file.
        final String pythonScriptPath = extractPythonScriptToFile();

        final List<String> args = new ArrayList<>();
        args.add(mitmdumpPath);
        args.add("--anticache");
        args.add("-q");
        args.add("-p");
        args.add(String.valueOf(listenPort));
        if (listenHost != null) {
            args.add("--listen-host");
            args.add(listenHost);
        }
        args.add("-s");
        args.add(pythonScriptPath);
        args.add("--set");
        args.add("logbook_port=" + logbookPort);

        process = (new ProcessBuilder(args))
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
    }

    public void stop() {
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    /**
     * This method is taken from mitmproxy-java
     *
     * @see <a href="https://github.com/appium/mitmproxy-java/blob/master/src/main/java/io/appium/mitmproxy/MitmproxyJava.java">Original Source</a>
     */
    private String extractPythonScriptToFile() throws IOException {
        File outfile = File.createTempFile("mitmproxy-logbook-kai-addon", ".py");

        try (
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("scripts/proxy.py");
                FileOutputStream outputStream = new FileOutputStream(outfile)) {

            inputStream.transferTo(outputStream);
        }

        return outfile.getCanonicalPath();
    }
}
