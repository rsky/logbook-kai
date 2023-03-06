package logbook.internal;

import logbook.bean.Chara;

/**
 * 敵HP
 */
public class EnemyHp {
    /**
     * APIレスポンスに含まれている形式
     */
    public static final String NOT_AVAILABLE = "N/A";

    /**
     * "N/A"を内部的に識別するための値
     */
    public static final int NOT_AVAILABLE_VALUE = -128;

    public static boolean isNotAvailable(int hp) {
        return hp == NOT_AVAILABLE_VALUE;
    }

    public static boolean isNotAvailable(Chara chara) {
        return isNotAvailable(chara.getMaxhp());
    }
}
