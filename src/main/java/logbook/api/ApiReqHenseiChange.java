package logbook.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.JsonObject;

import logbook.bean.*;
import logbook.internal.Ships;
import logbook.proxy.RequestMetaData;
import logbook.proxy.ResponseMetaData;

/**
 * /kcsapi/api_req_hensei/change
 *
 */
@API("/kcsapi/api_req_hensei/change")
public class ApiReqHenseiChange implements APIListenerSpi {

    @Override
    public void accept(JsonObject json, RequestMetaData req, ResponseMetaData res) {
        // 変化した艦隊
        final Set<Integer> changed = new HashSet<>();

        final Map<Integer, DeckPort> deckMap = DeckPortCollection.get()
                .getDeckPortMap();

        final Integer deckId = Integer.valueOf(req.getParameter("api_id"));
        final Integer shipId = Integer.valueOf(req.getParameter("api_ship_id"));
        int shipIdx = Integer.parseInt(req.getParameter("api_ship_idx"));

        final DeckPort deck = deckMap.get(deckId).clone();
        final List<Integer> ships = new ArrayList<>(deck.getShip());
        deck.setShip(ships);
        deckMap.put(deckId, deck);

        if (shipId == -1) {
            // はずす
            ships.remove(shipIdx);
            ships.add(-1);
        } else if (shipId == -2) {
            // 随伴艦一括解除
            Integer first = ships.getFirst();
            ships.replaceAll(ship -> first.equals(ship) ? ship : -1);
        } else {
            // 追加 or 移動 or 入れ替え
            Integer from = ships.get(shipIdx);
            for (Entry<Integer, DeckPort> deckEntry : deckMap.entrySet()) {
                if (deckEntry.getValue().getShip().contains(shipId)) {
                    DeckPort deck2 = deckEntry.getValue().clone();
                    List<Integer> ships2 = new ArrayList<>(deck2.getShip());
                    deck2.setShip(ships2);
                    deckMap.put(deck2.getId(), deck2);
                    if (from == -1) {
                        // 移動
                        ships2.removeIf(id -> id.equals(shipId));
                        ships2.add(-1);
                        shipIdx = deckMap.get(deckId).getShip().indexOf(-1);
                    } else {
                        // 入れ替え
                        ships2.set(ships2.indexOf(shipId), from);
                    }
                    changed.add(deck2.getId());
                    break;
                }
            }
            deckMap.get(deckId).getShip().set(shipIdx, shipId);
        }
        changed.add(deckId);

        // 随伴艦一括解除以外の場合
        if (shipId != -2) {
            // 変化した艦隊の旗艦に工作艦が存在する場合は泊地修理タイマーをセットする
            for (Integer port : changed) {
                final List<Integer> changedShips = deckMap.get(port).getShip();
                if (!changedShips.isEmpty()) {
                    final Integer candidateShipId = changedShips.getFirst();
                    final Ship ship = ShipCollection.get().getShipMap().get(candidateShipId);
                    if (ship != null) {
                        final String type = Ships.stype(ship).map(Stype::getName).orElse("");
                        if ("工作艦".equals(type)) {
                            AppCondition.get().setAkashiTimer(System.currentTimeMillis());
                            break;
                        }
                    }
                }
            }
            // 変化した艦隊の旗艦または2番艦に給糧艦が存在する場合は給糧艦タイマーをセットする
            for (Integer port : changed) {
                final List<Integer> changedShips = deckMap.get(port).getShip();
                if (!changedShips.isEmpty()) {
                    boolean timerHasChanged = false;
                    for (int index = 0; index < Math.min(changedShips.size(), 2); index++) {
                        final Integer candidateShipId = changedShips.get(index);
                        final Ship ship = ShipCollection.get().getShipMap().get(candidateShipId);
                        if (ship != null) {
                            final ShipMst shipMst = ShipMstCollection.get().getShipMap().get(ship.getShipId());
                            if (shipMst.getName().startsWith("野埼")) {
                                AppCondition.get().setNosakiTimer(System.currentTimeMillis());
                                timerHasChanged = true;
                                break;
                            }
                        }
                    }
                    if (timerHasChanged) {
                        break;
                    }
                }
            }
        }
    }
}
