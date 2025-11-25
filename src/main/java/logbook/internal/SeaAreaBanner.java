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
    private static final int MAX_SEA_AREA_NUMBER = 14;

    /**
     * 出撃札の画像パスを取得します
     *
     * @param area 海域(札)番号
     * @return 出撃札の画像パス。存在しない場合はnull
     */
    static Path getJoinBannerPath(int area) {
        if (area < 1 || area > MAX_SEA_AREA_NUMBER) {
            return null;
        }

        // 2025年秋イベント後段作戦バージョン
        // 不規則な番号に対しては定数化も行わない
        final int imageNumber = switch (area) {
            case 1 -> 4;    // 改R4計画艦隊
            case 10 -> 5;   // 礼号作戦部隊
            case 11 -> 7;   // 横須賀防備戦隊
            case 12 -> 9;   // 連合艦隊
            case 13 -> 11;  // 第百四戦隊
            case 14 -> 13;  // 決戦艦隊
            // 札2~9 -> 16, 18, ..., 28, 30
            default -> IMAGE_NUMBER_BASE_OFFSET + area * IMAGE_NUMBER_MULTIPLIER;
        };

        return Paths.get("common", COMMON_EVENT, String.format("%s_%d.png", COMMON_EVENT, imageNumber));
    }
}
