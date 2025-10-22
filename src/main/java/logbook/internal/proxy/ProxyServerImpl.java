package logbook.internal.proxy;

import io.appium.mitmproxy.InterceptedMessage;
import io.appium.mitmproxy.MitmproxyJava;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プロキシサーバーです。
 * 設定に応じてパッシブモードのHTTPサーバーまたはmitmproxy(mtimdump)を起動します。
 */
public final class ProxyServerImpl implements ProxyServerSpi {
    @Override
    public void run() {
        final boolean usePassiveMode = AppConfig.get().isUsePassiveMode();
        final boolean useMitmproxy = AppConfig.get().isUseMitmproxy();
        if (usePassiveMode) {
            if (useMitmproxy) {
                showAlert("設定に問題があります", "設定でパッシブモードとmitmproxyの両方が有効になっています。mitmproxyは無視されます。", null);
            }
            this.runServer(true);
        } else if (useMitmproxy) {
            this.runServer(false);
        } else {
            showAlert("サーバーを起動できませんでした", "設定からパッシブモードかmitmproxyのどちらかを有効にしてください。", null);
        }
    }

    private void runServer(boolean passiveMode) {
        // TCPポートが使用中かチェック
        final int port = AppConfig.get().getListenPort();
        if (!isPortAvailable(port)) {
            showPortAlreadyUsed(port);
            return;
        }
        if (passiveMode) {
            final int webSocketPort = 8765;
            if (!isPortAvailable(webSocketPort)) {
                showPortAlreadyUsed(webSocketPort);
                return;
            }
        }

        final Server server = new Server();
        MitmproxyJava proxy = null;

        final ConcurrentHashMap<String, Integer> pidMap = new ConcurrentHashMap<>();

        try (final ServerConnector connector = new ServerConnector(server)) {
            if (passiveMode) {
                // 指定されたポートでパッシブモードのHTTPサーバが待ち受ける
                connector.setPort(port);
                if (AppConfig.get().isAllowOnlyFromLocalhost()) {
                    connector.setHost("localhost");
                }
                server.setConnectors(new Connector[]{connector});
                server.setHandler(new PassiveModeHandler());
            } else {
                // 指定されたポートでmitmproxyが待ち受ける
                // serverはproxyを終了させないようにjoin()するためにランダムなポートで待ち受ける
                final List<String> extraMitmproxyParams = new ArrayList<>();
                extraMitmproxyParams.add("--quiet");
                if (AppConfig.get().isAllowOnlyFromLocalhost()) {
                    extraMitmproxyParams.add("--listen-host");
                    extraMitmproxyParams.add("127.0.0.1");
                }

                final MitmMessageInterceptor interceptor = new MitmMessageInterceptor();

                proxy = new MitmproxyJava(AppConfig.get().getMitmdumpPath(), (InterceptedMessage m) -> {
                    // Windowsでは起動したmitmdumpの子プロセスが終了しない問題があるので、
                    // ワークアラウンドとしてアドオンからPIDを送らせて、手動で始末する。
                    if (m.getRequest().getMethod().equals("PID")) {
                        pidMap.put("PID", m.getResponse().getStatusCode());
                    } else {
                        interceptor.intercept(m);
                    }
                    // レスポンスを改変しないのでnullを返す
                    return null;
                }, port, extraMitmproxyParams);
            }

            try {
                try {
                    if (proxy != null) {
                        proxy.start();
                    }
                    server.start();
                    server.join();
                } finally {
                    if (proxy != null) {
                        try {
                            if (pidMap.containsKey("PID")) {
                                ProcessBuilder p = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pidMap.get("PID")));
                                p.start().exitValue();
                            }
                            proxy.stop();
                        } catch (Exception ex) {
                            LoggerHolder.get().warn("MitmproxyJavaサーバーのシャットダウンで例外", ex);
                        }
                    }
                    try {
                        server.stop();
                    } catch (Exception ex) {
                        LoggerHolder.get().warn("Logbook-Kai HTTPサーバーのシャットダウンで例外", ex);
                    }
                }
            } catch (Exception e) {
                handleException(e);
            }
        } catch (Exception e) {
            LoggerHolder.get().fatal("Logbook-Kai HTTPサーバーの起動に失敗しました", e);
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
