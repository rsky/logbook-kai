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
        Set<Integer> changed = new HashSet<>();

        Map<Integer, DeckPort> deckMap = DeckPortCollection.get()
                .getDeckPortMap();

        Integer portId = Integer.valueOf(req.getParameter("api_id"));
        Integer shipId = Integer.valueOf(req.getParameter("api_ship_id"));
        int shipIdx = Integer.parseInt(req.getParameter("api_ship_idx"));

        DeckPort deckPort = deckMap.get(portId)
                .clone();
        List<Integer> ships = new ArrayList<>(deckPort.getShip());
        deckPort.setShip(ships);
        deckMap.put(portId, deckPort);

        if (shipId == -1) {
            ships.remove(shipIdx);
            ships.add(-1);
        } else if (shipId == -2) {
            Integer first = ships.getFirst();
            ships.replaceAll(ship -> first.equals(ship) ? ship : -1);
        } else {
            Integer from = ships.get(shipIdx);
            for (Entry<Integer, DeckPort> entry : deckMap.entrySet()) {
                if (entry.getValue().getShip().contains(shipId)) {
                    DeckPort port2 = entry.getValue().clone();
                    List<Integer> ships2 = new ArrayList<>(port2.getShip());
                    port2.setShip(ships2);
                    deckMap.put(port2.getId(), port2);

                    if (from == -1) {
                        ships2.removeIf(id -> id.equals(shipId));
                        ships2.add(-1);
                        shipIdx = deckMap.get(portId).getShip().indexOf(-1);
                    } else {
                        ships2.set(ships2.indexOf(shipId), from);
                    }
                    changed.add(port2.getId());
                    break;
                }
            }
            deckMap.get(portId).getShip().set(shipIdx, shipId);
        }
        changed.add(portId);

        // 随伴艦一括解除以外の場合
        if (shipId != -2) {
            // 変化した艦隊の旗艦に工作艦が存在する場合は泊地修理タイマーをセットする
            for (Integer port : changed) {
                List<Integer> changedShips = deckMap.get(port).getShip();
                if (!changedShips.isEmpty()) {
                    Integer shipid = changedShips.getFirst();
                    Ship ship = ShipCollection.get().getShipMap().get(shipid);
                    if (ship != null) {
                        String type = Ships.stype(ship).map(Stype::getName).orElse("");
                        if ("工作艦".equals(type)) {
                            AppCondition.get().setAkashiTimer(System.currentTimeMillis());
                            break;
                        }
                    }
                }
            }
            // 変化した艦隊の旗艦または2番艦に給糧艦が存在する場合は給糧艦タイマーをセットする
            for (Integer port : changed) {
                List<Integer> changedShips = deckMap.get(port).getShip();
                if (!changedShips.isEmpty()) {
                    boolean timerHasChanged = false;
                    for (int index = 0; index < Math.max(changedShips.size(), 2); index++) {
                        Integer shipid = changedShips.get(index);
                        Ship ship = ShipCollection.get().getShipMap().get(shipid);
                        if (ship != null) {
                            ShipMst shipMst = ShipMstCollection.get().getShipMap().get(ship.getShipId());
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
