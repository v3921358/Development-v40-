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
import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.movement.AbsoluteLifeMovement;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovePlayerHandler extends AbstractMovementPacketHandler {
    private static Logger log = LoggerFactory.getLogger(MovePlayerHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte(); // next int is original x and y positions
        
        short orig_x = slea.readShort(); 
        short orig_y = slea.readShort(); 
        List<LifeMovementFragment> res = parseMovement(slea);
        if (res != null) {
            if (slea.available() != 10) {
                log.warn("slea.available != 10 (movement parsing error)");
                return;
            }
            slea.seek(2); 
            if (!c.getPlayer().isHidden()) {
                MaplePacket packet = MaplePacketCreator.movePlayer(c.getPlayer().getId(), slea);
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), packet, false);
            }
            if (CheatingOffense.FAST_MOVE.isEnabled() || CheatingOffense.HIGH_JUMP.isEnabled()) {
                checkMovementSpeed(c.getPlayer(), res);
            }
            updatePosition(res, c.getPlayer(), 0);
            c.getPlayer().getMap().movePlayer(c.getPlayer(), c.getPlayer().getPosition());
        }
    }
	
    private static void checkMovementSpeed(MapleCharacter chr, List<LifeMovementFragment> moves) {
        double playerSpeedMod = chr.getSpeedMod() + 0.005;
        boolean encounteredUnk0 = false;
        for (LifeMovementFragment lmf : moves) {
            if (lmf.getClass() == AbsoluteLifeMovement.class) {
                final AbsoluteLifeMovement alm = (AbsoluteLifeMovement) lmf;
                double speedMod = Math.abs(alm.getPixelsPerSecond().x) / 125.0;
                if (speedMod > playerSpeedMod) {
                    //if (alm.getUnk() == 0) { // to prevent FJ fucking us
                    //	encounteredUnk0 = true;
                    //}
                    if (!encounteredUnk0) {
                        if (speedMod > playerSpeedMod) {
                            chr.getCheatTracker().registerOffense(CheatingOffense.FAST_MOVE);
                        }
                    }
                }
            }
        }
    }
}
