package logbook.bean;

import logbook.internal.JsonHelper;
import lombok.Data;

import javax.json.JsonObject;
import java.io.Serializable;

@Data
public class SpEffectItem implements Serializable {

    private static final long serialVersionUID = -1430429039139415315L;

    /** 青リボン */
    public static final int KIND_BLUE_RIBBON = 1;

    /** 白タスキ */
    public static final int KIND_WHITE_TASUKI = 2;

    /** 種別 */
    private Integer kind;

    /** 砲撃 */
    private Integer houg;

    /** 雷撃 */
    private Integer raig;

    /** 装甲 */
    private Integer souk;

    /** 回避 */
    private Integer kaih;

    public static SpEffectItem toSpEffectItem(JsonObject json) {
        SpEffectItem bean = new SpEffectItem();
        JsonHelper.bind(json)
                .setInteger("api_kind", bean::setKind)
                .setInteger("api_houg", bean::setHoug)
                .setInteger("api_raig", bean::setRaig)
                .setInteger("api_souk", bean::setSouk)
                .setInteger("api_kaih", bean::setKaih);
        return bean;
    }
}
