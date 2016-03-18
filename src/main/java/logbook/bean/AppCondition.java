package logbook.bean;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import logbook.internal.Config;
import logbook.internal.log.BattleLog;

/**
 * 出撃などの状態
 *
 */
public class AppCondition implements Serializable {

    private static final long serialVersionUID = 4744529121773992869L;

    /** 連合艦隊 */
    private Boolean combinedFlag = Boolean.FALSE;

    /** 連合艦隊 (0=未結成, 1=機動部隊, 2=水上部隊, 3=輸送部隊, -x=解隊(-1=機動部隊, -2=水上部隊)) */
    private Integer combinedType = 0;

    /** 出撃中 */
    private Boolean mapStart = Boolean.FALSE;

    /** 出撃艦隊 */
    private Integer deckId = 0;

    /** 最後の戦闘結果 */
    private BattleLog battleResult;

    /** 退避艦ID */
    private Set<Integer> escape = new HashSet<>();

    /**
     * 連合艦隊を取得します。
     * @return 連合艦隊
     */
    public Boolean getCombinedFlag() {
        return this.combinedFlag;
    }

    /**
     * 連合艦隊を設定します。
     * @param combinedFlag 連合艦隊
     */
    public void setCombinedFlag(Boolean combinedFlag) {
        this.combinedFlag = combinedFlag;
    }

    /**
     * 連合艦隊 (0=未結成, 1=機動部隊, 2=水上部隊, 3=輸送部隊, -x=解隊(-1=機動部隊, -2=水上部隊))を取得します。
     * @return 連合艦隊 (0=未結成, 1=機動部隊, 2=水上部隊, 3=輸送部隊, -x=解隊(-1=機動部隊, -2=水上部隊))
     */
    public Integer getCombinedType() {
        return this.combinedType;
    }

    /**
     * 連合艦隊 (0=未結成, 1=機動部隊, 2=水上部隊, 3=輸送部隊, -x=解隊(-1=機動部隊, -2=水上部隊))を設定します。
     * @param combinedType 連合艦隊 (0=未結成, 1=機動部隊, 2=水上部隊, 3=輸送部隊, -x=解隊(-1=機動部隊, -2=水上部隊))
     */
    public void setCombinedType(Integer combinedType) {
        this.combinedType = combinedType;
    }

    /**
     * 出撃中を取得します。
     * @return 出撃中
     */
    public Boolean getMapStart() {
        return this.mapStart;
    }

    /**
     * 出撃中を設定します。
     * @param mapStart 出撃中
     */
    public void setMapStart(Boolean mapStart) {
        this.mapStart = mapStart;
    }

    /**
     * 出撃艦隊を取得します。
     * @return 出撃艦隊
     */
    public Integer getDeckId() {
        return this.deckId;
    }

    /**
     * 出撃艦隊を設定します。
     * @param deckId 出撃艦隊
     */
    public void setDeckId(Integer deckId) {
        this.deckId = deckId;
    }

    /**
     * 最後の戦闘結果を取得します。
     * @return 最後の戦闘結果
     */
    public BattleLog getBattleResult() {
        return this.battleResult;
    }

    /**
     * 最後の戦闘結果を設定します。
     * @param battleResult 最後の戦闘結果
     */
    public void setBattleResult(BattleLog battleResult) {
        this.battleResult = battleResult;
    }

    /**
     * 退避艦IDを取得します。
     * @return 退避艦ID
     */
    public Set<Integer> getEscape() {
        return this.escape;
    }

    /**
     * 退避艦IDを設定します。
     * @param escape 退避艦ID
     */
    public void setEscape(Set<Integer> escape) {
        this.escape = escape;
    }

    /**
     * アプリケーションのデフォルト設定ディレクトリから{@link AppCondition}を取得します、
     * これは次の記述と同等です
     * <blockquote>
     *     <code>Config.getDefault().get(AppCondition.class, AppCondition::new)</code>
     * </blockquote>
     *
     * @return {@link AppCondition}
     */
    public static AppCondition get() {
        return Config.getDefault().get(AppCondition.class, AppCondition::new);
    }
}