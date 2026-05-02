package logbook.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.json.JsonObject;

import logbook.bean.AppCondition;
import logbook.bean.BattleLog;
import logbook.bean.BattleResult;
import logbook.bean.BattleResult.Escape;
import logbook.bean.Ship;
import logbook.proxy.RequestMetaData;
import logbook.proxy.ResponseMetaData;

/**
 * /kcsapi/api_req_sortie/goback_port
 * /kcsapi/api_req_combined_battle/goback_port
 *
 */
@API({ "/kcsapi/api_req_sortie/goback_port", "/kcsapi/api_req_combined_battle/goback_port" })
public class ApiReqCombinedBattleGobackPort implements APIListenerSpi {

    @Override
    public void accept(JsonObject json, RequestMetaData req, ResponseMetaData res) {

        BattleLog log = AppCondition.get().getBattleResultConfirm();
        if (log != null) {
            BattleResult result = log.getResult();
            Escape escape = result.getEscape();

            Set<Integer> escapeSet = AppCondition.get()
                    .getEscape();

            // 退避
            Optional.of(escape.getEscapeIdx())
                    .map(List::getFirst)
                    .map(i -> this.getShipId(log.getDeckMap(), i))
                    .ifPresent(escapeSet::add);
            // 護衛
            Optional.ofNullable(escape.getTowIdx())
                    .map(List::getFirst)
                    .map(i -> this.getShipId(log.getDeckMap(), i))
                    .ifPresent(escapeSet::add);
        }
    }

    /**
     * 退避した艦娘のIDを返します
     *
     * @param deckMap 艦隊スナップショット
     * @param index 艦隊インデックス
     * @return 退避した艦娘のID
     */
    private Integer getShipId(Map<Integer, List<Ship>> deckMap, Integer index) {
        List<Integer> decks = new ArrayList<>(deckMap.keySet());
        decks.sort(Integer::compareTo);

        Ship ship;
        if ((index - 1) < Math.max(deckMap.get(decks.getFirst()).size(), 6)) {
            ship = deckMap.get(decks.getFirst()).get(index - 1);
        } else {
            ship = deckMap.get(decks.get(1)).get(index - 6 - 1);
        }
        return ship.getId();
    }
}
