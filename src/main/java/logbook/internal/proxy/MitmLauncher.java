package logbook.internal.proxy;

import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import org.zeroturnaround.process.ProcessUtil;
import org.zeroturnaround.process.Processes;
import org.zeroturnaround.process.SystemProcess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
final public class MitmLauncher {
    private final String mitmdumpPath;
    private final int listenPort;
    private final String listenHost;
    private final int logbookPort;
    private StartedProcess process = null;

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

        process = new ProcessExecutor()
                .command(args)
                .redirectOutput(Slf4jStream.ofCaller().asInfo())
                .redirectErrorStream(true)
                .destroyOnExit()
                .start();
    }

    public void stop() throws IOException, InterruptedException, TimeoutException {
        if (process != null) {
            SystemProcess p = Processes.newStandardProcess(process.getProcess());
            ProcessUtil.destroyGracefullyOrForcefullyAndWait(p, 5, TimeUnit.SECONDS, 5, TimeUnit.SECONDS);
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
