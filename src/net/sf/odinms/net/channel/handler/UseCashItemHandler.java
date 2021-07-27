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

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseCashItemHandler extends AbstractMaplePacketHandler {
	private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UseCashItemHandler.class);
	
	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
                byte slot = (byte)slea.readShort();
		int itemId = slea.readInt();
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
		MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
		try {
                    switch(itemId) {
                        case 2081000: // Megaphone
                            if (c.getPlayer().getLevel() >= 10) {
                                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(2, c.getPlayer().getName() + " : " + slea.readMapleAsciiString()));
                            } else {
                                c.getPlayer().dropMessage("You may not use this until you're level 10");
                            }
                            break;
                        case 2082000: // Super Megaphone
                            c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(3, c.getChannel(), c.getPlayer().getName() + " : " + slea.readMapleAsciiString()).getBytes());
                            break;
                        case 2090000: // Weather Effects
                        case 2090001:
                        case 2090002:
                        case 2090003:
                        case 2090004:
                        case 2090005:
                        case 2090006:
                        case 2090007:
                        case 2090008:
                            c.getPlayer().getMap().startMapEffect(ii.getMsg(itemId).replaceFirst("%s", c.getPlayer().getName()).replaceFirst("%s", slea.readMapleAsciiString()), itemId);
                            break;
                        case 2110000: // Pet Tag
                            MaplePet pet = c.getPlayer().getPet(0);
                            if (pet != null) {
                                String newName = slea.readMapleAsciiString();
                                if (newName.length() > 2 && newName.length() < 14) {
                                    pet.setName(newName);
                                    c.getSession().write(MaplePacketCreator.updatePet(pet, true));
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.changePetName(c.getPlayer(), newName, 1), true);
                                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                                } else {
                                    c.getPlayer().dropMessage(1, "Names must be 3 - 13 characters.");
                                }
                            } else {
                                c.getSession().write(MaplePacketCreator.enableActions());
                            }
                            break;
                        case 2130000: // Kite
                        case 2130001:
                        case 2130002:
                        case 2130003:
                            c.getPlayer().getMap().startKiteEffect(slea.readMapleAsciiString(), itemId, c.getPlayer());
                            break;
                        case 2140000: // Meso Sacks
                        case 2140001:
                        case 2140002:
                            if (ii.getMeso(itemId) + c.getPlayer().getMeso() < Integer.MAX_VALUE) {
                                c.getPlayer().gainMeso(ii.getMeso(itemId), true, false, true);
                                c.getSession().write(MaplePacketCreator.enableActions()); // do we really need this?
                            } else {
                                c.getPlayer().dropMessage(1, "Cannot hold anymore mesos.");
                            }
                            break;
                        case 2150000: // Jukebox
                            c.getPlayer().getMap().startJukebox(c.getPlayer().getName(), itemId);
                            break;
                        case 2160000: // Memo
                            String sendTo = slea.readMapleAsciiString();
                            String msg = slea.readMapleAsciiString();
                            int recipientId = MapleCharacter.getIdByName(sendTo, c.getWorld());
                            if (recipientId > -1) {
                                Connection con = DatabaseConnection.getConnection();
                                try {
                                    PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
                                    ps.setInt(1, recipientId);
                                    ps.setString(2, c.getPlayer().getName());
                                    ps.setString(3, msg);
                                    ps.setLong(4, System.currentTimeMillis());
                                    ps.executeUpdate();
                                    ps.close();
                                } catch (SQLException e) {
                                    log.error("SAVING NOTE", e);
                                }
                                c.getPlayer().dropMessage("The note has successfully been sent");
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            } else {
                                c.getPlayer().dropMessage(5, "This player could not be found.");
                            }
                            break;
                        case 2170000: // Teleport Rock
                            byte rocktype = slea.readByte();
                            if (rocktype == 0) {
                                int mapId = slea.readInt();
                                if ((c.getChannelServer().getMapFactory().getMap(mapId).getReturnMap().getId() == 999999999) || ServerConstants.ReturnScrollAnywhere) {
                                    for (int i : ServerConstants.BLOCKED_VIP_MAPS) {
                                        if (mapId == i) {
                                            c.getPlayer().dropMessage(1, "You cannot use teleport to a blocked location.");
                                        }
                                    }
                                    c.getPlayer().changeMap(mapId); // if we returnScrollAnywhere, we'll let players map-travel anywhere too.
                                }
                            } else {
                                String name = slea.readMapleAsciiString();
                                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                                if (victim == null) {
                                    int channel = c.getChannelServer().getWorldInterface().find(name);
                                    if (channel == -1) {
                                        c.getPlayer().dropMessage(1, "This player is not online.");
                                        break;
                                    }
                                    ChangeChannelHandler.changeChannel(channel, c);
                                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                                }
                                if (victim == null) { // player dcs / ccs while you cc o____o
                                    c.getPlayer().dropMessage(5, "A System error has occured, please try again.");
                                } else {
                                    MapleMap map = victim.getMap();
                                    for (int i : ServerConstants.BLOCKED_VIP_MAPS) {
                                        if (map.getId() == i) {
                                            c.getPlayer().dropMessage(1, "You cannot use teleport to a blocked location.");
                                        }
                                    }
                                    if ((victim.isGM() && c.getPlayer().isGM()) || !victim.isGM()) { // I should really handle this before the switch
                                        if (c.getPlayer().getMapId() / 100000000 == victim.getMapId() / 100000000) { // Continent calculation. Map 0, 1, 2.
                                            c.getPlayer().changeMap(map, map.findClosestSpawnpoint(victim.getPosition()));
                                        } else {
                                            c.getPlayer().dropMessage(1, "You cannot warp to this player because he's not in the same continent.");
                                        }
                                    } else {
                                        c.getPlayer().dropMessage(1, "You cannot use teleport to a blocked location.");
                                    }
                                }
                            }
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                            break;
                        default:
                            c.getPlayer().dropMessage(1, "Uh-oh! It looks like you've found an item Eric hasn't coded in yet! Please report this to him!");
                            break;
                    }
                    c.getSession().write(MaplePacketCreator.enableActions());
		} catch (RemoteException e) {
			c.getChannelServer().reconnectWorld();
			log.error("REOTE TRHOW", e);
		}
	}
}
