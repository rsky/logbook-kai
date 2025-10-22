package logbook.internal.proxy;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import logbook.Messages;
import logbook.bean.AppConfig;
import logbook.internal.LoggerHolder;
import logbook.internal.gui.InternalFXMLLoader;
import logbook.proxy.ProxyServerSpi;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.BindException;
import java.net.ServerSocket;

/**
 * プロキシサーバー実装。
 * パッシブモードのHTTPサーバーを起動し、必要に応じてmitmproxy(mitmdump)も起動します。
 * mitmproxyを利用する際はmitmdumpがlistenPortで待ち受け、
 * HTTPサーバーは内部通信用に空いているポートで待ち受けます。
 */
public final class ProxyServerImpl implements ProxyServerSpi {
    @Override
    public void run() {
        // TCPポートが使用中かチェック
        final int listenPort = AppConfig.get().getListenPort();
        if (!isPortAvailable(listenPort)) {
            showPortAlreadyUsed(listenPort);
            return;
        }

        final boolean useMitmproxy = AppConfig.get().isUseMitmproxy();
        // mitmproxyを使うとき、通信用に空きポートを確保
        int internalPort = 0;
        if (useMitmproxy) {
            try {
                internalPort = findAvailablePort();
            } catch (IOException e){
                LoggerHolder.get().warn("Exception occurred while searching for available port", e);
                internalPort = 8765; // 便宜上fallback。通常は到達しない。
            }
        }

        this.runServer(listenPort, internalPort, useMitmproxy);
    }

    private void runServer(int listenPort, int internalPort, boolean useMitmproxy) {
        final Server server = new Server();

        try (final ServerConnector connector = new ServerConnector(server)) {
            if (useMitmproxy) {
                connector.setPort(internalPort);
                connector.setHost("localhost");
            } else {
                connector.setPort(listenPort);
                if (AppConfig.get().isAllowOnlyFromLocalhost()) {
                    connector.setHost("localhost");
                }
            }
            server.setConnectors(new Connector[]{connector});
            server.setHandler(new PassiveModeHandler());

            final MitmLauncher mitmLauncher;
            if (useMitmproxy) {
                String listenHost = null;
                if (AppConfig.get().isAllowOnlyFromLocalhost()) {
                    listenHost = "127.0.0.1";
                }
                mitmLauncher = new MitmLauncher(AppConfig.get().getMitmdumpPath(), listenPort, listenHost, internalPort);
            } else {
                mitmLauncher = null;
            }

            try {
                runAndWaitServer(server, mitmLauncher);
            } catch (Exception e) {
                handleException(e);
            }
        } catch (Exception e) {
            LoggerHolder.get().fatal("Failed to start HTTP server", e);
        }
    }

    private void runAndWaitServer(Server server, MitmLauncher mitmLauncher) throws Exception {
        try {
            server.start();
            if (mitmLauncher != null) {
                mitmLauncher.start();
            }
            server.join();
        } finally {
            if (mitmLauncher != null) {
                try {
                    if (mitmLauncher.getMitmPid() > 0) {
                        // Windowsのみ起動した孫プロセスが生き続ける問題があるので手動で始末する
                        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                            try {
                                ProcessBuilder p = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(mitmLauncher.getMitmPid()));
                                p.start().wait();
                            } catch (Exception exc) {
                                LoggerHolder.get().warn("Exception occurred while terminating mitmproxy child process", exc);
                            }
                        }
                    }
                    mitmLauncher.stop();
                } catch (Exception ex) {
                    LoggerHolder.get().warn("Exception occurred during mitmproxy server shutdown", ex);
                }
            }
            try {
                server.stop();
            } catch (Exception ex) {
                LoggerHolder.get().warn("Exception occurred during HTTP server shutdown", ex);
            }
        }
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void showPortAlreadyUsed(int port) {
        showAlert("ポートが使用中です",
                "ポート " + port + " は既に使用されています。\n" +
                        "設定画面でポート番号を変更するか、他のアプリケーションを終了してください。",
                null);
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void handleException(Exception e) {
        // Title
        String title = Messages.getString("ProxyServer.7"); //$NON-NLS-1$
        // Message
        StringBuilder sb = new StringBuilder(Messages.getString("ProxyServer.8")); //$NON-NLS-1$
        if (e instanceof BindException) {
            sb.append("\n"); //$NON-NLS-1$
            sb.append(Messages.getString("ProxyServer.10")); //$NON-NLS-1$
        }
        String message = sb.toString();
        // StackTrace
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        String stackTrace = w.toString();

        showAlert(title, message, stackTrace);
    }

    private static void showAlert(String title, String message, String extraContent) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.WARNING);
            alert.getDialogPane().getStylesheets().add("logbook/gui/application.css");
            InternalFXMLLoader.setGlobal(alert.getDialogPane());
            if (extraContent != null) {
                TextArea textArea = new TextArea(extraContent);
                alert.getDialogPane().setExpandableContent(textArea);
            }

            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
