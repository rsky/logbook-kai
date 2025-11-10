package logbook.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 出撃札
 */
final class SeaAreaBanner {
    private static final String COMMON_EVENT = "common_event";

    private static final int IMAGE_NUMBER_BASE_OFFSET = 12;
    private static final int IMAGE_NUMBER_MULTIPLIER = 2;
    // 以下の定数は、計算で求められない例外的なパターンに適用する
    private static final int IMAGE_NUMBER_AREA_1 = 4;  // 改R4計画艦隊
    private static final int IMAGE_NUMBER_AREA_10 = 5; // 礼号作戦部隊

    /**
     * 出撃札の画像パスを取得します
     *
     * @param area 海域(札)番号
     * @return 出撃札の画像パス。存在しない場合はnull
     */
    static Path getJoinBannerPath(int area) {
        if (area < 1 || area > 10) {
            return null;
        }

        // 2025年秋イベント後段作戦バージョン
        final int imageNumber = switch (area) {
            case 1 -> IMAGE_NUMBER_AREA_1;
            case 10 -> IMAGE_NUMBER_AREA_10;
            default -> IMAGE_NUMBER_BASE_OFFSET + area * IMAGE_NUMBER_MULTIPLIER; // 札2~9 -> 14, 16, ..., 28, 30
        };

        return Paths.get("common", COMMON_EVENT, String.format("%s_%d.png", COMMON_EVENT, imageNumber));
    }
}
