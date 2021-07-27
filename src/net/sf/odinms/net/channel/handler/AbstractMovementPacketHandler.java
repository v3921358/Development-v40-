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

import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.maps.AnimatedMapleMapObject;
import net.sf.odinms.server.movement.AbsoluteLifeMovement;
import net.sf.odinms.server.movement.ChairMovement;
import net.sf.odinms.server.movement.ChangeEquipSpecialAwesome;
import net.sf.odinms.server.movement.LifeMovement;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.server.movement.RelativeLifeMovement;
import net.sf.odinms.server.movement.TeleportMovement;
import net.sf.odinms.tools.HexTool;
import net.sf.odinms.tools.data.input.LittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMovementPacketHandler extends AbstractMaplePacketHandler {
	private static Logger log = LoggerFactory.getLogger(AbstractMovementPacketHandler.class);

	protected List<LifeMovementFragment> parseMovement(LittleEndianAccessor lea) {
		List<LifeMovementFragment> res = new ArrayList<LifeMovementFragment>();
                //short orig_x = lea.readShort(); 
                //short orig_y = lea.readShort(); 
                byte numCommands = lea.readByte();
		for (byte i = 0; i < numCommands; i++) {
                    byte command = lea.readByte();
                    switch (command) {
                        case 0x00: // normal move
                        case 0x05:
                        {
                            short xpos = lea.readShort();
                            short ypos = lea.readShort();
                            short xwobble = lea.readShort();
                            short ywobble = lea.readShort();
                            short duration = lea.readShort();
                            byte newstate = lea.readByte();
                            lea.skip(2); // short, random and changes
                            AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                            alm.setPixelsPerSecond(new Point(xwobble, ywobble));
                            res.add(alm);
                            break;
                        }
                        case 0x01:
                        case 0x02:
                        case 0x06: // fj
                        {
                            short xmod = lea.readShort();
                            short ymod = lea.readShort();
                            byte newstate = lea.readByte();
                            short duration = lea.readShort();
                            RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xmod, ymod), duration, newstate);
                            res.add(rlm);
                            break;
                        }
                        case 0x03:
                        case 0x04: // tele... -.-
                        case 0x07: // assaulter
                        {
                            short xpos = lea.readShort();
                            short ypos = lea.readShort();
                            short xwobble = lea.readShort();
                            short ywobble = lea.readShort();
                            byte newstate = lea.readByte();
                            TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);
                            tm.setPixelsPerSecond(new Point(xwobble, ywobble));
                            res.add(tm);
                            break;
                        }
                        case 0x08: // change equip ???
                        {
                            res.add(new ChangeEquipSpecialAwesome(lea.readByte()));
                            break;
                        }
                        default: {
                            log.warn("Unhandeled movement command {} received", command);
                            return null;
                        }
                    }
		}
		if (numCommands != res.size()) {
                    log.warn("numCommands ({}) does not match the number of deserialized movement commands ({})", numCommands, res.size());
		}
		return res;
	}

	protected void updatePosition(List<LifeMovementFragment> movement, AnimatedMapleMapObject target, int yoffset) {
		for (LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				if (move instanceof AbsoluteLifeMovement) {
					Point position = ((LifeMovement) move).getPosition();
					position.y += yoffset;
					target.setPosition(position);
				}
				target.setStance(((LifeMovement) move).getNewstate());
			}
		}
	}
}
