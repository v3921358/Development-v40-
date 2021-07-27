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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.odinms.net.channel.handler;

import java.net.InetAddress;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.login.LoginServer;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public class ChangeChannelHandler extends AbstractMaplePacketHandler {

        @Override
        public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
            int channel = slea.readByte() + 1; // there is some int after it...but...wtf?
            changeChannel(channel, c);
        }
    
        public static void changeChannel(int channel, MapleClient c) {
		c.getPlayer().cancelAllBuffs();
		String ip = ChannelServer.getInstance(c.getChannel()).getIP(channel);
		System.out.println("Changing channel towards " + ip);
		if (c.getPlayer().getTrade() != null) {
                    MapleTrade.cancelTrade(c.getPlayer());
		}
                c.getPlayer().cancelMagicDoor();
		c.getPlayer().saveToDB(true);
		if (c.getPlayer().getCheatTracker() != null)
                    c.getPlayer().getCheatTracker().dispose();
		c.getPlayer().getMap().removePlayer(c.getPlayer());
		ChannelServer.getInstance(c.getChannel()).removePlayer(c.getPlayer());
		c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                String[] socket = ip.split(":");
		try {
                    MaplePacket packet = MaplePacketCreator.getChannelChange(ServerConstants.getIP(), Short.parseShort(socket[1]));
                    c.getSession().write(packet);
		} catch (Exception e) {
                    throw new RuntimeException(e);
		}
	}
}
