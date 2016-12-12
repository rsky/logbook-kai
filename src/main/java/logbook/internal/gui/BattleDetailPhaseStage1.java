package logbook.internal.gui;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import logbook.bean.BattleTypes.Stage1;

/**
 * Stage1 詳細
 *
 */
public class BattleDetailPhaseStage1 extends VBox {

    private Stage1 stage1;

    private String friendName;

    @FXML
    private Label fName;

    @FXML
    private Label fCount;

    @FXML
    private Label fCountAfter;

    @FXML
    private Label fLostcount;

    @FXML
    private Label eName;

    @FXML
    private Label eCount;

    @FXML
    private Label eCountAfter;

    @FXML
    private Label eLostcount;

    /**
    * Stage1 詳細
    * @param stage1 stage1
    * @param friendName 味方の名前(僚艦/基地航空隊 等)
    */
    public BattleDetailPhaseStage1(Stage1 stage1, String friendName) {
        this.stage1 = stage1;
        this.friendName = friendName;
        try {
            FXMLLoader loader = InternalFXMLLoader.load("logbook/gui/battle_detail_phase_stage1.fxml");
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
        } catch (IOException e) {
            LoggerHolder.LOG.error("FXMLのロードに失敗しました", e);
        }
    }

    @FXML
    void initialize() {
        if (this.stage1 != null) {
            this.fName.setText(this.friendName);

            if (this.stage1.getFCount() != null && this.stage1.getFLostcount() != null) {
                this.fCount.setText(Integer.toString(this.stage1.getFCount()));
                this.fCountAfter.setText(Integer.toString(this.stage1.getFCount() - this.stage1.getFLostcount()));
                this.fLostcount.setText(Integer.toString(this.stage1.getFLostcount()));
            }
            if (this.stage1.getECount() != null && this.stage1.getELostcount() != null) {
                this.eCount.setText(Integer.toString(this.stage1.getECount()));
                this.eCountAfter.setText(Integer.toString(this.stage1.getECount() - this.stage1.getELostcount()));
                this.eLostcount.setText(Integer.toString(this.stage1.getELostcount()));
            }
        }
    }

    private static class LoggerHolder {
        /** ロガー */
        private static final Logger LOG = LogManager.getLogger(BattleDetailPhaseStage1.class);
    }
}
