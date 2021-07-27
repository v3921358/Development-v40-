/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.odinms.net.channel.handler;

import java.util.LinkedList;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.maps.MapleMapItem;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.client.MapleInventoryType;

/**
 *
 * @author Raz
 */
public class PetLootHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getNoPets() == 0) {
            return;
        }
        MaplePet pet = c.getPlayer().getPet(0);
        slea.skip(4);
        int oid = slea.readInt();
        MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
        if (ob == null || pet == null) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        if (ob instanceof MapleMapItem) {
            MapleMapItem mapitem = (MapleMapItem) ob;
            if (mapitem.getDropper().getObjectId() == c.getPlayer().getObjectId()) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            synchronized (mapitem) {
                if (mapitem.isPickedUp()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    return;
                }
                double distance = pet.getPos().distanceSq(mapitem.getPosition());
                c.getPlayer().getCheatTracker().checkPickupAgain();
                if (distance > 90000.0) { // 300^2, 550 is approximatly the range of ultis
                    c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.ITEMVAC);
                } else if (distance > 22500.0) {
                    c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.SHORT_ITEMVAC);
                }
                if (mapitem.getMeso() > 0) {
                    if (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).findById(1812000) != null) { //Evil hax until I find the right packet - Ramon
                        if (c.getPlayer().getParty() != null) {
                            List<MapleCharacter> pChrs = c.getChannelServer().getPartyMembers(c.getPlayer().getParty());
                            List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                            int mesosamm = mapitem.getMeso();

                            for (MapleCharacter partymem : pChrs) {
                                if (partymem.getMapId() == c.getPlayer().getMapId()) {
                                    toGive.add(partymem);
                                }
                            }
                            int mesosgain = mesosamm / toGive.size();
                            for (MapleCharacter partymem : toGive) {
                                if (partymem != null) {
                                    partymem.gainMeso(mesosgain, true, true);
                                }
                            }
                        } else {
                            c.getPlayer().gainMeso(mapitem.getMeso(), true, true);
                        }
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, 0), mapitem.getPosition());
                        c.getPlayer().getCheatTracker().pickupComplete();
                        c.getSession().write(MaplePacketCreator.enableActions());
                    } else {
                        c.getPlayer().getCheatTracker().pickupComplete();
                        mapitem.setPickedUp(false);
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                }

                if (mapitem.getItem().getItemId() >= 5000000 && mapitem.getItem().getItemId() <= 5000005) {
                    int petId = MaplePet.createPet(mapitem.getItem().getItemId());
                    if (petId == -1) {
                        return;
                    }
                    if (MapleInventoryManipulator.addById(c, mapitem.getItem().getItemId(), mapitem.getItem().getQuantity(), null, petId)) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, 0), mapitem.getPosition());
                        c.getPlayer().getCheatTracker().pickupComplete();
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        c.getPlayer().getCheatTracker().pickupComplete();
                        return;
                    }
                } else {
                    if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), "")) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, 0), mapitem.getPosition());
                        c.getPlayer().getCheatTracker().pickupComplete();
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        c.getPlayer().getCheatTracker().pickupComplete();
                        return;
                    }
                }
            }
            mapitem.setPickedUp(true);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
