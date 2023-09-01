package logbook.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 出撃札
 */
class SeaAreaBanner {
    private static final String JOIN_BANNER = "sally_strategymap/sally_strategymap_{0}.png";
    private static final String JOIN_BANNER_2 = "sally_strategymap_second/sally_strategymap_second_{0}.png";
    private static final String JOIN_BANNER_3 = "sally_strategymap_third/sally_strategymap_third_{0}.png";

    /**
     * 出撃札の画像パスを取得します
     *
     * @param area 海域(札)番号
     * @return 出撃札の画像パス。存在しない場合はnull
     */
    static Path getJoinBannerPath(int area) {
        String bannerFormat;
        int imageNumber;

        // 2023年夏イベント最終版
        switch (area) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                bannerFormat = JOIN_BANNER;
                imageNumber = area + 32;
                break;
            case 7:
            case 8:
            case 9:
                bannerFormat = JOIN_BANNER_2;
                imageNumber = area + 10;
                break;
            case 10:
                bannerFormat = JOIN_BANNER_2;
                imageNumber = 16;
                break;
            case 11:
                bannerFormat = JOIN_BANNER_3;
                imageNumber = 12;
                break;
            default:
                return null;
        }

        return Paths.get("sally", bannerFormat.replace("{0}", Integer.toString(imageNumber)));
    }
}