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

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author AngelSL
 */
public class UseSummonBagHandler extends AbstractMaplePacketHandler {
    private static Logger log = LoggerFactory.getLogger(UseSummonBagHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt(); //as if we cared... ;)
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemId && c.getPlayer().isAlive()) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            int[][] toSpawn = ii.getSummonMobs(itemId);
            for (int z = 0; z < toSpawn.length; z++) {
                int[] toSpawnChild = toSpawn[z];
                if ((int) Math.ceil(Math.random() * 100) <= toSpawnChild[1]) {
                    MapleMonster ht = MapleLifeFactory.getMonster(toSpawnChild[0]);
                    c.getPlayer().getMap().spawnMonsterOnGroudBelow(ht, c.getPlayer().getPosition());
                }
            }
        } else {
            log.info("[h4x] " + c.getPlayer().getName() + " is trying to use a summonbag not in item inventory.");
            return;
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}