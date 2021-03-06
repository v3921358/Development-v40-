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

import java.awt.Point;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class MovePetHandler extends AbstractMovementPacketHandler {
	private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MovePetHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		slea.skip(4);
		List<LifeMovementFragment> res = parseMovement(slea);
                if (res.isEmpty()) return;

		MapleCharacter player = c.getPlayer();
                MaplePet pet = player.getPet(0); // No multi-pet in v40b.
		int slot = player.getPetIndex(pet);
		if (player.inCS() || slot == -1) {
                    return;
		}
		player.getPet(slot).updatePosition(res); 
                slea.seek(1);
		player.getMap().broadcastMessage(player, MaplePacketCreator.movePet(player.getId(), slea), false);
	}
}