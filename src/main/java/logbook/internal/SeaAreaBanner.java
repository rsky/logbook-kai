package logbook.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 出撃札
 */
final class SeaAreaBanner {
    private static final String COMMON_EVENT = "common_event";

    private static final int IMAGE_NUMBER_BASE_OFFSET = 2;
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

        //final int imageNumber =  IMAGE_NUMBER_BASE_OFFSET + area * IMAGE_NUMBER_MULTIPLIER;
        // 2026年夏イベント後段作戦バージョン
        // 不規則な番号に対しては定数化も行わない
        final int imageNumber = switch (area) {
            case 1 -> 4;    // 第三十一戦隊
            case 2 -> 14;   // 増強第三十一戦隊
            case 3 -> 16;   // 多号作戦部隊
            case 4 -> 18;   // 連合艦隊
            case 5 -> 20;   // ウルシー攻撃部隊
            case 6 -> 22;   // 第六艦隊
            case 7 -> 24;   // 仏第3艦隊
            case 8 -> 26;   // 仏第地中海艦隊
            case 9 -> 28;   // 2-eme Escadre Leoele
            case 10 -> 5;   // イギリス救援艦隊
            case 11 -> 7;   // Force de Raid
            case 12 -> 9;   // Force H
            case 13 -> 11;  // 欧州連合艦隊
            default -> IMAGE_NUMBER_BASE_OFFSET + area * IMAGE_NUMBER_MULTIPLIER;
        };

        return Paths.get("common", COMMON_EVENT, String.format("%s_%d.png", COMMON_EVENT, imageNumber));
    }
}
