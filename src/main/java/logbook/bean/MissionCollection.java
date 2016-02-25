package logbook.bean;

import java.util.HashMap;
import java.util.Map;

import logbook.internal.Config;

/**
 * 遠征のコレクション
 *
 */
public class MissionCollection {

    /** 遠征 */
    private Map<Integer, Mission> missionMap = new HashMap<>();

    /**
     * 遠征を取得します。
     * @return 遠征
     */
    public Map<Integer, Mission> getMissionMap() {
        return this.missionMap;
    }

    /**
     * 遠征を設定します。
     * @param missionMap 遠征
     */
    public void setMissionMap(Map<Integer, Mission> missionMap) {
        this.missionMap = missionMap;
    }

    /**
     * アプリケーションのデフォルト設定ディレクトリから<code>MissionCollection</code>を取得します、
     * これは次の記述と同等です
     * <blockquote>
     *     <code>Config.getDefault().get(MissionCollection.class)</code>
     * </blockquote>
     *
     * @return <code>MissionCollection</code>
     */
    public static MissionCollection get() {
        return Config.getDefault().get(MissionCollection.class, MissionCollection::new);
    }
}
