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

import java.util.ArrayList;
import java.util.List;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributeAPHandler extends AbstractMaplePacketHandler {
	private static Logger log = LoggerFactory.getLogger(DistributeAPHandler.class);
	
	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		List<Pair<MapleStat, Integer>> statupdate = new ArrayList<Pair<MapleStat, Integer>>(2);
		c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true));
		int update = slea.readInt();
		if (c.getPlayer().getRemainingAp() > 0) {
			switch (update) {
				case 0x40: // str
					if (c.getPlayer().getStr() >= 999) {
                                            return;
					}
					c.getPlayer().setStr(c.getPlayer().getStr() + 1);
					statupdate.add(new Pair<MapleStat, Integer>(MapleStat.STR, c.getPlayer().getStr()));
					break;
				case 0x80: // dex
					if (c.getPlayer().getDex() >= 999) {
                                            return;
					}
					c.getPlayer().setDex(c.getPlayer().getDex() + 1);
					statupdate.add(new Pair<MapleStat, Integer>(MapleStat.DEX, c.getPlayer().getDex()));
					break;
				case 0x100: // int
					if (c.getPlayer().getInt() >= 999) {
                                            return;
					}
					c.getPlayer().setInt(c.getPlayer().getInt() + 1);
					statupdate.add(new Pair<MapleStat, Integer>(MapleStat.INT, c.getPlayer().getInt()));
					break;
				case 0x200: // luk
					if (c.getPlayer().getLuk() >= 999) {
                                            return;
					}
					c.getPlayer().setLuk(c.getPlayer().getLuk() + 1);
					statupdate.add(new Pair<MapleStat, Integer>(MapleStat.LUK, c.getPlayer().getLuk()));
					break;
				 case 0x800: // hp
                                     if (c.getPlayer().getMaxHp() >= 30000) {
                                        return;
                                     }
                                        c.getPlayer().setMaxHp(c.getPlayer().getMaxHp() + 1);
                                        statupdate.add(new Pair<MapleStat, Integer>(MapleStat.HP, c.getPlayer().getLuk()));
                                        break;
				 case 0x2000: // mp
                                     if (c.getPlayer().getMaxMp() >= 30000) {
                                        return;
                                     }
                                        c.getPlayer().setMaxMp(c.getPlayer().getMaxMp() + 1);
                                        statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MP, c.getPlayer().getLuk()));
                                        break;
				default: // TODO: implement hp and mp adding
					c.getSession().write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
					return;
			}
			c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - 1);
			statupdate.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp()));
			c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true));
		} else {
			//AutobanManager.getInstance().addPoints(c, 334, 120000, "Trying to distribute AP to " + update + " that are not availables");
			log.info("[h4x] Player {} is distributing ap to {} without having any", c.getPlayer().getName(), Integer.valueOf(update));
		}
	}
}
