package logbook.internal.proxy;

import logbook.internal.LoggerHolder;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import org.zeroturnaround.process.ProcessUtil;
import org.zeroturnaround.process.Processes;
import org.zeroturnaround.process.SystemProcess;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * mitmproxy (mitmdump) launcher.
 *
 * <p>This implementation is inspired by
 * <a href="https://github.com/appium/mitmproxy-java">mitmproxy-java</a>
 * which is licensed under the Apache License 2.0.
 *
 * @see <a href="https://github.com/appium/mitmproxy-java/blob/master/src/main/java/io/appium/mitmproxy/MitmproxyJava.java">Original Source</a>
 */
@Slf4j
final public class MitmLauncher {
    private final String mitmdumpPath;
    private final int listenPort;
    private final String listenHost;
    private final int logbookPort;
    private StartedProcess process = null;
    private String pidFilePath = null;

    public MitmLauncher(String mitmdumpPath, int listenPort, String listenHost, int logbookPort) {
        this.mitmdumpPath = mitmdumpPath;
        this.listenPort = listenPort;
        this.listenHost = listenHost;
        this.logbookPort = logbookPort;
    }

    public void start() throws IOException {
        // python script file is zipped inside our jar. extract it into a temporary file.
        final String pythonScriptPath = extractPythonScriptToFile();
        pidFilePath = createPidFile();

        final List<String> args = buildCommandArgs(pythonScriptPath);

        process = new ProcessExecutor()
                .command(args)
                .redirectOutput(Slf4jStream.ofCaller().asInfo())
                .redirectErrorStream(true)
                .destroyOnExit()
                .start();
    }

    private List<String> buildCommandArgs(String pythonScriptPath) {
        final List<String> args = new ArrayList<>();
        args.add(mitmdumpPath);

        // 出力を抑制する
        args.add("-q");

        // ホスト・ポート・アドオンスクリプト
        args.add("-p");
        args.add(String.valueOf(listenPort));
        if (listenHost != null) {
            args.add("--listen-host");
            args.add(listenHost);
        }
        args.add("-s");
        args.add(pythonScriptPath);

        // アドオンのオプション
        args.add("--set");
        args.add("logbook_port=" + logbookPort);
        if (pidFilePath != null) {
            args.add("--set");
            args.add("pid_file=" + pidFilePath);
        }

        return args;
    }

    public void stop() throws IOException, InterruptedException, TimeoutException {
        if (process != null) {
            SystemProcess p = Processes.newStandardProcess(process.getProcess());
            ProcessUtil.destroyGracefullyOrForcefullyAndWait(p, 5, TimeUnit.SECONDS, 5, TimeUnit.SECONDS);
            process = null;
        }
        pidFilePath = null;
    }

    public int getMitmPid() {
        if (pidFilePath == null) {
            return -1;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(pidFilePath))) {
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                return -1;
            }
            return Integer.parseInt(line.trim());
        } catch (IOException | NumberFormatException e) {
            LoggerHolder.get().error("Exception occurred while trying to get mitm pid", e);
            return -1;
        }
    }

    private String createPidFile() throws IOException {
        File pidFile = File.createTempFile("mitmproxy-pid-", ".txt");
        pidFile.deleteOnExit();

        return pidFile.getCanonicalPath();
    }

    private String extractPythonScriptToFile() throws IOException {
        File outfile = File.createTempFile("mitmproxy-addon-", ".py");
        outfile.deleteOnExit();

        try (
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("scripts/proxy.py");
                FileOutputStream outputStream = new FileOutputStream(outfile)) {

            inputStream.transferTo(outputStream);
        }

        return outfile.getCanonicalPath();
    }
}
