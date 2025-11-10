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
     * HPに入ってくる可能性がまずないであろう値
     * "N/A" -> "\x4e\x2f\x41" -> 0x4e2f41 -> 5123905 -> -1x
     */
    public static final int NOT_AVAILABLE_VALUE = -5123905;

    public static boolean isNotAvailable(int hp) {
        return hp == NOT_AVAILABLE_VALUE;
    }

    public static boolean isNotAvailable(Chara chara) {
        return isNotAvailable(chara.getMaxhp());
    }
}
