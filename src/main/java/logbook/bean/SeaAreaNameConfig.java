package logbook.bean;

import logbook.internal.Config;
import lombok.Data;

import java.io.Serializable;

@Data
public class SeaAreaNameConfig implements Serializable {
    private String area1 = "識別札1";
    private String area2 = "識別札2";
    private String area3 = "識別札3";
    private String area4 = "識別札4";
    private String area5 = "識別札5";
    private String area6 = "識別札6";
    private String area7 = "識別札7";
    private String area8 = "識別札8";
    private String area9 = "識別札9";
    private String area10 = "識別札10";

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
