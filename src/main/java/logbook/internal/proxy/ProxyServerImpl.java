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
import java.net.Socket;

/**
 * プロキシサーバー実装。
 * パッシブモードのHTTPサーバーを起動し、必要に応じてmitmproxy(mitmdump)も起動します。
 * mitmproxyを利用する際はmitmdumpがlistenPortで待ち受け、
 * HTTPサーバーは内部通信用に空いているポートで待ち受けます。
 */
public final class ProxyServerImpl implements ProxyServerSpi {
    private static final String LOCAL_ADDRESS = "127.0.0.1";

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
            } catch (IOException e) {
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
                connector.setHost(LOCAL_ADDRESS);
            } else {
                connector.setPort(listenPort);
                if (AppConfig.get().isAllowOnlyFromLocalhost()) {
                    connector.setHost(LOCAL_ADDRESS);
                }
            }
            server.setConnectors(new Connector[]{connector});
            server.setHandler(new PassiveModeHandler());

            final MitmLauncher mitmLauncher;
            if (useMitmproxy) {
                final String mitmdumpPath = AppConfig.get().getMitmdumpPath();
                final String listenHost = AppConfig.get().isAllowOnlyFromLocalhost() ? LOCAL_ADDRESS : null;
                final boolean outputEnabled = AppConfig.get().isEnableMitmdumpOutput();
                mitmLauncher = new MitmLauncher(mitmdumpPath, listenPort, listenHost, internalPort, outputEnabled);
            } else {
                mitmLauncher = null;
            }

            try {
                runAndWaitServer(server, mitmLauncher, internalPort);
            } catch (Exception e) {
                handleException(e);
            }
        } catch (Exception e) {
            LoggerHolder.get().fatal("Failed to start HTTP server", e);
        }
    }

    private void runAndWaitServer(Server server, MitmLauncher mitmLauncher, int internalPort) throws Exception {
        try {
            server.start();
            if (mitmLauncher != null) {
                waitForInternalPortListeningOn(internalPort);
                mitmLauncher.start();
            }
            server.join();
        } finally {
            if (mitmLauncher != null) {
                try {
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

    private static void waitForInternalPortListeningOn(int internalPort) {
        // 100ms間隔で最大10回(合計1秒まで)LOCAL_ADDRESS:internalPortに接続できるのを待つ
        final int maxAttempts = 10;
        final int intervalMs = 100;

        for (int i = 0; i < maxAttempts; i++) {
            try (Socket socket = new Socket(LOCAL_ADDRESS, internalPort)) {
                // 接続成功 - サーバーがリッスンしている
                return;
            } catch (IOException e) {
                // 接続失敗 - まだサーバーが起動していない
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LoggerHolder.get().warn("Interrupted while waiting for internal port", ie);
                    return;
                }
            }
        }
        LoggerHolder.get().warn("Internal port {} did not start listening within timeout", internalPort);
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
