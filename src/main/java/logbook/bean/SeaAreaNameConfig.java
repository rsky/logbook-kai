package logbook.bean;

import logbook.internal.Config;
import logbook.internal.SeaArea;
import lombok.Data;

import java.io.Serializable;

@Data
public class SeaAreaNameConfig implements Serializable {
    private static final long serialVersionUID = -3429972632602581322L;

    private String area1 = SeaArea.識別札1.getName();
    private String area2 = SeaArea.識別札2.getName();
    private String area3 = SeaArea.識別札3.getName();
    private String area4 = SeaArea.識別札4.getName();
    private String area5 = SeaArea.識別札5.getName();
    private String area6 = SeaArea.識別札6.getName();
    private String area7 = SeaArea.識別札7.getName();
    private String area8 = SeaArea.識別札8.getName();
    private String area9 = SeaArea.識別札9.getName();
    private String area10 = SeaArea.識別札10.getName();
    private String area11 = SeaArea.識別札11.getName();
    private String area12 = SeaArea.識別札12.getName();

    public String getAreaName(int area) {
        switch (area) {
            case 1:
                return this.area1;
            case 2:
                return this.area2;
            case 3:
                return this.area3;
            case 4:
                return this.area4;
            case 5:
                return this.area5;
            case 6:
                return this.area6;
            case 7:
                return this.area7;
            case 8:
                return this.area8;
            case 9:
                return this.area9;
            case 10:
                return this.area10;
            case 11:
                return this.area11;
            case 12:
                return this.area12;
            default:
                return null;
        }
    }

    /**
     * アプリケーションのデフォルト設定ディレクトリからアプリケーション設定を取得します、
     * これは次の記述と同等です
     * <blockquote>
     *     <code>Config.getDefault().get(SeaAreaNameConfig.class, SeaAreaNameConfig::new)</code>
     * </blockquote>
     *
     * @return アプリケーションの設定
     */
    public static SeaAreaNameConfig get() {
        return Config.getDefault().get(SeaAreaNameConfig.class, SeaAreaNameConfig::new);
    }
}
