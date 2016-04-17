package logbook.pushbullet.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import logbook.internal.gui.WindowController;
import logbook.plugin.PluginContainer;
import logbook.plugin.gui.MainExtMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PushbulletConfigMenu implements MainExtMenu {
    @Override
    public MenuItem getContent() {
        MenuItem item = new MenuItem("Pushbullet...");
        item.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(PluginContainer.getInstance().getClassLoader()
                        .getResource("logbook/pushbullet/gui/pushbullet_config.fxml"));
                loader.setClassLoader(this.getClass().getClassLoader());
                Stage stage = new Stage();
                Parent root = loader.load();
                stage.setScene(new Scene(root));

                WindowController controller = loader.getController();
                controller.setWindow(stage);

                stage.initOwner(((MenuItem) e.getSource()).getParentPopup().getOwnerWindow());
                stage.setTitle("Pushbulletの設定");
                stage.show();
            } catch (Exception ex) {
                LoggerHolder.LOG.error("設定の初期化に失敗しました", ex);
            }
        });
        return item;
    }

    private static class LoggerHolder {
        /**
         * ロガー
         */
        private static final Logger LOG = LogManager.getLogger(PushbulletConfigMenu.class);
    }
}