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
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class DenyPartyRequestHandler extends AbstractMaplePacketHandler {
	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		byte msg = slea.readByte();
                String from;
                String to;
                MapleCharacter cfrom;
                
                switch(msg) {
                    case 13: // Not accepting invites
                        from = slea.readMapleAsciiString();
                        to = slea.readMapleAsciiString(); 
                        cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
                        if (cfrom != null) {
                            cfrom.getClient().getSession().write(MaplePacketCreator.partyStatusMessage(13, to));
                        }
                        break;
                    case 14: // Taking care of another invite
                        from = slea.readMapleAsciiString();
                        to = slea.readMapleAsciiString();
                        cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
                        if (cfrom != null) {
                            cfrom.getClient().getSession().write(MaplePacketCreator.partyStatusMessage(14, to));
                        }
                        break;
                    case 0x15: // Denied your invitation
                        from = slea.readMapleAsciiString();
                        to = slea.readMapleAsciiString(); 
                        cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
                        if (cfrom != null) {
                            cfrom.getClient().getSession().write(MaplePacketCreator.partyStatusMessage(15, to));
                        }
                        break;
                    default:
                        System.out.println("Unhandled DenyPartyRequest action: " + slea.toString());
                        break;
                }
	}
}
