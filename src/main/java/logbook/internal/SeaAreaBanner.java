package logbook.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 出撃札
 */
class SeaAreaBanner {
    private static final String COMMON_EVENT = "common_event";

    /**
     * 出撃札の画像パスを取得します
     *
     * @param area 海域(札)番号
     * @return 出撃札の画像パス。存在しない場合はnull
     */
    static Path getJoinBannerPath(int area) {
        if (area < 1) {
            return null;
        }

        // 2024年夏イベント後段作戦まで
        int imageNumber = 2 + area * 2;

        return Paths.get("common", COMMON_EVENT, String.format("%s_%d.png", COMMON_EVENT, imageNumber));
    }
}
