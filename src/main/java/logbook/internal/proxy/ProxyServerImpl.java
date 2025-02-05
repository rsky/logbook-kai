package logbook.internal.proxy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.BindException;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import logbook.Messages;
import logbook.bean.AppConfig;
import logbook.internal.LoggerHolder;
import logbook.internal.gui.InternalFXMLLoader;
import logbook.proxy.ProxyServerSpi;

/**
 * HTTPサーバーです。
 * 歴史的経緯によりProxyを名乗っていますが、プロキシ機能はありません。
 */
public final class ProxyServerImpl implements ProxyServerSpi {
    @Override
    public void run() {
        final Server server = new Server();

        try (final ServerConnector connector = new ServerConnector(server)) {
            connector.setPort(AppConfig.get().getListenPort());
            if (AppConfig.get().isAllowOnlyFromLocalhost()) {
                connector.setHost("localhost");
            }
            server.setConnectors(new Connector[]{connector});

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            ServletHolder passive = new ServletHolder(new PassiveModeServlet());
            passive.setInitParameter("maxThreads", "128");
            passive.setInitParameter("timeout", "300000");
            context.addServlet(passive, PassiveModeServlet.PATH_SPEC);
            server.setHandler(context);

            try {
                try {
                    server.start();
                    server.join();
                } finally {
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

        Runnable runnable = () -> {
            Alert alert = new Alert(AlertType.WARNING);
            alert.getDialogPane().getStylesheets().add("logbook/gui/application.css");
            InternalFXMLLoader.setGlobal(alert.getDialogPane());
            TextArea textArea = new TextArea(stackTrace);
            alert.getDialogPane().setExpandableContent(textArea);

            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        };

        Platform.runLater(runnable);
    }
}
