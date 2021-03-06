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

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeMapHandler extends AbstractMaplePacketHandler {
	private static Logger log = LoggerFactory.getLogger(ChangeMapHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
                if (c.getPlayer().inCS()) { // <3
                    c.getPlayer().setInCS(false);
                    c.getPlayer().changeMap(c.getPlayer().getMap(), c.getPlayer().getPosition()); //c.getPlayer().getMap().getPortal(0));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
		slea.skip(1);
		int targetid = slea.readInt(); //FF FF FF FF

		String startwp = slea.readMapleAsciiString();
		MaplePortal portal = c.getPlayer().getMap().getPortal(startwp);
		
		MapleCharacter player = c.getPlayer();
		if (targetid != -1 && !c.getPlayer().isAlive()) {
			boolean executeStandardPath = true;
			if (player.getEventInstance() != null) {
				executeStandardPath = player.getEventInstance().revivePlayer(player);
			}
			if (executeStandardPath) {
				player.setHp(50);
				MapleMap to = c.getPlayer().getMap().getReturnMap();
				MaplePortal pto = to.getPortal(0);
				player.setStance(0);
				player.changeMap(to, pto);
			}
		} else if (targetid != -1 && c.getPlayer().isGM()) {
			MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
			MaplePortal pto = to.getPortal(0);
			player.changeMap(to, pto);
		} else if (targetid != -1 && !c.getPlayer().isGM()) {
			log.warn("Player {} attempted Mapjumping without being a gm", c.getPlayer().getName());
		} else {
                    if (portal != null) {
                            portal.enterPortal(c);
                    } else {
                            c.getSession().write(MaplePacketCreator.enableActions());
                            log.warn("Portal {} not found on map {}", startwp, c.getPlayer().getMap().getId());
                    }
		}
	}

}
