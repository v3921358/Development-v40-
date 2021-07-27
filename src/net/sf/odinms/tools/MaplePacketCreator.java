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
package net.sf.odinms.tools;

import java.awt.Point;
import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import net.sf.odinms.client.BuddylistEntry;
import net.sf.odinms.client.IEquip;
import net.sf.odinms.client.IEquip.ScrollResult;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleKeyBinding;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.client.MapleQuestStatus;
import net.sf.odinms.client.MapleRing;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.ByteArrayMaplePacket;
import net.sf.odinms.net.LongValueHolder;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.SendPacketOpcode;
import net.sf.odinms.net.channel.handler.AbstractDealDamageHandler.SummonAttackEntry;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.server.CashItemInfo;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePlayerShop;
import net.sf.odinms.server.MaplePlayerShopItem;
import net.sf.odinms.server.MapleShopItem;
import net.sf.odinms.server.MapleSnowball;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.PlayerInteraction.IPlayerInteractionManager;
import net.sf.odinms.server.PlayerInteraction.MapleMiniGame;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleReactor;
import net.sf.odinms.server.maps.SummonMovementType;
import net.sf.odinms.server.movement.LifeMovementFragment;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.tools.data.output.LittleEndianWriter;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides all MapleStory packets needed in one place.
 * 
 * @author Frz
 * @since Revision 259
 * @version 1.0
 */
public class MaplePacketCreator {
	private static Logger log = LoggerFactory.getLogger(MaplePacketCreator.class);

	private final static byte[] CHAR_INFO_MAGIC = new byte[] { (byte) 0xff, (byte) 0xc9, (byte) 0x9a, 0x3b };
	private final static byte[] ITEM_MAGIC = new byte[] { (byte) 0x80, 5 };
	public static final List<Pair<MapleStat, Integer>> EMPTY_STATUPDATE = Collections.emptyList();
        private final static long FT_UT_OFFSET = 116444592000000000L; // EDT

        private static long getKoreanTimestamp(long realTimestamp) {
            long time = (realTimestamp / 1000 / 60); // convert to minutes
            return ((time * 600000000) + FT_UT_OFFSET);
        }
        
	/**
	 * Sends a hello packet.
	 * 
	 * @param mapleVersion The maple client version.
	 * @param sendIv the IV used by the server for sending
	 * @param recvIv the IV used by the server for receiving
	 */
	//* GMS v40 Beta MapleStory
        public static MaplePacket getHello(short mapleVersion, byte[] sendIv, byte[] recvIv, boolean testServer) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.CLIENT_HELLO.getValue());
            mplew.writeShort(40);
            mplew.writeShort(1);
            mplew.write(0);
            mplew.write(recvIv);
            mplew.write(sendIv);
            mplew.write(5);
            return mplew.getPacket();
	}
        //*/
        
        /* KMS v1 Beta MapleStory
        public static MaplePacket getHello(short mapleVersion, byte[] sendIv, byte[] recvIv, boolean testServer) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(0x0E);
            mplew.writeShort(223);
            mplew.writeMapleAsciiString("");
            mplew.write(recvIv);
            mplew.write(sendIv);
            mplew.write(1); // 1 = KMS, 2 = KMST
            return mplew.getPacket();
	}
        //*/

	/**
	 * Sends a ping packet.
	 * 
	 * @return The packet.
	 */
	public static MaplePacket getPing() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PING.getValue());
            return mplew.getPacket();
	}

	/**
	 * Gets a login failed packet.
	 * 
	 * Possible values for <code>reason</code>:<br>
	 * 3: ID deleted or blocked<br>
	 * 4: Incorrect password<br>
	 * 5: Not a registered id<br>
	 * 6: System error<br>
	 * 7: Already logged in<br>
	 * 8: System error<br>
	 * 9: System error<br>
	 * 10: Cannot process so many connections<br>
	 * 11: Only users older than 20 can use this channel
	 * 
	 * @param reason The reason logging in failed.
	 * @return The login failed packet.
	 */
	public static MaplePacket getLoginFailed(int reason) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
            mplew.write(SendPacketOpcode.LOGIN_STATUS.getValue());
            mplew.write(reason);
            mplew.write(0);
            mplew.writeInt(0);
            return mplew.getPacket();
	}
        
        /**
	 * Gets a successful authentication and PIN Request packet.
	 * 
	 * @param c The client.
	 * @return The PIN request packet.
	 */
	public static MaplePacket OnCheckPasswordResult(MapleClient c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.LOGIN_STATUS.getValue());
            mplew.write(0); // 0 or 1? o.o
            mplew.write(0);
            mplew.writeInt(0);
            mplew.writeInt(c.getAccID());
            mplew.write(0);
            mplew.write(c.isGm() ? 1 : 0);
            mplew.write(1);
            mplew.writeMapleAsciiString(c.getAccountName());
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            return mplew.getPacket();
	}
        
        public static MaplePacket getServerIP(int charid, byte[] ip, short port) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x05);
            mplew.writeShort(0); // ?
            mplew.write(ip);
            mplew.writeShort(port); //login or world or channel o.o
            mplew.writeInt(charid); // charid
            mplew.write(0);
            return mplew.getPacket();
        }
        
        public static MaplePacket getServerList() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            int channels = ServerConstants.Channels;
            mplew.write(SendPacketOpcode.SERVERLIST.getValue());
            mplew.write(0);
            mplew.writeMapleAsciiString(ServerConstants.WORLD_NAME); // Tespia is the only world
            mplew.write((byte)channels); // channels
            for (byte i = 0; i < (byte)channels; i++) {
                mplew.writeMapleAsciiString(ServerConstants.WORLD_NAME + "-" + (i + 1));
                mplew.writeInt(100); // server load, get connected size
                mplew.write(0); // Tespia is the only world
                mplew.write((byte)i);
                mplew.write(0); // unk
            }   
            return mplew.getPacket();
        }
        
        /**
	 * Gets a packet saying that the server list is over.
	 * 
	 * @return The end of server list packet.
	 */
	public static MaplePacket getEndOfServerList() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.SERVERLIST.getValue());
            mplew.write(0xFF);
            return mplew.getPacket();
	}
        
        /**
	 * Gets a packet detailing a server status message.
	 * 
	 * Possible values for <code>status</code>:<br>
	 * 0 - Normal<br>
	 * 1 - Highly populated<br>
	 * 2 - Full
	 * 
	 * @param status The server status.
	 * @return The server status packet.
	 */
	public static MaplePacket getServerStatus(byte status) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.SERVERSTATUS.getValue());
            mplew.write(status);
            return mplew.getPacket();
	}
        
        /**
	 * Gets a packet with a list of characters.
	 * 
	 * @param c The MapleClient to load characters of.
	 * @param serverId The ID of the server requested.
	 * @return The character list packet.
	 */
	public static MaplePacket getCharList(MapleClient c, int serverId) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CHARLIST.getValue());
		mplew.write(0);
		List<MapleCharacter> chars = c.loadCharacters(serverId);
		mplew.write((byte) chars.size());
		for (MapleCharacter chr : chars) {
                    addCharData(mplew, chr);
		}
                mplew.writeLong(0); 
		return mplew.getPacket();
	}
        
        /**
	 * Adds an entry for a character to an existing
	 * MaplePacketLittleEndianWriter.
	 * 
	 * @param mplew The MaplePacketLittleEndianWrite instance to write the stats
	 *            to.
	 * @param chr The character to add.
	 */
	private static void addCharData(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
            mplew.writeInt(chr.getId());
            mplew.writeAsciiString(chr.getName(), 13);
            mplew.write(chr.getGender());
            mplew.write(chr.getSkinColor().getId());
            mplew.writeInt(chr.getFace());
            mplew.writeInt(chr.getHair());
            
            mplew.writeLong(0); // Pet SN 
            
            mplew.write(chr.getLevel());
            mplew.writeShort(chr.getJob().getId());
            mplew.writeShort(chr.getStr());
            mplew.writeShort(chr.getDex());
            mplew.writeShort(chr.getInt());
            mplew.writeShort(chr.getLuk());
            mplew.writeShort(chr.getHp());
            mplew.writeShort(chr.getMaxHp());
            mplew.writeShort(chr.getMp());
            mplew.writeShort(chr.getMaxMp());
            mplew.writeShort(chr.getRemainingAp());
            mplew.writeShort(chr.getRemainingSp());
            mplew.writeInt(chr.getExp());
            mplew.writeShort(chr.getFame());
            
            mplew.writeInt(chr.getMapId());
            mplew.write(chr.getInitialSpawnpoint());
            
            mplew.writeLong(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            
            MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
            Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
            Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
            for (IItem item : iv.list()) {
                byte pos = (byte) (item.getPosition() * -1);
                if (pos < 100 && myEquip.get(pos) == null) {
                    maskedEquip.put(pos, item.getItemId()); 
                } else if (pos > 100) { // lol odinms. walao eh
                    pos -= 100;
                    if (myEquip.get(pos) != null) {
                        maskedEquip.put(pos, myEquip.get(pos));
                    }
                    myEquip.put(pos, item.getItemId());
                } else if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, item.getItemId());
                }
            }
            for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
                mplew.write(entry.getKey());
                mplew.writeInt(entry.getValue());
            }
            mplew.write(0);
            for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
                mplew.write(entry.getKey());
                mplew.writeInt(entry.getValue());
            }
            mplew.write(0);
            if ((chr.getJob().getId() / 100) != 5) {
                mplew.write(1);
                mplew.writeInt(chr.getRank());
                mplew.writeInt(chr.getRankMove()); // might be reverted
                mplew.writeInt(chr.getJobRank());
                mplew.writeInt(chr.getJobRankMove());
            } else {
                mplew.write(0);
            }
	}
        
        public static MaplePacket charNameResponse(String charname, boolean nameUsed) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
            mplew.writeMapleAsciiString(charname);
            mplew.write(nameUsed ? 1 : 0);
            return mplew.getPacket();
	}
        
        public static MaplePacket addNewCharEntry(MapleCharacter chr, boolean worked) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
            mplew.write(worked ? 0 : 1);
            if (worked) {
                addCharData(mplew, chr);
            }
            mplew.writeLong(0);
            mplew.writeLong(0);
            return mplew.getPacket();
	}
        
        /**
	 * Gets a packet telling the client the IP of the channel server.
	 * 
	 * @param ip The ip of the requested channel server.
	 * @param port The port the channel is on.
	 * @param clientId The ID of the client.
	 * @return The server IP packet.
	 */
	public static MaplePacket getServerIP(byte[] ip, short port, int clientId) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SERVER_IP.getValue());
		mplew.writeShort(0);
		mplew.write(ip);
		mplew.writeShort(port);
		mplew.writeInt(clientId);
		mplew.write(0);
		return mplew.getPacket();
	}
        
        /**
	 * Gets a packet telling the client the IP of the new channel.
	 * 
	 * @param ip The ip of the requested channel server.
	 * @param port The port the channel is on.
	 * @return The server IP packet.
	 */
	public static MaplePacket getChannelChange(byte[] ip, int port) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CHANGE_CHANNEL.getValue());
		mplew.write(1);
		mplew.write(ip);
		mplew.writeShort(port);
		return mplew.getPacket();
	}
        
        /**
	 * Adds item info to existing MaplePacketLittleEndianWriter.
	 * 
	 * @param mplew The MaplePacketLittleEndianWriter to write to.
	 * @param item The item to add info about.
	 * @param zeroPosition Is the position zero?
	 * @param leaveOut Leave out the item if position is zero?
	 */
        private static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean shortPos) {
            addItemInfo(mplew, item, shortPos, false);
        }
        
	private static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean shortPos, boolean zeroPosition) {
            Random rand = new Random();
            boolean cash = false;
            boolean ring = false;
            IEquip equip = null;
            if (item.getType() == IItem.EQUIP) {
                equip = (IEquip) item;
                if (equip.getRingId() > -1) {
                    ring = true;
                }
            }
            byte pos = zeroPosition ? 0 : item.getPosition();
            if (pos != 0) {
                if (shortPos) {
                    mplew.writeShort(pos);
                } else {
                    pos = (byte)Math.abs(pos);
                    if (pos > 100 || (item.getType() == MapleInventoryType.EQUIPPED.getType() && ring)) pos -= 100;
                    mplew.write(pos);
                }
            }
            boolean isEquip = (item.getItemId() / 1000000 == 1);
            boolean isPet = (item.getItemId() / 1000000 == 5);
            mplew.writeInt(item.getItemId());
            if (cash) {
                mplew.write(1);
                mplew.writeLong(rand.nextLong()); // Cash Item ID/SN
                mplew.writeLong(getKoreanTimestamp((long) (System.currentTimeMillis() * 1.2)));
            } else if (ring) {
                mplew.write(1);
                mplew.writeLong(equip.getRingId());
                mplew.writeLong(getKoreanTimestamp((long) (System.currentTimeMillis() * 1.2)));
            } else if (isPet) {
                mplew.write(1); // we use unique id like rings here
                mplew.writeLong(item.getPetId());
                mplew.writeLong(getKoreanTimestamp((long) (System.currentTimeMillis() * 1.2)));
                MaplePet pet = MaplePet.loadFromDb(item.getItemId(), item.getPosition(), item.getPetId());
                String petname = pet.getName();
                mplew.writeAsciiString(petname, 13);
                mplew.write(pet.getLevel());
                mplew.writeShort(pet.getCloseness());
                mplew.write(pet.getFullness());
                mplew.writeLong(getKoreanTimestamp((long) (System.currentTimeMillis() * 1.2))); // TODO: expirations
                return;
            } else {
                mplew.write(0);
                mplew.writeLong(rand.nextLong()); // Non-NX don't expire, so.. random 8 byte long here
            }
            if (isEquip) {
                mplew.write(equip.getUpgradeSlots());
                mplew.write(equip.getLevel());
                mplew.writeShort(equip.getStr());
                mplew.writeShort(equip.getDex());
                mplew.writeShort(equip.getInt());
                mplew.writeShort(equip.getLuk());
                mplew.writeShort(equip.getHp());
                mplew.writeShort(equip.getMp());
                mplew.writeShort(equip.getWatk());
                mplew.writeShort(equip.getMatk());
                mplew.writeShort(equip.getWdef());
                mplew.writeShort(equip.getMdef());
                mplew.writeShort(equip.getAcc());
                mplew.writeShort(equip.getAvoid());
                mplew.writeShort(equip.getHands());
                mplew.writeShort(equip.getSpeed());
                mplew.writeShort(equip.getJump());
            } else {
                mplew.writeShort(item.getQuantity());
            }
	}
        
        /**
	 * Gets character info for a character.
	 * 
	 * @param chr The character to get info about.
	 * @return The character info packet.
	 */
        public static MaplePacket getCharInfo(MapleCharacter chr) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.WARP_TO_MAP.getValue());
                
		mplew.writeInt(chr.getClient().getChannel() - 1); //channel
		mplew.write(0); // portals
		mplew.write(1); // connecting
                
                Random random = new Random();
                mplew.writeInt(random.nextInt()); // 3 random ints here
                mplew.writeInt(random.nextInt());
                mplew.writeInt(random.nextInt());
                
                mplew.writeInt(0);
                
                mplew.writeShort(-1); //flags
                mplew.writeInt(chr.getId());
                mplew.writeAsciiString(chr.getName(), 13);
                mplew.write(chr.getGender());
                mplew.write(chr.getSkinColor().getId());
                mplew.writeInt(chr.getFace());
                mplew.writeInt(chr.getHair());
                
                mplew.writeLong(0); // Pet SN
                
                mplew.write(chr.getLevel());
                mplew.writeShort(chr.getJob().getId());
                mplew.writeShort(chr.getStr());
                mplew.writeShort(chr.getDex());
                mplew.writeShort(chr.getInt());
                mplew.writeShort(chr.getLuk());
                mplew.writeShort(chr.getHp());
                mplew.writeShort(chr.getMaxHp());
                mplew.writeShort(chr.getMp());
                mplew.writeShort(chr.getMaxMp());
                mplew.writeShort(chr.getRemainingAp());
                mplew.writeShort(chr.getRemainingSp());
                mplew.writeInt(chr.getExp());
                mplew.writeShort(chr.getFame());
                
                mplew.writeInt(chr.getMapId());
                mplew.write(chr.getInitialSpawnpoint());
                
                mplew.writeLong(0); // dont know what this is lul
                mplew.writeInt(0);
                mplew.writeInt(0);
                
                mplew.write(chr.getBuddylist().getCapacity());
                mplew.writeInt(chr.getMeso());
                
                MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
		Collection<IItem> equippedList = iv.list();
                Item[] equipped = new Item[17];
                Item[] equippedCash = new Item[17];
		for (IItem item : equippedList) {
                    byte pos = item.getPosition();
                    if (pos < 0) {
                        pos = (byte)Math.abs(pos);
                        if (pos > 100) {
                            equippedCash[(byte)(pos - 100)] = (Item)item;
                        } else {
                            equipped[(byte)pos] = (Item)item;
                        }
                    }
                    if (pos < 0) {
                        if (pos < -100) {
                            pos += 100;
                            pos = (byte)Math.abs(pos);
                            equippedCash[(byte)(pos - 100)] = (Item)item;
                        } else {
                            pos = (byte)Math.abs(pos);
                            equipped[(byte)pos] = (Item)item;
                        }
                    }
		}
		for (Item item : equipped) {
                    if (item != null) {
                        addItemInfo(mplew, item, false);
                    }
		}
		mplew.write(0);
                for (Item item : equippedCash) {
                    if (item != null) {
                        addItemInfo(mplew, item, false);
                    }
		}
                mplew.write(0);
                for (byte i = 1; i < 6; i++) {
                    mplew.write(100); // TODO: REAL maxslots and not forced 100 to all players + fix the CS operation to increase
                    iv = chr.getInventory(MapleInventoryType.getByType((byte)i));
                    for (IItem item : iv.list()) {
                        if (item != null && item.getPosition() > 0) {
                            addItemInfo(mplew, item, false);
                        }
                    }
                    mplew.write(0);
                }
                Map<ISkill, MapleCharacter.SkillEntry> skills = chr.getSkills();
		mplew.writeShort(skills.size());
		for (Entry<ISkill, MapleCharacter.SkillEntry> skill : skills.entrySet()) {
                    mplew.writeInt(skill.getKey().getId());
                    mplew.writeInt((byte)skill.getValue().skillevel);
		}
                mplew.writeShort(0); // TODO: Quests
		mplew.writeShort(0); // TODO: Mini Games
                
                List<MapleRing> rings = getRing(chr);
                mplew.writeShort(rings.size());
                for (MapleRing ring : rings) {
                    mplew.writeInt(ring.getPartnerChrId());
                    mplew.writeAsciiString(StringUtil.getRightPaddedStr(ring.getPartnerName(), '\0', 13));
                    mplew.writeInt(ring.getRingId());
                    mplew.writeInt(0);
                    mplew.writeInt(ring.getPartnerRingId());
                    mplew.writeInt(0);
                }
                
                for (int map : chr.getVIPRockMaps()) {
                    mplew.writeInt(map);
                }
		return mplew.getPacket();
	}
        
        /**
	 * Gets a packet spawning a player as a mapobject to other clients.
	 * 
	 * @param chr The character to spawn to other clients.
	 * @return The spawn player packet.
	 */
	public static MaplePacket spawnPlayerMapobject(MapleCharacter chr) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SPAWN_PLAYER.getValue());
		mplew.writeInt(chr.getId());
		mplew.writeMapleAsciiString(chr.getName());

                int buffmask = 0;
		Integer buffvalue = null;
                
                if (chr.getBuffedValue(MapleBuffStat.DARKSIGHT) != null && !chr.isHidden()) {
                    buffmask |= MapleBuffStat.DARKSIGHT.getValue();
		}
                if (chr.getBuffedValue(MapleBuffStat.COMBO) != null) {
                    buffmask |= MapleBuffStat.COMBO.getValue();
                    buffvalue = Integer.valueOf(chr.getBuffedValue(MapleBuffStat.COMBO).intValue());
		}
                if (chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null) {
                    buffmask |= MapleBuffStat.SHADOWPARTNER.getValue();
		}
                
		mplew.writeInt(buffmask); // TODO: Finish buffmasks
                if (buffvalue != null) {
                    mplew.write(buffvalue.byteValue());
		}
                
                addCharLook(mplew, chr, true);
                mplew.writeInt(0); // [10/28/2014 8:25:40 PM] Justin: IncMaxMemberNumCost
                mplew.writeInt(chr.getItemEffect()); // TODO: Fix Chocolate Hearts (This is Cash Effect not Hearts)
                mplew.writeInt(chr.getChair());
                mplew.writeShort(chr.getPosition().x);
                mplew.writeShort(chr.getPosition().y);
                mplew.write(chr.getStance());
                mplew.writeShort(0); // FH
                
                // Pets
                if (chr.getPet(0) != null) {
                    mplew.write(1);
                    MaplePet pet = chr.getPet(0);
                    mplew.writeInt(pet.getItemId());
                    mplew.writeMapleAsciiString(pet.getName());
                    mplew.writeLong(pet.getUniqueId());
                    mplew.writeShort(pet.getPos().x);
                    mplew.writeShort(pet.getPos().y);
                    mplew.write(pet.getStance());
                    mplew.writeShort(pet.getFh());
                } else {
                    mplew.write(0);
                }
                
                // Mini Game & Interaction Boxes
                if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr)) {
                    addShopBox(mplew, chr.getPlayerShop());
		} else if ((MapleMiniGame) chr.getInteraction() != null) {
                    addAnnounceBox(mplew, chr.getInteraction());
                } else {
                    mplew.write(0);
		}
                
                // Rings
                List<MapleRing> rings = getRing(chr);
                mplew.write(rings.size());
                for (MapleRing ring : rings) { 
                    mplew.writeInt(ring.getRingId());
                    mplew.writeInt(0);
                    mplew.writeInt(ring.getPartnerRingId());
                    mplew.writeInt(0);
                    mplew.writeInt(ring.getItemId());
                }
		return mplew.getPacket();
	}
        
        private static List<MapleRing> getRing(MapleCharacter chr) {
                MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED); 
                Collection<IItem> equippedC = iv.list(); 
                List<Item> equipped = new ArrayList<Item>(equippedC.size()); 
                for (IItem item : equippedC) { 
                    equipped.add((Item) item); 
                } 
                Collections.sort(equipped); 
                List<MapleRing> rings = new ArrayList<MapleRing>(); 
                for (Item item : equipped) { 
                        if (((IEquip) item).getRingId() > -1) { 
                            rings.add(MapleRing.loadFromDb(((IEquip) item).getRingId())); 
                        } 
                } 
                Collections.sort(rings); 
		return rings;
	}
        
        /**
	 * Gets a server message packet.
	 * 
	 * @param message The message to convey.
	 * @return The server message packet.
	 */
	public static MaplePacket serverMessage(String message) {
		return serverMessage(4, 0, message, true, false);
	}

	/**
	 * Gets a server notice packet.
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0: [Notice]<br>
	 * 1: Popup<br>
	 * 2: Light blue background and lolwhut<br>
	 * 4: Scrolling message at top<br>
	 * 5: Pink Text<br>
	 * 6: Lightblue Text
	 * 
	 * @param type The type of the notice.
	 * @param message The message to convey.
	 * @return The server notice packet.
	 */
	public static MaplePacket serverNotice(int type, String message) {
            return serverMessage(type, 0, message, false, false);
	}

	/**
	 * Gets a server notice packet.
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0: [Notice]<br>
	 * 1: Popup<br>
	 * 2: Light blue background and lolwhut<br>
	 * 4: Scrolling message at top<br>
	 * 5: Pink Text<br>
	 * 6: Lightblue Text
	 * 
	 * @param type The type of the notice.
	 * @param channel The channel this notice was sent on.
	 * @param message The message to convey.
	 * @return The server notice packet.
	 */
	public static MaplePacket serverNotice(int type, int channel, String message) {
		return serverMessage(type, channel, message, false, false);
	}

	/**
	 * Gets a server message packet.
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0: [Notice]<br>
	 * 1: Popup<br>
	 * 2: Light blue background and lolwhut<br>
	 * 4: Scrolling message at top<br>
	 * 5: Pink Text<br>
	 * 6: Lightblue Text
	 * 
	 * @param type The type of the notice.
	 * @param channel The channel this notice was sent on.
	 * @param message The message to convey.
	 * @param servermessage Is this a scrolling ticker?
	 * @return The server notice packet.
	 */
	private static MaplePacket serverMessage(int type, int channel, String message, boolean servermessage, boolean whisper) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SERVERMESSAGE.getValue());
		mplew.write(type > 5 ? type == 6 ? 0 : 5 : type); // v40 beta only has 0: [Notice], 1: Popup, 2: Mega, 3: Smega, 4: Header, and 5: Red text
		if (servermessage) {
                    mplew.write(1);
		}
		mplew.writeMapleAsciiString(message);
		if (type == 3) {
                    mplew.write(channel - 1); // channel
                    mplew.write(whisper ? 1 : 0); // boolean whisper byte is used on super megaphones
		}
		return mplew.getPacket();
	}
        
        public static MaplePacket facialExpression(MapleCharacter from, int expression) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.FACIAL_EXPRESSION.getValue());
		mplew.writeInt(from.getId());
		mplew.writeInt(expression);
		return mplew.getPacket();
	}

        public static MaplePacket movePlayer(int cid, SeekableLittleEndianAccessor slea) {//List<LifeMovementFragment> moves
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MOVE_PLAYER.getValue());
		mplew.writeInt(cid);
                mplew.write(slea.read((int)slea.available()));
		return mplew.getPacket();
	}
        
        /**
	 * Gets a general chat packet.
	 * 
	 * @param chr The character who sent the chat.
	 * @param text The text of the chat.
	 * @return The general chat packet.
	 */
	public static MaplePacket getChatText(MapleCharacter chr, String text) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CHATTEXT.getValue());
		mplew.writeInt(chr.getId());
		mplew.write(chr.isGM() ? 1 : 0); // gms have this set to != 0, gives them white
		mplew.writeMapleAsciiString(text);
		return mplew.getPacket();
	}
        
        /**
	 * Gets a packet telling the client to change maps.
	 * 
	 * @param to The <code>MapleMap</code> to warp to.
	 * @param spawnPoint The spawn portal number to spawn at.
	 * @param chr The character warping to <code>to</code>
	 * @return The map change packet.
	 */
	public static MaplePacket getWarpToMap(MapleMap to, int spawnPoint, MapleCharacter chr) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.WARP_TO_MAP.getValue()); // 0x49
		mplew.writeInt(chr.getClient().getChannel() - 1);
		mplew.write(to.getPortals().size());
                mplew.write(0);
		mplew.writeInt(to.getId());
		mplew.write(spawnPoint);
		mplew.writeShort(chr.getHp()); // hp (???)
		return mplew.getPacket();
	}
        
        public static MaplePacket charInfo(MapleCharacter chr) {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(SendPacketOpcode.CHAR_INFO.getValue());
                mplew.writeInt(chr.getId());
                mplew.write(chr.getLevel());
                mplew.writeShort(chr.getJob().getId());
                mplew.writeShort(chr.getFame());
                mplew.writeMapleAsciiString(chr.isGM() ? chr.getName().equalsIgnoreCase("<3") || chr.getName().equalsIgnoreCase("Eric") ? "The Boss" : "Eric's bitch" : ""); //hehe
                if (chr.getPet(0) != null) {
                    MaplePet pet = chr.getPet(0);
                    mplew.write(1);
                    mplew.writeInt(pet.getItemId());
                    mplew.writeMapleAsciiString(pet.getName());
                    mplew.write(pet.getLevel());
                    mplew.writeShort(pet.getCloseness());
                    mplew.write(pet.getFullness());
                    mplew.writeInt(0); // Pet Equip ID? I can't find the ID in v40 so i'm not sure wtf this is
                } else {
                    mplew.write(0);
                }
                mplew.write(0); // Wishlist: while true, 4bytes (int)
                mplew.writeLong(0); // Unknown: Rings (?)
                return mplew.getPacket();
	}
        
        /**
	 * Gets a NPC spawn packet.
	 * 
	 * @param life The NPC to spawn.
	 * @return The NPC spawn packet.
	 */
	
        public static MaplePacket spawnNPC(MapleNPC life) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(SendPacketOpcode.SPAWN_NPC.getValue());
                mplew.writeInt(life.getObjectId());
		mplew.writeInt(life.getId());
		mplew.writeShort(life.getPosition().x);
		mplew.writeShort(life.getCy());
		mplew.write(life.isFacingLeft() ? 0 : 1); // faces left
		mplew.writeShort(life.getFh());
		mplew.writeShort(life.getRx0());
		mplew.writeShort(life.getRx1());
		return mplew.getPacket();
        }
        
        public static MaplePacket spawnNPCRequest(MapleNPC life) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
                mplew.write(0x01);
                mplew.writeInt(life.getObjectId());
		mplew.writeInt(life.getId());
		mplew.writeShort(life.getPosition().x);
		mplew.writeShort(life.getCy());
		mplew.write(life.isFacingLeft() ? 0 : 1); // faces left
		mplew.writeShort(life.getFh());
		mplew.writeShort(life.getRx0());
		mplew.writeShort(life.getRx1());
		return mplew.getPacket();
        }
        
        public static MaplePacket NPCAnimation(SeekableLittleEndianAccessor slea) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x7F);
            byte[] data = slea.read((int)slea.available());
            if (data.length == 5) {
                try {
                    mplew.writeInt(BitTools.toInt32(data, 0));
                    mplew.writeShort(BitTools.toInt16(data, 4));
                } catch (Exception e) {
                }
            } else {
                mplew.write(data);
            }
            return mplew.getPacket();
        }
        
        /**
	 * Adds the aesthetic aspects of a character to an existing
	 * MaplePacketLittleEndianWriter.
	 * 
	 * @param mplew The MaplePacketLittleEndianWrite instance to write the stats
	 *            to.
	 * @param chr The character to add the looks of.
	 * @param mega Unknown
	 */
	private static void addCharLook(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean mega) {
                if (mega) {
                    mplew.write(chr.getGender());
                }
		mplew.write(chr.getSkinColor().getId()); // skin color
		mplew.writeInt(chr.getFace()); // face
		mplew.write(0); // OdinMS: mega ? 1 : 0
		mplew.writeInt(chr.getHair()); // hair

                MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
		Collection<IItem> equippedList = iv.list();
                Item[] equipped = new Item[17];
                Item[] equippedCash = new Item[17];
		for (IItem item : equippedList) {
                    byte pos = item.getPosition();
                    if (pos < 0) {
                        pos = (byte)Math.abs(pos);
                        if (pos > 100) {
                            equippedCash[(byte)(pos - 100)] = (Item)item;
                        } else {
                            equipped[(byte)pos] = (Item)item;
                        }
                    }
                    if (pos < 0) {
                        if (pos < -100) {
                            pos += 100;
                            pos = (byte)Math.abs(pos);
                            equippedCash[(byte)(pos - 100)] = (Item)item;
                        } else {
                            pos = (byte)Math.abs(pos);
                            equipped[(byte)pos] = (Item)item;
                        }
                    }
		}
                Map<Byte, Integer> items = new LinkedHashMap<>();
                for (Item item : equippedCash) {
                    if (item != null) {
                        byte slotuse = (byte)Math.abs(item.getPosition());
                        if (slotuse > 100) slotuse -= 100;
                        items.put(slotuse, item.getItemId());
                    }
                }
                for (Item item : equipped) {
                    if (item != null && !items.containsKey((byte)Math.abs(item.getPosition()))) {
                        items.put((byte)Math.abs(item.getPosition()), item.getItemId());
                    }
                }
                for (Entry<Byte, Integer> entry : items.entrySet()) {
                    mplew.write(entry.getKey());
                    mplew.writeInt(entry.getValue());
                }
		mplew.write(0xFF);
                
		if (chr.getPet(0) != null) {
                    mplew.writeInt(chr.getPet(0).getItemId());
		} else {
                    mplew.writeInt(0); // Pet
		}
	}
        
        public static MaplePacket reportReply(byte type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.REPORT.getValue());
            mplew.write(type);
            return mplew.getPacket();
        }

	/**
	 * Gets an empty stat update.
	 * 
	 * @return The empy stat update packet.
	 */
	public static MaplePacket enableActions2() {
		return updatePlayerStats(EMPTY_STATUPDATE, true);
	}

	/**
	 * Gets an update for specified stats.
	 * 
	 * @param stats The stats to update.
	 * @return The stat update packet.
	 */
	public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Integer>> stats) {
            return updatePlayerStats(stats, false);
	}

	/**
	 * Gets an update for specified stats.
	 * 
	 * @param stats The list of stats to update.
	 * @param itemReaction Result of an item reaction(?)
	 * @return The stat update packet.
	 */
	public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Integer>> stats, boolean itemReaction) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.UPDATE_STATS.getValue());
		if (itemReaction) {
                    mplew.write(1);
		} else {
                    mplew.write(0);
		}
		int updateMask = 0;
		for (Pair<MapleStat, Integer> statupdate : stats) {
			updateMask |= statupdate.getLeft().getValue();
		}
		List<Pair<MapleStat, Integer>> mystats = stats;
		if (mystats.size() > 1) {
			Collections.sort(mystats, new Comparator<Pair<MapleStat, Integer>>() {

				@Override
				public int compare(Pair<MapleStat, Integer> o1, Pair<MapleStat, Integer> o2) {
					int val1 = o1.getLeft().getValue();
					int val2 = o2.getLeft().getValue();
					return (val1 < val2 ? -1 : (val1 == val2 ? 0 : 1));
				}
			});
		}
		mplew.writeInt(updateMask);
		for (Pair<MapleStat, Integer> statupdate : mystats) {
			if (statupdate.getLeft().getValue() >= 1) {
				if (statupdate.getLeft().getValue() == 0x1) {
                                    mplew.writeShort(statupdate.getRight().shortValue());
				} else if (statupdate.getLeft().getValue() <= 0x4) {
					mplew.writeInt(statupdate.getRight());
				} else if (statupdate.getLeft().getValue() < 0x20) {
					mplew.write(statupdate.getRight().shortValue());
				} else if (statupdate.getLeft().getValue() < 0xFFFF) {
					mplew.writeShort(statupdate.getRight().shortValue());
				} else {
					mplew.writeInt(statupdate.getRight().intValue());
				}
			}
		}
		return mplew.getPacket();
	}
        
        /**
	 * Gets a spawn monster packet.
	 * 
	 * @param life The monster to spawn.
	 * @param newSpawn Is it a new spawn?
	 * @return The spawn monster packet.
	 */
	public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn) {
		return spawnMonsterInternal(life, false, newSpawn, false, 0);
	}

	/**
	 * Gets a spawn monster packet.
	 * 
	 * @param life The monster to spawn.
	 * @param newSpawn Is it a new spawn?
	 * @param effect The spawn effect.
	 * @return The spawn monster packet.
	 */
	public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn, int effect) {
		return spawnMonsterInternal(life, false, newSpawn, false, effect);
	}

	/**
	 * Gets a control monster packet.
	 * 
	 * @param life The monster to give control to.
	 * @param newSpawn Is it a new spawn?
	 * @param aggro Aggressive monster?
	 * @return The monster control packet.
	 */
	public static MaplePacket controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
		return spawnMonsterInternal(life, true, newSpawn, aggro, 0);
	}

	/**
	 * Internal function to handler monster spawning and controlling.
	 * 
	 * @param life The mob to perform operations with.
	 * @param requestController Requesting control of mob?
	 * @param newSpawn New spawn (fade in?)
	 * @param aggro Aggressive mob?
	 * @param effect The spawn effect to use.
	 * @return The spawn/control packet.
	 */
	private static MaplePacket spawnMonsterInternal(MapleMonster life, boolean requestController, boolean newSpawn, boolean aggro, int effect) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		if (requestController) {
                    mplew.write(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
                    mplew.write(1);
		} else {
                    mplew.write(SendPacketOpcode.SPAWN_MONSTER.getValue());
		}
		mplew.writeInt(life.getObjectId());
		mplew.writeInt(life.getId());
		mplew.writeShort(life.getPosition().x);
		mplew.writeShort(life.getPosition().y);
                if (requestController) {
                    mplew.write(2);
                } else {
                    mplew.write((life.getController() != null ? 0x08 : 0x02));
                }
                mplew.writeShort(life.getFh()); // seems to be left and right
                mplew.writeShort(life.getStance());
		if (effect > 0) {
                    mplew.write(effect);
                    mplew.writeInt(life.getObjectId());
		}
		if (newSpawn) {
                    mplew.write(-2);
		} else {
                    mplew.write(-1);
		}
                mplew.write(0);
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	/**
	 * Gets a stop control monster packet.
	 * 
	 * @param oid The ObjectID of the monster to stop controlling.
	 * @return The stop control monster packet.
	 */
	public static MaplePacket stopControllingMonster(int oid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
		mplew.write(0);
                mplew.writeInt(0);// ?
		return mplew.getPacket();
	}

	/**
	 * Gets a response to a move monster packet.
	 * 
	 * @param objectid The ObjectID of the monster being moved.
	 * @param moveid The movement ID.
	 * @param currentMp The current MP of the monster.
	 * @param useSkills Can the monster use skills?
         * @param skill The skill used
         * @param level The level of the skill used
	 * @return The move response packet.
	 */
	public static MaplePacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, byte skill, byte level) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
		mplew.writeInt(objectid);
                mplew.writeShort(moveid);
                mplew.write(useSkills ? 1 : 0);
		mplew.writeShort(currentMp);
		mplew.write(skill); // skill
                mplew.write(level); // level
		return mplew.getPacket();
	}
        
        public static MaplePacket giveFameResponse(int mode, String charname, int newfame) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.FAME_RESPONSE.getValue());
		mplew.write(0);
		mplew.writeMapleAsciiString(charname);
		mplew.write(mode);
		mplew.writeInt(newfame);
		return mplew.getPacket();
	}

	/**
	 * status can be: <br>
	 * 0: ok, use giveFameResponse<br>
	 * 1: the username is incorrectly entered<br>
	 * 2: users under level 15 are unable to toggle with fame.<br>
	 * 3: can't raise or drop fame anymore today.<br>
	 * 4: can't raise or drop fame for this character for this month anymore.<br>
	 * 5: received fame, use receiveFame()<br>
	 * 6: level of fame neither has been raised nor dropped due to an unexpected
	 * error
	 * 
	 * @param status
	 * @param mode
	 * @param charname
	 * @param newfame
	 * @return
	 */
	public static MaplePacket giveFameErrorResponse(int status) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.FAME_RESPONSE.getValue());
		mplew.writeInt(status);
		return mplew.getPacket();
	}

	public static MaplePacket receiveFame(int mode, String charnameFrom) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.FAME_RESPONSE.getValue());
		mplew.write(5);
		mplew.writeMapleAsciiString(charnameFrom);
		mplew.write(mode);
		return mplew.getPacket();
	}

	public static MaplePacket partyCreated() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PARTY_OPERATION.getValue());
		mplew.write(7);
                for (int i = 0; i < 5; i++) {
                    if (i >= 0 && i <= 2)
                        mplew.writeInt(1);
                    else if (i >= 3 && i <= 4)
                        mplew.writeShort(1);
                }
		return mplew.getPacket();
	}

	public static MaplePacket partyInvite(MapleCharacter from) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PARTY_OPERATION.getValue());
		mplew.write(4);
		mplew.writeInt(from.getParty().getId());
		mplew.writeMapleAsciiString(from.getName());
		return mplew.getPacket();
	}

	/**
	 * 9: a beginner can't create a party<br>
	 * 10/13/18: your request for a party didn't work due to an unexpected error<br>
	 * 12: you have yet to join a party<br>
	 * 15: already have joined a party<br>
	 * 16: the party you are trying to join is already at full capacity<br>
	 * 17: unable to find the requested character in this channel<br>
	 * 
	 * @param message
	 * @return
	 */
	public static MaplePacket partyStatusMessage(int message) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PARTY_OPERATION.getValue());
		mplew.write(message);
		return mplew.getPacket();
	}

	/**
	 * 21: has denied the invitation<br>
	 * 
	 * @param message
	 * @param charname
	 * @return
	 */
	public static MaplePacket partyStatusMessage(int message, String charname) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PARTY_OPERATION.getValue());
		mplew.write(message);
		mplew.writeMapleAsciiString(charname);
		return mplew.getPacket();
	}

	private static void addPartyStatus(int forchannel, MapleParty party, LittleEndianWriter lew, boolean leaving) {
		List<MaplePartyCharacter> partymembers = new ArrayList<MaplePartyCharacter>(party.getMembers());
		while (partymembers.size() < 6) {
                    partymembers.add(new MaplePartyCharacter());
		}
		for (MaplePartyCharacter partychar : partymembers) {
                    lew.writeInt(partychar.getId());
		}
		for (MaplePartyCharacter partychar : partymembers) {
                    lew.writeAsciiString(StringUtil.getRightPaddedStr(partychar.getName(), '\0', 13));
                }
                for (MaplePartyCharacter partychar : partymembers) {
                    if (partychar.isOnline()) {
                        // This handles the Map IDs, but in v40 beta if you're
                        // not on the same map, the player appears offline. We
                        // have to eventually find a workaround for this.
                        int id = partychar.getMapid();
                        lew.writeInt(id);
                    } else {
                        lew.writeInt(-2);
                    }
                }
                lew.writeInt(party.getLeader().getId());
                for (MaplePartyCharacter partychar : partymembers) {
                    if (partychar.isOnline()) {
                        lew.writeInt(partychar.getChannel() - 1);
                    } else {
                        lew.writeInt(-2);
                    }
                }
                for (MaplePartyCharacter partychar : partymembers) {
                    if (partychar.getChannel() == forchannel && !leaving) {
                        lew.writeInt(partychar.getDoorTown());
                        lew.writeInt(partychar.getDoorTarget());
                        lew.writeInt(partychar.getDoorPosition().x);
                        lew.writeInt(partychar.getDoorPosition().y);
                    } else {
                        lew.writeInt(999999999);
                        lew.writeInt(999999999);
                        lew.writeInt(-1);
                        lew.writeInt(-1);
                    }
                }
	}

        public static MaplePacket updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PARTY_OPERATION.getValue());
		switch (op) {
                        case DISBAND:
			case EXPEL:
			case LEAVE:
				mplew.write(0xB);
                                mplew.writeInt(party.getId());
                                mplew.writeInt(target.getId());
				if (op == PartyOperation.DISBAND) {
                                    mplew.write(0);
				} else {
                                    mplew.write(1);
                                    if (op == PartyOperation.EXPEL) {
                                        mplew.write(1);
                                    } else {
                                        mplew.write(0);
                                    }
                                    mplew.writeMapleAsciiString(target.getName());
                                    addPartyStatus(forChannel, party, mplew, false);
				}

				break;
			case JOIN:
				mplew.write(0xE);
				mplew.writeInt(1);
				mplew.writeMapleAsciiString(target.getName());
				addPartyStatus(forChannel, party, mplew, false);
				break;
			case SILENT_UPDATE:
			case LOG_ONOFF: // for some reason this has problems updating when logging off lol idk
				mplew.write(0x06);
                                mplew.writeInt(party.getId());
                                addPartyStatus(forChannel, party, mplew, false);
				break;

		}
		return mplew.getPacket();
	}

	public static MaplePacket partyPortal(int townId, int targetId, Point position) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PARTY_OPERATION.getValue());
		mplew.writeShort(0x1A); // 0x22 v55
		mplew.writeInt(townId);
		mplew.writeInt(targetId);
		mplew.writeShort(position.x);
		mplew.writeShort(position.y);
		return mplew.getPacket();
	}

	public static MaplePacket updatePartyMemberHP(int cid, int curhp, int maxhp) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.UPDATE_PARTYMEMBER_HP.getValue());
		mplew.writeInt(cid);
		mplew.writeInt(curhp);
		mplew.writeInt(maxhp);
		return mplew.getPacket();
	}

	/**
	 * mode: 0 buddychat; 1 partychat; 2 guildchat
	 * 
	 * @param name
	 * @param chattext
	 * @param mode
	 * @return
	 */
	public static MaplePacket multiChat(String name, String chattext, int mode) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MULTICHAT.getValue());
		mplew.write(mode);
		mplew.writeMapleAsciiString(name);
		mplew.writeMapleAsciiString(chattext);
		return mplew.getPacket();
	}
        
        public static MaplePacket damageSummon(int cid, int summonSkillId, int damage, int unkByte, int monsterIdFrom) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.DAMAGE_SUMMON.getValue());
		mplew.writeInt(cid);
		mplew.writeInt(summonSkillId);
		mplew.write(unkByte);
                mplew.writeInt(monsterIdFrom);
		mplew.writeInt(damage);
		mplew.write(0);
		return mplew.getPacket();
	}

	public static MaplePacket damageMonster(int oid, int damage) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.DAMAGE_MONSTER.getValue());
		mplew.writeInt(oid);
		mplew.write(0);
		mplew.writeInt(damage);
		return mplew.getPacket();
	}

        public static MaplePacket updateBuddylist(Collection<BuddylistEntry> buddylist) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.BUDDYLIST.getValue());
		mplew.write(7);
		mplew.write(buddylist.size());
		for (BuddylistEntry buddy : buddylist) {
                    if (buddy.isVisible()) {
                        mplew.writeInt(buddy.getCharacterId()); // cid
                        mplew.writeAsciiString(StringUtil.getRightPaddedStr(buddy.getName(), '\0', 13));
                        mplew.write(0);
                        mplew.writeInt(buddy.getChannel() - 1);
                    }
		}
		for (int x = 0; x < buddylist.size(); x++) {
                    mplew.writeInt(0);
		}
		return mplew.getPacket();
	}

	public static MaplePacket requestBuddylistAdd(int cidFrom, String nameFrom) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.BUDDYLIST.getValue());
		mplew.write(9);
		mplew.writeInt(cidFrom);
		mplew.writeMapleAsciiString(nameFrom);
		mplew.writeInt(cidFrom);
		mplew.writeAsciiString(StringUtil.getRightPaddedStr(nameFrom, '\0', 13));
		mplew.write(1);
		mplew.writeInt(0);//?
                mplew.write(1);
		return mplew.getPacket();
	}

	public static MaplePacket updateBuddyChannel(int characterid, int channel) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.BUDDYLIST.getValue());
		mplew.write(0x14);
		mplew.writeInt(characterid);
		mplew.write(0);
		mplew.writeInt(channel);
		return mplew.getPacket();
	}

	public static MaplePacket updateBuddyCapacity(int capacity) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
		mplew.write(0x15);
		mplew.write(capacity);
		return mplew.getPacket();
	}
        
        public static MaplePacket buddylistMessage(byte message) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.BUDDYLIST.getValue());
		mplew.write(message);
		return mplew.getPacket();
	}

	public static MaplePacket showChair(short itemid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_CHAIR.getValue());
		mplew.write(itemid == -1 ? 0 : 1);
		if (itemid != -1) {
                    mplew.writeShort(itemid);
                }
		return mplew.getPacket();
	}

	public static MaplePacket musicChange(String song) {
		return environmentChange(song, 6);
	}

	public static MaplePacket showEffect(String effect) {
		return environmentChange(effect, 3);
	}

	public static MaplePacket playSound(String sound) {
		return environmentChange(sound, 4);
	}

	public static MaplePacket environmentChange(String env, int mode) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.BOSS_ENV.getValue());
		mplew.write(mode);
		mplew.writeMapleAsciiString(env);
		return mplew.getPacket();
	}

	public static MaplePacket startMapEffect(String msg, int itemid, boolean active) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MAP_EFFECT.getValue());
		mplew.write(active ? 0 : 1);
		mplew.writeInt(itemid);
		if (active)
                    mplew.writeMapleAsciiString(msg);
		return mplew.getPacket();
	}
        
        public static MaplePacket startKiteEffect(MapleCharacter chr, int itemid, String message) {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(0x88);
                mplew.writeInt(chr.getId());
                mplew.writeInt(itemid);
                mplew.writeMapleAsciiString(message);
                mplew.writeMapleAsciiString(chr.getName());
                mplew.writeShort(chr.getPosition().x);
                mplew.writeShort(chr.getPosition().y);
                return mplew.getPacket();
        }
        
        public static MaplePacket startJukebox(int itemid, String name) {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(0x32);
                mplew.writeInt(itemid);
                mplew.writeMapleAsciiString(name);
                return mplew.getPacket();
        }

	public static MaplePacket removeMapEffect() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MAP_EFFECT.getValue());
		mplew.write(0);
		mplew.writeInt(0);
		return mplew.getPacket();
	}
        
        public static MaplePacket removeKiteEffect(int id) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(0x89);
		mplew.write(0);
		mplew.writeInt(id); // id or 0?
		return mplew.getPacket();
	}
        
        public static MaplePacket showBuffeffect(int cid, int skillid, int effectid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		mplew.writeInt(cid); // ?
                mplew.write(effectid);
		mplew.writeInt(skillid);
                mplew.writeInt(1); // probably buff level but we don't know it and it doesn't really matter
		return mplew.getPacket();
	}

	public static MaplePacket showOwnBuffEffect(int skillid, int effectid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
                mplew.write(effectid);
                mplew.writeInt(skillid);
                mplew.write(1);
		return mplew.getPacket();
	}

	public static MaplePacket getClock(int time) { // time in seconds
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CLOCK.getValue());
		mplew.write(2);
		mplew.writeInt(time);
		return mplew.getPacket();
	}

	public static MaplePacket getClockTime(int hour, int min, int sec) { // Current Time
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CLOCK.getValue());
		mplew.write(1); //Clock-Type
		mplew.write(hour);
		mplew.write(min);
		mplew.write(sec);
		return mplew.getPacket();
	}
        
        /**
	 * Gets a packet to spawn a special map object.
	 * 
	 * @param chr The MapleCharacter who spawned the object.
	 * @param skill The skill used.
	 * @param skillLevel The level of the skill used.
	 * @param pos Where the object was spawned.
	 * @param movementType Movement type of the object.
	 * @param animated Animated spawn?
	 * @return The spawn packet for the map object.
	 */
	public static MaplePacket spawnSpecialMapObject(MapleCharacter chr, int skill, int skillLevel, Point pos, SummonMovementType movementType, boolean animated) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SPAWN_SPECIAL_MAPOBJECT.getValue());
		mplew.writeInt(chr.getId());
		mplew.writeInt(skill);
		mplew.write(skillLevel);
		mplew.writeShort(pos.x);
		mplew.writeShort(pos.y);
		mplew.write(0); // ?
		mplew.writeShort(0);
		mplew.write(movementType.getValue()); // 0 = don't move, 1 = follow
		mplew.write(animated ? 0 : 1);
		return mplew.getPacket();
	}

	/**
	 * Gets a packet to remove a special map object.
	 * 
	 * @param chr The MapleCharacter who removed the object.
	 * @param skill The skill used to create the object.
	 * @param animated Animated removal?
	 * @return The packet removing the object.
	 */
	public static MaplePacket removeSpecialMapObject(MapleCharacter chr, int skill, boolean animated) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.REMOVE_SPECIAL_MAPOBJECT.getValue());
		mplew.writeInt(chr.getId());
		mplew.writeInt(skill);
		mplew.write(animated ? 4 : 1); // ?
		return mplew.getPacket();
	}

	/**
	 * Gets a packet telling the client to show an EXP increase.
	 * 
	 * @param gain The amount of EXP gained.
	 * @param inChat In the chat box?
	 * @param white White text or yellow?
	 * @return The exp gained packet.
	 */
	public static MaplePacket getShowExpGain(int gain, boolean inChat, boolean white) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
		mplew.write(3); // 3 = exp, 4 = fame, 5 = mesos
		mplew.write(white ? 1 : 0);
		mplew.writeInt(gain);
		mplew.write(inChat ? 1 : 0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
		return mplew.getPacket();
	}

	/**
	 * Gets a packet telling the client to show a meso gain.
	 * 
	 * @param gain How many mesos gained.
	 * @return The meso gain packet.
	 */
	public static MaplePacket getShowMesoGain(int gain) {
		return getShowMesoGain(gain, false);
	}

	/**
	 * Gets a packet telling the client to show a meso gain.
	 * 
	 * @param gain How many mesos gained.
	 * @param inChat Show in the chat window?
	 * @return The meso gain packet.
	 */
	public static MaplePacket getShowMesoGain(int gain, boolean inChat) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
		if (!inChat) {
                    mplew.write(0);
                    mplew.write(1);
		} else {
                    mplew.write(5);
		}
		mplew.writeInt(gain);
		mplew.writeShort(0);
		return mplew.getPacket();
	}
        
        public static MaplePacket enableActions() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(0x12);
		mplew.write(1);
                mplew.write(0);
                mplew.write(0);
		return mplew.getPacket();
	}

	/**
	 * Gets a packet telling the client to show a item gain.
	 * 
	 * @param itemId The ID of the item gained.
	 * @param quantity How many items gained.
	 * @return The item gain packet.
	 */
	public static MaplePacket getShowItemGain(int itemId, short quantity) {
		return getShowItemGain(itemId, quantity, false);
	}

	/**
	 * Gets a packet telling the client to show an item gain.
	 * 
	 * @param itemId The ID of the item gained.
	 * @param quantity The number of items gained.
	 * @param inChat Show in the chat window?
	 * @return The item gain packet.
	 */
	public static MaplePacket getShowItemGain(int itemId, short quantity, boolean inChat) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		if (inChat) {
			mplew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
			mplew.write(3);
			mplew.write(1);
			mplew.writeInt(itemId);
			mplew.writeInt(quantity);
		} else {
			mplew.write(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
			mplew.writeShort(0);
			mplew.writeInt(itemId);
			mplew.writeInt(quantity);
			mplew.writeInt(0);
			mplew.writeInt(0);
		}
		return mplew.getPacket();
	}

	/**
	 * Gets a packet telling the client that a monster was killed.
	 * 
	 * @param oid The objectID of the killed monster.
	 * @param animation Show killed animation?
	 * @return The kill monster packet.
	 */
	public static MaplePacket killMonster(int oid, boolean animation) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.KILL_MONSTER.getValue());
		mplew.writeInt(oid);
		if (animation) {
                    mplew.write(1);
		} else {
                    mplew.write(0);
		}
		return mplew.getPacket();
	}

	/**
	 * Gets a packet telling the client to show mesos coming out of a map
	 * object.
	 * 
	 * @param amount The amount of mesos.
	 * @param itemoid The ObjectID of the dropped mesos.
	 * @param dropperoid The OID of the dropper.
	 * @param ownerid The ID of the drop owner.
	 * @param dropfrom Where to drop from.
	 * @param dropto Where the drop lands.
	 * @param mod ?
	 * @return The drop mesos packet.
	 */
	public static MaplePacket dropMesoFromMapObject(int amount, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod) {
            return dropItemFromMapObjectInternal(amount, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, true);
	}

	/**
	 * Gets a packet telling the client to show an item coming out of a map
	 * object.
	 * 
	 * @param itemid The ID of the dropped item.
	 * @param itemoid The ObjectID of the dropped item.
	 * @param dropperoid The OID of the dropper.
	 * @param ownerid The ID of the drop owner.
	 * @param dropfrom Where to drop from.
	 * @param dropto Where the drop lands.
	 * @param mod ?
	 * @return The drop mesos packet.
	 */
	public static MaplePacket dropItemFromMapObject(int itemid, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod) {
		return dropItemFromMapObjectInternal(itemid, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, false);
	}

	/**
	 * Internal function to get a packet to tell the client to drop an item onto
	 * the map.
	 * 
	 * @param itemid The ID of the item to drop.
	 * @param itemoid The ObjectID of the dropped item.
	 * @param dropperoid The OID of the dropper.
	 * @param ownerid The ID of the drop owner.
	 * @param dropfrom Where to drop from.
	 * @param dropto Where the drop lands.
	 * @param mod ?
	 * @param mesos Is the drop mesos?
	 * @return The item drop packet.
	 */
	public static MaplePacket dropItemFromMapObjectInternal(int itemid, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod, boolean mesos) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
		mplew.write(mod); // type
		mplew.writeInt(itemoid);
		mplew.write(mesos ? 1 : 0); // 1 = mesos, 0 =item
		mplew.writeInt(itemid);
		mplew.writeInt(ownerid);
		mplew.write(0); // drop.Type
		mplew.writeShort(dropto.x);
		mplew.writeShort(dropto.y);
                mplew.writeInt(dropperoid); 
		if (mod != 2) {
                    mplew.writeShort(dropfrom.x);
                    mplew.writeShort(dropfrom.y);
                    mplew.writeShort(0);
		}
		if (!mesos) {
                    addExpirationTime(mplew, System.currentTimeMillis(), false);
		}
                mplew.write(0); // player dropped
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket moveSummon(int cid, int summonSkill, SeekableLittleEndianAccessor slea) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MOVE_SUMMON.getValue());
		mplew.writeInt(cid);
		mplew.writeInt(summonSkill);
		mplew.write(slea.read((int)slea.available()));
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket moveMonster(int mobid, boolean useSkill, byte skill, Point startPos, SeekableLittleEndianAccessor slea) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MOVE_MONSTER.getValue());
		mplew.writeInt(mobid);
                mplew.write(useSkill ? 1 : 0);
		mplew.write(skill);
		mplew.writeInt(0);
		mplew.write(slea.read((int)slea.available()));
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket summonAttack(int cid, int skill, int stance, int direction, int numAttackedAndDamage, List<Pair<Integer, List<Integer>>> damage) {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SUMMON_ATTACK.getValue());
		addAttackSummon(mplew, cid, skill, stance, damage);
		return mplew.getPacket();
	}

	public static MaplePacket closeRangeAttack(int cid, int skill, int stance, int direction, int numAttackedAndDamage, List<Pair<Integer, List<Integer>>> damage) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CLOSE_RANGE_ATTACK.getValue());
		if (skill == 4211006) {
                    addMesoExplosion(mplew, cid, skill, stance, direction, numAttackedAndDamage, 0, damage);
		} else {
                    addAttackBody(mplew, cid, skill, stance, direction, numAttackedAndDamage, 0, damage);
		}
		return mplew.getPacket();
	}

	public static MaplePacket rangedAttack(int cid, int skill, int stance, int direction, int numAttackedAndDamage, int projectile, List<Pair<Integer, List<Integer>>> damage) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.RANGED_ATTACK.getValue()); // 0x54 in v40 tooo
		addAttackBody(mplew, cid, skill, stance, direction, numAttackedAndDamage, projectile, damage);
		return mplew.getPacket();
	}

	public static MaplePacket magicAttack(int cid, int skill, int stance, int direction, int numAttackedAndDamage, List<Pair<Integer, List<Integer>>> damage) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MAGIC_ATTACK.getValue());
		addAttackBody(mplew, cid, skill, stance, direction, numAttackedAndDamage, 0, damage);
		return mplew.getPacket();
	}

	private static void addAttackBody(MaplePacketLittleEndianWriter lew, int cid, int skill, int stance, int direction, int numAttackedAndDamage, int projectile, List<Pair<Integer, List<Integer>>> damage) {
            lew.writeInt(cid);
            lew.write(numAttackedAndDamage);
            if (skill > 0) {
                lew.write(0xFF); // skill level, too low and some skills don't work (?)
                lew.writeInt(skill);
            } else {
                lew.write(0);
            }
            lew.write(stance);
            lew.write(direction);
            lew.write(0); // mastery.. 
            lew.writeInt(projectile); // 00
            for (Pair<Integer, List<Integer>> oned : damage) {
                if (oned.getRight() != null) {
                    lew.writeInt(oned.getLeft().intValue());
                    lew.write(0x06);
                    for (Integer eachd : oned.getRight()) {
                        lew.writeInt(eachd.intValue());
                    }
                }
            }
            lew.writeLong(0);
	}
        
        private static void addAttackSummon(MaplePacketLittleEndianWriter lew, int cid, int skill, int stance, List<Pair<Integer, List<Integer>>> damage) {
            lew.writeInt(cid);
            lew.writeInt(skill); // 2311006 (Bishop - Summon Dragon) : SkillID == SummonID
            lew.write(stance); // ?
            lew.write(1); // numAttacked == 1
            for (Pair<Integer, List<Integer>> oned : damage) {
                if (oned.getRight() != null) {
                    lew.writeInt(oned.getLeft().intValue());
                    lew.write(0x06);
                    for (Integer eachd : oned.getRight()) {
                        lew.writeInt(eachd.intValue());
                    }
                }
            }
            lew.writeLong(0);
	}

	private static void addMesoExplosion(LittleEndianWriter lew, int cid, int skill, int stance, int direction, int numAttackedAndDamage, int projectile, List<Pair<Integer, List<Integer>>> damage) {
		lew.writeInt(cid);
		lew.write(numAttackedAndDamage);
		lew.write(0x1E);
		lew.writeInt(skill);
		lew.write(stance);
                lew.write(direction);
                lew.write(0); // mastery
		lew.writeInt(projectile); // 00
		for (Pair<Integer, List<Integer>> oned : damage) {
                    if (oned.getRight() != null) {
                        lew.writeInt(oned.getLeft().intValue());
                        lew.write(0xFF);
                        lew.write(oned.getRight().size());
                        for (Integer eachd : oned.getRight()) {
                            lew.writeInt(eachd.intValue());
                        }
                    }
		}
	}
        
        public static MaplePacket applyMonsterStatus(int oid, Map<MonsterStatus, Integer> stats, int skill, boolean monsterSkill, int delay) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
		mplew.writeInt(oid);
		int mask = 0;
		for (MonsterStatus stat : stats.keySet()) {
                    mask |= stat.getValue();
		}
		mplew.writeInt(mask);
		for (Integer val : stats.values()) {
			mplew.writeShort(val);
			if (monsterSkill) {
                            mplew.writeShort(skill);
                            mplew.writeShort(1);
			} else {
                            mplew.writeInt(skill);
			}
			mplew.writeShort(100); // buffTime, this needs to be coded properly -- as a workaround, we'll use 100.
		}
                
		mplew.writeShort(delay); // delay in ms
		return mplew.getPacket();
	}

	public static MaplePacket cancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
		mplew.writeInt(oid);
		int mask = 0;
		for (MonsterStatus stat : stats.keySet()) {
                    mask |= stat.getValue();
		}
		mplew.writeInt(mask);
		mplew.write(1);
		return mplew.getPacket();
	}

        public static MaplePacket getNPCShop(int sid, List<MapleShopItem> items) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.OPEN_NPC_SHOP.getValue());
		mplew.writeInt(sid);
                mplew.writeShort(items.size()); // item count
		for (MapleShopItem item : items) {
                        mplew.writeInt(item.getItemId());
                        mplew.writeInt(item.getPrice());
                        if (item.getItemId() / 10000 == 207) {
                            mplew.writeLong(5);
                        }
                        mplew.writeShort(item.getBuyable());
		}
		return mplew.getPacket();
	}
        
        /**
	 * code (8 = sell, 0 = buy, 0x20 = due to an error the trade did not happen
	 * o.o)
	 * 
	 * @param code
	 * @return
	 */
	public static MaplePacket confirmShopTransaction(byte code) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
		mplew.write(code); // recharge == 8?
		return mplew.getPacket();
	}
        
        /*
	 * 19 reference 00 01 00 = new while adding 01 01 00 = add from drop 00 01 01 = update count 00 01 03 = clear slot
	 * 01 01 02 = move to empty slot 01 02 03 = move and merge 01 02 01 = move and merge with rest
	 */
	public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item) {
		return addInventorySlot(type, item, false);
	}

	public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		if (fromDrop) {
                    mplew.write(1);
		} else {
                    mplew.write(1); // 0
		}
                mplew.write(1);
                mplew.write(0);
		mplew.write(type.getType()); // iv type
		addItemInfo(mplew, item, true);
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item) {
            return updateInventorySlot(type, item, false);
	}

	public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		if (fromDrop) {
                    mplew.write(1);
		} else {
                    mplew.write(0);
		}
		mplew.write(HexTool.getByteArrayFromHexString("01 01")); // update mode
		mplew.write(type.getType()); // iv type
		mplew.write(item.getPosition()); // slot id
		mplew.write(0); // ?
		mplew.writeShort(item.getQuantity());
		return mplew.getPacket();
	}

	public static MaplePacket moveInventoryItem(MapleInventoryType type, byte src, byte dst) {
		return moveInventoryItem(type, src, dst, (byte) -1);
	}

	public static MaplePacket moveInventoryItem(MapleInventoryType type, byte src, byte dst, byte equipIndicator) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("01 01 02"));
		mplew.write(type.getType());
		mplew.writeShort(src);
		mplew.writeShort(dst);
		if (equipIndicator != -1) {
                    mplew.write(equipIndicator);
		}
		return mplew.getPacket();
	}

	public static MaplePacket moveAndMergeInventoryItem(MapleInventoryType type, byte src, byte dst, short total) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("01 02 03"));
		mplew.write(type.getType());
		mplew.writeShort(src);
		mplew.write(1); // merge mode?

		mplew.write(type.getType());
		mplew.writeShort(dst);
		mplew.writeShort(total);
		return mplew.getPacket();
	}

	public static MaplePacket moveAndMergeWithRestInventoryItem(MapleInventoryType type, byte src, byte dst, short srcQ, short dstQ) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("01 02 01"));
		mplew.write(type.getType());
		mplew.writeShort(src);
		mplew.writeShort(srcQ);
		mplew.write(HexTool.getByteArrayFromHexString("01"));
		mplew.write(type.getType());
		mplew.writeShort(dst);
		mplew.writeShort(dstQ);
		return mplew.getPacket();
	}

	public static MaplePacket clearInventoryItem(MapleInventoryType type, byte slot, boolean fromDrop) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		mplew.write(fromDrop ? 1 : 0);
		mplew.write(HexTool.getByteArrayFromHexString("01 03"));
		mplew.write(type.getType());
		mplew.writeShort(slot);
		return mplew.getPacket();
	}

	public static MaplePacket removePlayerFromMap(int cid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
		mplew.writeInt(cid);
		return mplew.getPacket();
	}

        /**
         * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/>
         * 4 - explode<br/> cid is ignored for 0 and 1
         * 
         * @param oid
         * @param animation
         * @param cid
         * @return
        */
        public static MaplePacket removeItemFromMap(int oid, int animation, int cid) {
            return removeItemFromMap(oid, animation, cid, false, 0);
        }
        
	/**
	 * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/>
	 * 4 - explode<br/> cid is ignored for 0 and 1
	 * 
	 * @param oid
	 * @param animation
	 * @param cid
	 * @return
	 */
	public static MaplePacket removeItemFromMap(int oid, int animation, int cid, boolean pet, int slot) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
		mplew.write(animation); // expire
		mplew.writeInt(oid);
		if (animation >= 2) {
                    mplew.writeInt(cid);
                    if (pet) {
                        mplew.write(slot);
                    }
		}
		return mplew.getPacket();
	}

	public static MaplePacket updateCharLook(MapleCharacter chr) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.UPDATE_CHAR_LOOK.getValue());
		mplew.writeInt(chr.getId());
                mplew.writeInt(0);
		mplew.write(1);
		
		addCharLook(mplew, chr, false);
                
                mplew.writeInt(0); // Unknown: 4bytes (int)
                
                mplew.write(0); // Unknown: 8bytes (int, int)
                mplew.write(0); // Unknown: 1bytes (byte)
                
                List<MapleRing> rings = getRing(chr);
		mplew.write(rings.size());  // Rings: 16 bytes (long, long)
                for (MapleRing ring : rings) { 
                    mplew.writeInt(ring.getRingId());
                    mplew.writeInt(0);
                    mplew.writeInt(ring.getPartnerRingId());
                    mplew.writeInt(0);
                    mplew.writeInt(ring.getItemId());
                }
		return mplew.getPacket();
	}

	public static MaplePacket dropInventoryItem(MapleInventoryType type, short src) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("01 01 03"));
		mplew.write(type.getType());
		mplew.writeShort(src);
		if (src < 0) {
                    mplew.write(1);
		}
		return mplew.getPacket();
	}

	public static MaplePacket dropInventoryItemUpdate(MapleInventoryType type, IItem item) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("01 01 01"));
		mplew.write(type.getType());
		mplew.writeShort(item.getPosition());
		mplew.writeShort(item.getQuantity());
		return mplew.getPacket();
	}

	public static MaplePacket damagePlayer(int skill, int monsteridfrom, int cid, int damage) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.DAMAGE_PLAYER.getValue());
		mplew.writeInt(cid);
                mplew.write(0xFE);
		mplew.writeInt(damage);
		mplew.writeInt(damage);
                mplew.writeInt(damage);
		mplew.writeLong(0);
                mplew.writeLong(0);
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	private static <E extends LongValueHolder> int getIntMask(List<Pair<E, Integer>> statups) {
		int mask = 0x0;
		for (Pair<E, Integer> statup : statups) {
                    mask += statup.getLeft().getValue();
		}
		return mask;
	}

	private static <E extends LongValueHolder> int getIntMaskFromList(List<E> statups) {
		int mask = 0;
		for (E statup : statups) {
                    mask |= statup.getValue();
		}
		return mask;
	}

	/**
	 * It is important that statups is in the correct order (see decleration
	 * order in MapleBuffStat) since this method doesn't do automagical
	 * reordering.
	 * 
	 * @param buffid
	 * @param bufflength
	 * @param statups
	 * @return
	 */
	public static MaplePacket giveBuff(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.GIVE_BUFF.getValue());
		int mask = getIntMask(statups);
		mplew.writeInt(mask); // :|
		for (Pair<MapleBuffStat, Integer> statup : statups) {
                    mplew.writeShort(statup.getRight().shortValue());
                    mplew.writeInt(buffid);
                    mplew.writeShort(bufflength);
		}
                mplew.writeLong(0);
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket giveForeignBuff(int cid, List<Pair<MapleBuffStat, Integer>> statups) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
		mplew.writeInt(cid);
		int mask = getIntMask(statups);
		mplew.writeInt(mask);
		for (Pair<MapleBuffStat, Integer> statup : statups) {
                    mplew.writeShort(statup.getRight().byteValue());
		}
		mplew.writeShort(0); // same as give_buff
		return mplew.getPacket();
	}

	public static MaplePacket cancelForeignBuff(int cid, List<MapleBuffStat> statups) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
		mplew.writeInt(cid);
		mplew.writeInt(getIntMaskFromList(statups));
		return mplew.getPacket();
	}

	public static MaplePacket cancelBuff(List<MapleBuffStat> statups) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.CANCEL_BUFF.getValue());
		mplew.writeInt(getIntMaskFromList(statups));
		mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket getNPCTalk(int npc, String talk, boolean prev, boolean next, boolean simple, boolean yesno) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.NPC_TALK.getValue());
		mplew.write(4); // ?
		mplew.writeInt(npc);
                mplew.write((simple ? 4 : yesno ? 1 : 0));
		mplew.writeMapleAsciiString(talk);
                if ((prev || next) && (!simple || !yesno)) { // dont send next,previous,or ok with yesno/simple else crash
                    mplew.write(prev ? 1 : 0);
                    mplew.write(next ? 1 : 0);
                }
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket getNPCTalkStyle(int npc, String talk, int styles[]) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.NPC_TALK.getValue());
		mplew.write(4); // ?
		mplew.writeInt(npc);
		mplew.write(5);
		mplew.writeMapleAsciiString(talk);
		mplew.write(styles.length);
		for (int i = 0; i < styles.length; i++) {
                    mplew.writeInt(styles[i]);
		}
		return mplew.getPacket();
	}

	public static MaplePacket getNPCTalkNum(int npc, String talk, int def, int min, int max) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.NPC_TALK.getValue());
		mplew.write(4); // ?
		mplew.writeInt(npc);
		mplew.write(3);
		mplew.writeMapleAsciiString(talk);
		mplew.writeInt(def);
		mplew.writeInt(min);
		mplew.writeInt(max);
		mplew.writeLong(0); // prob int but w/e
		return mplew.getPacket();
	}

	public static MaplePacket getNPCTalkText(int npc, String talk) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.NPC_TALK.getValue());
		mplew.write(4); // ?
		mplew.writeInt(npc);
		mplew.write(2);
		mplew.writeMapleAsciiString(talk);
                mplew.writeMapleAsciiString(""); // def
		mplew.writeShort(0); // min 
                mplew.writeShort(0); // max
		mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket showLevelup(int cid) {
		return showForeignEffect(cid, 0);
	}

	public static MaplePacket showJobChange(int cid) {
		return showForeignEffect(cid, 8);
	}

	public static MaplePacket showForeignEffect(int cid, int effect) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		mplew.writeInt(cid); // ?
		mplew.write(effect);
                mplew.writeLong(0);
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket updateSkill(int skillid, int level, int masterlevel) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.UPDATE_SKILLS.getValue());
		mplew.write(1);
		mplew.writeShort(1);
		mplew.writeInt(skillid);
		mplew.writeInt(level);
		mplew.write(1);
		return mplew.getPacket();
	}

	public static MaplePacket updateQuestMobKills(MapleQuestStatus status) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
		mplew.write(1);
		mplew.writeShort(status.getQuest().getId());
		mplew.write(1);
		String killStr = "";
		for (int kills : status.getMobKills().values()) {
			killStr += StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3);
		}
		mplew.writeMapleAsciiString(killStr);
		mplew.writeInt(0);
		mplew.writeInt(0);
		return mplew.getPacket();
	}

	public static MaplePacket getShowQuestCompletion(int id) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
		mplew.writeShort(id);
		return mplew.getPacket();
	}
        
        public static MaplePacket getWhisper(String sender, int channel, String text) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.WHISPER.getValue());
		mplew.write(0x12);
		mplew.writeMapleAsciiString(sender);
		mplew.write(channel - 1); // I guess this is the channel
		mplew.writeMapleAsciiString(text);
		return mplew.getPacket();
	}

	/**
	 * 
	 * @param target name of the target character
	 * @param reply error code: 0x0 = cannot find char, 0x1 = success
	 * @return the MaplePacket
	 */
	public static MaplePacket getWhisperReply(String target, byte reply) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.WHISPER.getValue());
		mplew.write(0x0A); // whisper?
		mplew.writeMapleAsciiString(target);
		mplew.write(reply);
		return mplew.getPacket();
	}

	public static MaplePacket getFindReplyWithMap(String target, int mapid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.WHISPER.getValue());
		mplew.write(9);
		mplew.writeMapleAsciiString(target);
		mplew.write(1);
		mplew.writeInt(mapid);
		mplew.writeInt(0); // ?? official doesn't send zeros here but whatever
		return mplew.getPacket();
	}

	public static MaplePacket getFindReply(String target, int channel) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.WHISPER.getValue());
		mplew.write(9);
		mplew.writeMapleAsciiString(target);
		mplew.write(3);
		mplew.writeInt(channel - 1);
                mplew.writeInt(0);
		return mplew.getPacket();
	}

	public static MaplePacket getInventoryFull() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		mplew.write(1);
		mplew.write(0);
                mplew.write(0);//?
		return mplew.getPacket();
	}
        
        public static MaplePacket messengerInvite(String from, int messengerid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.MESSENGER.getValue());
            mplew.write(3);
            mplew.writeMapleAsciiString(from);
            mplew.write(0);
            mplew.writeInt(messengerid);
            mplew.write(0);
            return mplew.getPacket();
        }

        public static MaplePacket addMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.MESSENGER.getValue());
            mplew.write(0);
            mplew.write(position);
            addCharLook(mplew, chr, true);
            mplew.writeMapleAsciiString(from);
            mplew.write(channel);
            mplew.write(0);
            return mplew.getPacket();
        }

        public static MaplePacket removeMessengerPlayer(int position) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.MESSENGER.getValue());
            mplew.write(2);
            mplew.write(position);
            return mplew.getPacket();
        }

        public static MaplePacket updateMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.MESSENGER.getValue());
            mplew.write(7);
            mplew.write(position);
            addCharLook(mplew, chr, true);
            mplew.writeMapleAsciiString(from);
            mplew.write(channel);
            mplew.write(0);
            return mplew.getPacket();
        }

        public static MaplePacket joinMessenger(int position) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.MESSENGER.getValue());
            mplew.write(1);
            mplew.write(position);
            return mplew.getPacket();
        }

        public static MaplePacket messengerChat(String text) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.MESSENGER.getValue());
            mplew.write(6);
            mplew.writeMapleAsciiString(text);
            return mplew.getPacket();
        }

        public static MaplePacket messengerNote(String text, int mode, int mode2) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.MESSENGER.getValue());
            mplew.write(mode);
            mplew.writeMapleAsciiString(text);
            mplew.write(mode2);
            return mplew.getPacket();
        }
        
        // CASH SHOP PACKETS -- Credits to WvsBeta and OdinMS.
        public static MaplePacket sendWishList(int characterid, boolean update) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0xBB);
            if (update) {
                mplew.write(0x20);
            } else {
                mplew.write(0x1E);
            }
            Connection con = DatabaseConnection.getConnection();
            int i = 10;

            try {
                PreparedStatement ps = con.prepareStatement("SELECT sn FROM wishlist WHERE charid = ? LIMIT 10");
                ps.setInt(1, characterid);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    mplew.writeInt(rs.getInt("sn"));
                    i--;
                }
                rs.close();
                ps.close();
            } catch (SQLException se) {
                log.info("Error getting wishlist data:", se);
            }
            while (i > 0) {
                mplew.writeInt(0);
                i--;
            }
            return mplew.getPacket();
        }

        public static MaplePacket warpCS(MapleClient c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            MapleCharacter chr = c.getPlayer();
            mplew.write(0x27);
            
            mplew.writeShort(-1); //flags
            mplew.writeInt(chr.getId());
            mplew.writeAsciiString(chr.getName(), 13);
            mplew.write(chr.getGender());
            mplew.write(chr.getSkinColor().getId());
            mplew.writeInt(chr.getFace());
            mplew.writeInt(chr.getHair());

            mplew.writeLong(0); // pet serial number

            mplew.write(chr.getLevel());
            mplew.writeShort(chr.getJob().getId());
            mplew.writeShort(chr.getStr());
            mplew.writeShort(chr.getDex());
            mplew.writeShort(chr.getInt());
            mplew.writeShort(chr.getLuk());
            mplew.writeShort(chr.getHp());
            mplew.writeShort(chr.getMaxHp());
            mplew.writeShort(chr.getMp());
            mplew.writeShort(chr.getMaxMp());
            mplew.writeShort(chr.getRemainingAp());
            mplew.writeShort(chr.getRemainingSp());
            mplew.writeInt(chr.getExp());
            mplew.writeShort(chr.getFame());

            mplew.writeInt(chr.getMapId());
            mplew.write(chr.getInitialSpawnpoint());

            mplew.writeLong(0); // who knows
            mplew.writeInt(13);
            mplew.writeInt(12);

            mplew.write(chr.getBuddylist().getCapacity());
            mplew.writeInt(chr.getMeso());

            MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
            Collection<IItem> equippedList = iv.list();
            Item[] equipped = new Item[17];
            Item[] equippedCash = new Item[17];
            for (IItem item : equippedList) {
                byte pos = item.getPosition();
                if (pos < 0) {
                    pos = (byte)Math.abs(pos);
                    if (pos > 100) {
                        equippedCash[(byte)(pos - 100)] = (Item)item;
                    } else {
                        equipped[(byte)pos] = (Item)item;
                    }
                }
                if (pos < 0) {
                    if (pos < -100) {
                        pos += 100;
                        pos = (byte)Math.abs(pos);
                        equippedCash[(byte)(pos - 100)] = (Item)item;
                    } else {
                        pos = (byte)Math.abs(pos);
                        equipped[(byte)pos] = (Item)item;
                    }
                }
            }
            for (Item item : equipped) {
                if (item != null) {
                    addItemInfo(mplew, item, false);
                }
            }
            mplew.write(0);
            for (Item item : equippedCash) {
                if (item != null) {
                    addItemInfo(mplew, item, false);
                }
            }
            mplew.write(0);
            for (byte i = 1; i < 6; i++) {
                mplew.write(100); // TODO: custom max slots for GMS-like cash shop purposes
                iv = chr.getInventory(MapleInventoryType.getByType((byte)i));
                for (IItem item : iv.list()) {
                    if (item != null && item.getPosition() > 0) {
                        addItemInfo(mplew, item, false);
                    }
                }
                mplew.write(0);
            }

            Map<ISkill, MapleCharacter.SkillEntry> skills = chr.getSkills();
            mplew.writeShort(skills.size());
            for (Entry<ISkill, MapleCharacter.SkillEntry> skill : skills.entrySet()) {
                mplew.writeInt(skill.getKey().getId());
                mplew.writeInt((byte)skill.getValue().skillevel);
            }

            mplew.writeShort(0); // Quests
            mplew.writeShort(0); // Mini Games
            mplew.writeShort(0); // Rings
            
            for (int i = 0; i < 5; i++) { // Teleport rocks isn't short, it's 4 ints T_T
                mplew.writeInt(910000000 + i);
            }

            mplew.write(1); // 0 = beta or someshit lol
            mplew.writeMapleAsciiString(c.getAccountName()); 
            
            mplew.writeShort(0); // wishlist i think.. o.o
            
            //int[] itemz = new int[]{10000281, 10000282, 10000283, 10000284, 10000285};
            for (byte i = 1; i <= 8; i++) {
                for (byte j = 0; j <= 1; j++) {
                        mplew.writeInt(10000281); // best items, these are just first id's in Commodity
                        mplew.writeInt(i);
                        mplew.writeInt(j);
                        
                        mplew.writeInt(10000282);
                        mplew.writeInt(i);
                        mplew.writeInt(j);
                        
                        mplew.writeInt(10000283);
                        mplew.writeInt(i);
                        mplew.writeInt(j);
                        
                        mplew.writeInt(10000284);
                        mplew.writeInt(i);
                        mplew.writeInt(j);
                        
                        mplew.writeInt(10000285);
                        mplew.writeInt(i);
                        mplew.writeInt(j);
                }
            }
            
            mplew.writeShort(5); // Stock 
            mplew.writeInt(-1); // 1 = Sold Out, 2 = Not Sold      
            mplew.writeInt(20900028);
            mplew.writeInt(0); // 1 = Sold Out, 2 = Not Sold
            mplew.writeInt(20900027);
            mplew.writeInt(2); // 1 = Sold Out, 2 = Not Sold
            mplew.writeInt(20900026);
            mplew.writeInt(4); // 1 = Sold Out, 2 = Not Sold
            mplew.writeInt(20900026);
            mplew.writeInt(5); // 1 = Sold Out, 2 = Not Sold
            mplew.writeInt(20900026);
                    
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            
            return mplew.getPacket();
        }
        
        public static MaplePacket showBoughtCSItem(MapleCharacter chr, CashItemInfo item) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0xBB);
            mplew.write(0x22);
            
            mplew.writeLong(item.hashCode()); // Cash ID
            mplew.writeInt(chr.getId()); // Player ID
            mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01"));
            mplew.writeInt(item.getId());
            mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01"));
            mplew.writeShort(item.getCount()); // quantity is always 1?
            mplew.writeAsciiString(ii.getName(item.getId()), 13);
            mplew.writeLong(0); // Expiration.. items never expire so we shouldnt have a problem.
            mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01"));
            return mplew.getPacket();
        }
        
        public static MaplePacket sendItemInventory(CashItemInfo item, short slot) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0xBB);
            mplew.write(0x2F);
            
            mplew.writeShort(slot);
            mplew.write(2);//ServerConstants.getInventoryType(item.getId()).getType());
            Item i = new Item(2090000, (byte)slot, (short)1);
            addItemInfo(mplew, i, false, true);
            return mplew.getPacket();
        }
        
        public static MaplePacket showNXMapleTokens(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0xBA);
            mplew.writeInt(chr.getCSPoints(1)); // Paypal/PayByCash NX
            mplew.writeInt(chr.getCSPoints(2)); // Maple Points
            return mplew.getPacket();
        }
        
        // TODO, the players inventory of the items bought
        public static MaplePacket sendItems(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0xBB);
            mplew.write(0x1C);
            
            mplew.write(0);
            // for each byte 
            mplew.writeShort(10);

            return mplew.getPacket();
        }
        
        // TODO: Gifting
        public static MaplePacket showGifts(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0xBB);
            mplew.write(0x1E);
            mplew.writeAsciiString("fasfsa", 13);
            mplew.writeAsciiString("asfas", 73);
            return mplew.getPacket();
        }
        
        // END OF CASH SHOP PACKETS
        
        public static MaplePacket getStorage(int npcId, byte slots, Collection<IItem> items, int meso) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.OPEN_STORAGE.getValue());
		mplew.writeInt(npcId);
		mplew.write(slots);
		mplew.writeShort(0x7E);
		mplew.writeInt(meso);
                byte size1 = 0, size2 = 0, size3 = 0, size4 = 0, size5 = 0;
                for (IItem item : items) {
                    switch(item.getItemId() / 1000000) {
                        case 1: size1++; break;
                        case 2: size2++; break;
                        case 3: size3++; break;
                        case 4: size4++; break;
                        case 5: size5++; break;
                        default: System.out.println("Unknown type found!"); break;
                    }
                }
                mplew.write(size1);
		for (IItem item : items) { // Equip
                    if (item.getItemId() / 1000000 == 1) {
                        addItemInfo(mplew, item, false, true);
                    }
		}
		mplew.write(size2); 
		for (IItem item : items) { // Use
                    if (item.getItemId() / 1000000 == 2) {
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.write(size3); // Setup
		for (IItem item : items) {
                    if (item.getItemId() / 1000000 == 3) { 
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.write(size4); // Etc
		for (IItem item : items) {
                    if (item.getItemId() / 1000000 == 4) { 
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.write(size5); // Pets
		for (IItem item : items) {
                    if (item.getItemId() / 1000000 == 5) { 
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket getStorageFull() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write((SendPacketOpcode.OPEN_STORAGE.getValue() + 1));
		mplew.write(0x0C);
		return mplew.getPacket();
	}

	public static MaplePacket mesoStorage(byte slots, int meso) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write((SendPacketOpcode.OPEN_STORAGE.getValue() + 1)); // 0xA8
		mplew.write(0x0E);
		mplew.write(slots);
		mplew.writeShort(2);
		mplew.writeInt(meso);
		return mplew.getPacket();
	}

	public static MaplePacket storeStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write((SendPacketOpcode.OPEN_STORAGE.getValue() + 1));
		mplew.write(0x09);
		mplew.write(slots);
		mplew.writeShort(type.getBitfieldEncoding());
		byte size1 = 0, size2 = 0, size3 = 0, size4 = 0, size5 = 0;
                for (IItem item : items) {
                    switch(item.getItemId() / 1000000) {
                        case 1: size1++; break;
                        case 2: size2++; break;
                        case 3: size3++; break;
                        case 4: size4++; break;
                        case 5: size5++; break;
                        default: System.out.println("Unknown type found!"); break;
                    }
                }
                mplew.write(size1);
		for (IItem item : items) { // Equip
                    if (item.getItemId() / 1000000 == 1) {
                        addItemInfo(mplew, item, false, true);
                    }
		}
		mplew.write(size2); 
		for (IItem item : items) { // Use
                    if (item.getItemId() / 1000000 == 2) {
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.write(size3); // Setup
		for (IItem item : items) {
                    if (item.getItemId() / 1000000 == 3) { 
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.write(size4); // Etc
		for (IItem item : items) {
                    if (item.getItemId() / 1000000 == 4) { 
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.write(size5); // Pets
		for (IItem item : items) {
                    if (item.getItemId() / 1000000 == 5) { 
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.writeLong(0);
		return mplew.getPacket();
	}

	public static MaplePacket takeOutStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write((SendPacketOpcode.OPEN_STORAGE.getValue() + 1));
		mplew.write(0x07);
		mplew.write(slots);
		mplew.writeShort(type.getBitfieldEncoding());
		byte size1 = 0, size2 = 0, size3 = 0, size4 = 0, size5 = 0;
                for (IItem item : items) {
                    switch(item.getItemId() / 1000000) {
                        case 1: size1++; break;
                        case 2: size2++; break;
                        case 3: size3++; break;
                        case 4: size4++; break;
                        case 5: size5++; break;
                        default: System.out.println("Unknown type found!"); break;
                    }
                }
                mplew.write(size1);
		for (IItem item : items) { // Equip
                    if (item.getItemId() / 1000000 == 1) {
                        addItemInfo(mplew, item, false, true);
                    }
		}
		mplew.write(size2); 
		for (IItem item : items) { // Use
                    if (item.getItemId() / 1000000 == 2) {
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.write(size3); // Setup
		for (IItem item : items) {
                    if (item.getItemId() / 1000000 == 3) { 
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.write(size4); // Etc
		for (IItem item : items) {
                    if (item.getItemId() / 1000000 == 4) { 
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.write(size5); // Pets
		for (IItem item : items) {
                    if (item.getItemId() / 1000000 == 5) { 
                        addItemInfo(mplew, item, false, true);
                    }
		}
                mplew.writeLong(0);
		return mplew.getPacket();
	}
        
        public static MaplePacket spawnMist(int oid, int ownerCid, int skillId, Rectangle mistPosition) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SPAWN_MIST.getValue());
		mplew.writeInt(oid); // maybe this should actually be the "mistid" - seems to always be 1 with only one mist in the map...
		mplew.write(0); // from mob? o.O
		mplew.writeInt(skillId);
		mplew.write(1); // skill level
		mplew.writeShort(7); // delay i guess
		mplew.writeInt(mistPosition.x); // left position
		mplew.writeInt(mistPosition.y); // bottom position
		mplew.writeInt(mistPosition.x + mistPosition.width); // left position
		mplew.writeInt(mistPosition.y + mistPosition.height); // upper position
		return mplew.getPacket();
	}

	public static MaplePacket removeMist(int oid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.REMOVE_MIST.getValue());
		mplew.writeInt(oid);
		return mplew.getPacket();
	}
        
        // is there a way to spawn reactors non-animated?
	public static MaplePacket spawnReactor(MapleReactor reactor) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		Point pos = reactor.getPosition();
		mplew.write(SendPacketOpcode.REACTOR_SPAWN.getValue());
		mplew.writeShort(reactor.getObjectId());
                mplew.write(reactor.getState());
		mplew.writeShort(pos.x);
		mplew.writeShort(pos.y);
		mplew.write(2); // Rx0?
                mplew.write(2); // Rx1?
		return mplew.getPacket();
	}
	
	public static MaplePacket triggerReactor(MapleReactor reactor, int stance) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		Point pos = reactor.getPosition();
		mplew.write(SendPacketOpcode.REACTOR_HIT.getValue());
		mplew.writeShort(reactor.getObjectId());
		mplew.write(reactor.getState());
		mplew.writeShort(pos.x);
		mplew.writeShort(pos.y);
		mplew.writeShort(stance);
		mplew.write(0);
		//frame delay, set to 5 since there doesn't appear to be a fixed formula for it
		mplew.write(5);
		return mplew.getPacket();
	}
	
	public static MaplePacket destroyReactor(MapleReactor reactor) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.REACTOR_DESTROY.getValue());
		mplew.writeShort(reactor.getObjectId());
		return mplew.getPacket();
	}
        
        /**
	 * Gets a packet to spawn a portal.
	 * 
	 * @param townId The ID of the town the portal goes to.
	 * @param targetId The ID of the target.
	 * @param pos Where to put the portal.
	 * @return The portal spawn packet.
	 */
	public static MaplePacket spawnPortal(int townId, int targetId, Point pos) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SPAWN_PORTAL.getValue());
		mplew.writeInt(townId);
		mplew.writeInt(targetId);
		if (pos != null) {
                    mplew.writeShort(pos.x);
                    mplew.writeShort(pos.y);
		}
		return mplew.getPacket();
	}

	/**
	 * Gets a packet to spawn a door.
	 * 
	 * @param oid The door's object ID.
	 * @param pos The position of the door.
	 * @param town
	 * @return The remove door packet.
	 */
	public static MaplePacket spawnDoor(int oid, Point pos, boolean town) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SPAWN_DOOR.getValue());
		mplew.write(town ? 1 : 0);
		mplew.writeInt(oid);//?
		mplew.writeShort(pos.x);
		mplew.writeShort(pos.y);
		return mplew.getPacket();
	}

	/**
	 * Gets a packet to remove a door.
	 * 
	 * @param oid The door's ID.
	 * @param town
	 * @return The remove door packet.
	 */
	public static MaplePacket removeDoor(int oid, boolean town) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		if (town) {
			mplew.write(SendPacketOpcode.SPAWN_PORTAL.getValue());
			mplew.writeInt(999999999);
			mplew.writeInt(999999999);
		} else {
			mplew.write(SendPacketOpcode.REMOVE_DOOR.getValue());
			mplew.write(1); // or is it 0? lol
			mplew.writeInt(oid);
		}
		return mplew.getPacket();
	}
        
        /**
         * 
         * @param oid object id of boss
         * @param currHP current hp of boss
         * @param maxHP max hp of boss
         * @param tagColor the color of the hpbar
         * @param tagBgColor the color of the background for the hpbar
         * @return 
         */
	public static MaplePacket showBossHP(int oid, int currHP, int maxHP, byte tagColor, byte tagBgColor) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.BOSS_ENV.getValue());
		mplew.write(5);
		mplew.writeInt(currHP);
		mplew.writeInt(maxHP);
		mplew.writeInt(tagColor);
		mplew.writeInt(tagBgColor);
		return mplew.getPacket();
	}
        
        public static MaplePacket updatePet(MaplePet pet, boolean alive) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
            mplew.write(0);
            mplew.write(2);
            mplew.write(3);
            mplew.write(5);
            mplew.write(pet.getPosition());
            mplew.writeShort(0);
            mplew.write(5);
            mplew.write(pet.getPosition());
            mplew.write(0);
            mplew.writeInt(pet.getItemId());
            mplew.write(1);
            mplew.writeLong(pet.getUniqueId());
            mplew.writeLong(getKoreanTimestamp((long) (System.currentTimeMillis() * 1.2)));
            String petname = pet.getName();
            mplew.writeAsciiString(petname, 13);
            mplew.write(pet.getLevel());
            mplew.writeShort(pet.getCloseness());
            mplew.write(pet.getFullness());
            if (alive) {
                mplew.writeLong(getKoreanTimestamp((long) (System.currentTimeMillis() * 1.2)));
                mplew.writeInt(0);
            } else {
                mplew.writeLong(0);
            }
            return mplew.getPacket();
        }

        public static MaplePacket showPet(MapleCharacter chr, MaplePet pet, boolean remove) {
            return showPet(chr, pet, remove, false);
        }

        public static MaplePacket showPet(MapleCharacter chr, MaplePet pet, boolean remove, boolean hunger) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.SPAWN_PET.getValue());
            mplew.writeInt(chr.getId());
            if (remove) {
                mplew.write(0);
                mplew.write(hunger ? 1 : 0);
            } else {
                mplew.write(1);
                mplew.writeInt(pet.getItemId());
                mplew.writeMapleAsciiString(pet.getName());
                mplew.writeLong(pet.getUniqueId());
                mplew.writeShort(pet.getPos().x);
                mplew.writeShort(pet.getPos().y - 12);
                mplew.write(pet.getStance());
                mplew.writeInt(pet.getFh());
            }
            return mplew.getPacket();
        }

        public static MaplePacket movePet(int cid, SeekableLittleEndianAccessor slea) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.MOVE_PET.getValue());
            mplew.writeInt(cid);
            mplew.write(slea.read((int)slea.available()));
            return mplew.getPacket();
        }

        public static MaplePacket petChat(int cid, int un, String text, int slot) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PET_CHAT.getValue());
            mplew.writeInt(cid);
            mplew.write(slot);
            mplew.write(un);
            mplew.writeMapleAsciiString(text);
            return mplew.getPacket();
        }

        public static MaplePacket commandResponse(int cid, byte command, boolean success, boolean food) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PET_COMMAND.getValue());
            mplew.writeInt(cid);
            mplew.write(command == 1 ? 1 : 0);
            mplew.write(command);
            mplew.write(success ? 1 : 0);
            return mplew.getPacket();
        }

        public static MaplePacket showOwnPetLevelUp(int index) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
            mplew.write(4);
            mplew.write(0);
            mplew.write(index); // Pet Index? o.O
            return mplew.getPacket();
        }

        public static MaplePacket showPetLevelUp(MapleCharacter chr, int slot) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.write(4);
            mplew.write(0);
            mplew.write(slot);
            return mplew.getPacket();
        }

        public static MaplePacket changePetName(MapleCharacter chr, String newname, int slot) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PET_NAMECHANGE.getValue());
            mplew.writeInt(chr.getId());
            mplew.writeMapleAsciiString(newname);
            mplew.write(slot); // Pet index o.O
            return mplew.getPacket();
        }

        public static MaplePacket petStatUpdate(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.UPDATE_STATS.getValue());
            int mask = 0;
            mask |= MapleStat.PET.getValue();
            mplew.write(1);
            mplew.writeInt(mask);
            MaplePet pet = chr.getPet(0);
            if (pet != null) {
                mplew.writeInt(pet.getUniqueId());
                mplew.writeInt(0);
            } else {
                mplew.writeLong(0);
            }
            mplew.write(1);
            return mplew.getPacket();
        }
        
        /**
	 * state 0 = del ok state 12 = invalid bday
	 * 
	 * @param cid
	 * @param state
	 * @return
	 */
	public static MaplePacket deleteCharResponse(int cid, int state) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
		mplew.writeInt(cid);
		mplew.write(state);
		return mplew.getPacket();
	}
        
        public static MaplePacket refreshVIPRockMapList(int[] maps, byte type) {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(0x1C);
                mplew.write(type);
                for (int map : maps) {
                    mplew.writeInt(map);
                }
                return mplew.getPacket();
        }
        
        public static MaplePacket showNotes(ResultSet notes, int count) throws SQLException {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x1B); // OnMemoResult
            mplew.write(1);
            mplew.write(count);
            for (int i = 0; i < count; i++) {
                mplew.writeInt(notes.getInt("id"));
                mplew.writeMapleAsciiString(notes.getString("from"));
                mplew.writeMapleAsciiString(notes.getString("message"));
                mplew.writeLong(getKoreanTimestamp(notes.getLong("timestamp")));
                mplew.write(0);
                notes.next();
            }
            return mplew.getPacket();
        }
        
        /**
	 * 
	 * @param c
	 * @param shop
	 * @param owner
	 * @return
	 */
	public static MaplePacket getPlayerShop(MapleClient c, MaplePlayerShop shop, boolean owner) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("05 04 04"));
		mplew.write(owner ? 0 : 1);
		mplew.write(0);
		addCharLook(mplew, shop.getOwner(), true);
		mplew.writeMapleAsciiString(shop.getOwner().getName());
		MapleCharacter[] visitors = shop.getVisitors();
		for (int i = 0; i < visitors.length; i++) {
                    if (visitors[i] != null) {
                        mplew.write(i + 1);
                        addCharLook(mplew, visitors[i], true);
                        mplew.writeMapleAsciiString(visitors[i].getName());
                    }
		}
		mplew.write(0xFF);
		mplew.writeMapleAsciiString(shop.getDescription());
		List<MaplePlayerShopItem> items = shop.getItems();
		mplew.write(0x10);
		mplew.write(items.size());
		for (MaplePlayerShopItem item : items) {
                    mplew.writeShort(item.getBundles());
                    mplew.writeShort(item.getItem().getQuantity());
                    mplew.writeInt(item.getPrice());
                    addItemInfo(mplew, item.getItem(), true);
		}
		return mplew.getPacket();
	}

	public static MaplePacket getTradeStart(MapleClient c, MapleTrade trade, byte number) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("05 03 02"));
		mplew.write(number);
		if (number == 1) {
                    mplew.write(0);
                    addCharLook(mplew, trade.getPartner().getChr(), true);
                    mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
		}
		mplew.write(number);
		addCharLook(mplew, c.getPlayer(), true);
		mplew.writeMapleAsciiString(c.getPlayer().getName());
		mplew.write(0xFF);
		return mplew.getPacket();
	}

	public static MaplePacket getTradeConfirmation() {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(0xF);
		return mplew.getPacket();
	}

	public static MaplePacket getTradeCompletion(byte number, boolean success) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(0xA);
		mplew.write(number);
		mplew.write(success ? 5 : 6);
		return mplew.getPacket();
	}

	public static MaplePacket getTradeCancel(byte number) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(0xA);
		mplew.write(number);
		mplew.write(2);
		return mplew.getPacket();
	}
        
        public static MaplePacket getTradePartnerAdd(MapleCharacter c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("04 01"));
		addCharLook(mplew, c, true);
		mplew.writeMapleAsciiString(c.getName());
		return mplew.getPacket();
	}

	public static MaplePacket getTradeInvite(MapleCharacter c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("02 03"));
		mplew.writeMapleAsciiString(c.getName());
		mplew.write(HexTool.getByteArrayFromHexString("B7 50 00 00")); // the room id, int
		return mplew.getPacket();
	}

	public static MaplePacket getTradeMesoSet(byte number, int meso) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(0xE);
		mplew.write(number);
		mplew.writeInt(meso);
		return mplew.getPacket();
	}

	public static MaplePacket getTradeItemAdd(byte number, IItem item) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(0xD);
                mplew.write(number);
                mplew.write(item.getPosition());
                mplew.write(item.getType()); // 1 equip, 2 use(?), 5 pet
		addItemInfo(mplew, item, false, true);
		return mplew.getPacket();
	}
        
        public static MaplePacket sendInteractionBox(MapleCharacter c) {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(SendPacketOpcode.UPDATE_CHAR_BOX.getValue()); // 0x40
                mplew.writeInt(c.getId());
                addAnnounceBox(mplew, c.getInteraction());
                return mplew.getPacket();
        }

        public static MaplePacket removeCharBox(MapleCharacter c) {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
                mplew.writeInt(c.getId());
                mplew.writeInt(0);
                return mplew.getPacket();
        }

	public static MaplePacket getPlayerShopChat(MapleCharacter c, String chat, boolean owner) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("06 08"));
		mplew.write(owner ? 0 : 1);
		mplew.writeMapleAsciiString(c.getName() + " : " + chat);
		return mplew.getPacket();
	}
        
        public static MaplePacket itemEffect(int characterid, int itemid) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_ITEM_EFFECT.getValue());
		mplew.writeInt(characterid);
		mplew.writeInt(itemid);
		return mplew.getPacket();
	}
        
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////
        ////////////////////////////////////////////////// ALL OUTDATED PACKETS //////////////////////////////////////////////////

	public static MaplePacket getPermBan(byte reason) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.LOGIN_STATUS.getValue());
		mplew.writeShort(0x02); // Account is banned
		mplew.write(0x0);
		mplew.write(reason);
		mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01 00"));
		return mplew.getPacket();
	}

	public static MaplePacket getTempBan(long timestampTill, byte reason) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
		mplew.write(0x02);
		mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00")); // Account is banned
		mplew.write(reason);
		mplew.writeLong(timestampTill); // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.
		return mplew.getPacket();
	}

	/**
	 * Adds a quest info entry for a character to an existing
	 * MaplePacketLittleEndianWriter.
	 * 
	 * @param mplew The MaplePacketLittleEndianWrite instance to write the stats
	 *            to.
	 * @param chr The character to add quest info about.
	 */
	private static void addQuestInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
		List<MapleQuestStatus> started = chr.getStartedQuests();
		mplew.writeShort(started.size());
		for (MapleQuestStatus q : started) {
			mplew.writeInt(q.getQuest().getId());
		}
		List<MapleQuestStatus> completed = chr.getCompletedQuests();
		mplew.writeShort(completed.size());
		for (MapleQuestStatus q : completed) {
			mplew.writeShort(q.getQuest().getId());
			mplew.writeInt(KoreanDateUtil.getQuestTimestamp(q.getCompletionTime()));
			mplew.writeInt(KoreanDateUtil.getQuestTimestamp(q.getCompletionTime()));
		}
	}

	/**
	 * Adds expiration time info to an existing MaplePacketLittleEndianWriter.
	 * 
	 * @param mplew The MaplePacketLittleEndianWriter to write to.
	 * @param time The expiration time.
	 * @param showexpirationtime Show the expiration time?
	 */
	private static void addExpirationTime(MaplePacketLittleEndianWriter mplew, long time, boolean showexpirationtime) {
		mplew.writeInt(KoreanDateUtil.getItemTimestamp(time));
		mplew.write(showexpirationtime ? 1 : 2);
	}
        
        public static MaplePacket scrolledItem(IItem scroll, IItem item, boolean destroyed) {
		// 18 00 01 02 03 02 08 00 03 01 F7 FF 01
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
		mplew.write(1); // fromdrop always true
		if (destroyed) {
                    mplew.write(2);
		} else {
                    mplew.write(3); // is this 1? xD
		}
		if (scroll.getQuantity() > 0) {
                    mplew.write(1);
		} else {
                    mplew.write(3);
		}
		mplew.write(MapleInventoryType.USE.getType());
		mplew.writeShort(scroll.getPosition());
		if (scroll.getQuantity() > 0) {
                    mplew.writeShort(scroll.getQuantity());
		}
		mplew.write(3);
		if (!destroyed) {
                    mplew.write(MapleInventoryType.EQUIP.getType());
                    mplew.writeShort(item.getPosition());
                    mplew.write(0);
		}
		mplew.write(MapleInventoryType.EQUIP.getType());
		mplew.writeShort(item.getPosition());
		if (!destroyed) {
                    addItemInfo(mplew, item, true);
		}
		mplew.write(1);
		return mplew.getPacket();
	}

	public static MaplePacket getScrollEffect(ScrollResult scrollSuccess) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
		mplew.write(6);
		switch (scrollSuccess) {
                    case SUCCESS:
                        mplew.write(2); // 1: Nothing | 2: Nothing? wtf..
                        break;
                    case FAIL:
                        mplew.write(0);
                        break;
                    case CURSE:
                        mplew.write(1);
                        break;
                    default:
                        throw new IllegalArgumentException("effects don't even work in v40 beta and we found a new one? wut");
		}
		return mplew.getPacket();
	}

	/**
	 * 
	 * @param c
	 * @param quest
	 * @return
	 */
	public static MaplePacket startQuest(MapleCharacter c, short quest) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
		mplew.write(1);
		mplew.writeShort(quest);
		mplew.writeShort(1);
		mplew.write(0);
		mplew.writeInt(0);
		mplew.writeInt(0);
		return mplew.getPacket();
	}

	/**
	 * 
	 * @param c
	 * @param quest
	 * @return
	 */
	public static MaplePacket forfeitQuest(MapleCharacter c, short quest) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
		mplew.write(1);
		mplew.writeShort(quest);
		mplew.writeShort(0);
		mplew.write(0);
		mplew.writeInt(0);
		mplew.writeInt(0);
		return mplew.getPacket();
	}

	/**
	 * 
	 * @param c
	 * @param quest
	 * @return
	 */
	public static MaplePacket completeQuest(MapleCharacter c, short quest) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
		mplew.write(1);
		mplew.writeShort(quest);
		mplew.write(HexTool.getByteArrayFromHexString("02 A0 67 B9 DA 69 3A C8 01"));
		mplew.writeInt(0);
		mplew.writeInt(0);
		return mplew.getPacket();
	}

	/**
	 * 
	 * @param c
	 * @param quest
	 * @param npc
	 * @param progress
	 * @return
	 */
	public static MaplePacket updateQuestInfo(MapleCharacter c, short quest, int npc, byte progress) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(0x72); 
		mplew.write(progress);
		mplew.writeShort(quest);
		mplew.writeInt(npc);
		mplew.writeInt(0);
		return mplew.getPacket();
	}
        
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////
        ////////////////////////////// Working but needs fixing MiniGame packets //////////////////////////////

	/**
	 * Adds a announcement box to an existing MaplePacketLittleEndianWriter.
	 * 
	 * @param mplew The MaplePacketLittleEndianWriter to add an announcement box
	 *            to.
	 * @param shop The shop to announce.
	 */
	private static void addAnnounceBox(MaplePacketLittleEndianWriter mplew, IPlayerInteractionManager interaction) {
                mplew.write(interaction.getShopType());
                if (interaction.getShopType() == 4) {
                    mplew.writeInt(((MaplePlayerShop) interaction).getObjectId());
                } else {
                    mplew.writeInt(((MapleMiniGame) interaction).getObjectId());
                }
                mplew.writeMapleAsciiString(interaction.getDescription()); // desc
                mplew.write(0); // TODO: passwords
                mplew.write(interaction.getShopType() == 2 ? ((MapleMiniGame) interaction).getMatchesToWin() == 6 ? 2 : ((MapleMiniGame) interaction).getMatchesToWin() == 10 ? 1 : 0 : 0);
                mplew.write(1);
                mplew.write(interaction.getFreeSlot() > -1 ? 4 : 1);
                if (interaction.getShopType() == 4) {
                    mplew.write(0);
                } else {
                    mplew.write(((MapleMiniGame) interaction).getStarted() ? 1 : 0);
                }
        }
        
        private static void addShopBox(MaplePacketLittleEndianWriter mplew, MaplePlayerShop shop) {
		// 00: no game
		// 01: omok game
		// 02: card game
		// 04: shop
		mplew.write(4);
		mplew.writeInt(shop.getObjectId()); // gameid/shopid
		mplew.writeMapleAsciiString(shop.getDescription()); // desc
		// 00: public
		// 01: private
		mplew.write(0);
		// 00: red 4x3
		// 01: green 5x4
		// 02: blue 6x5
		// omok:
		// 00: normal
		mplew.write(0);
		// first slot: 1/2/3/4
		// second slot: 1/2/3/4
		mplew.write(1);
		mplew.write(4);
		// 0: open
		// 1: in progress
		mplew.write(0);
	}

	public static MaplePacket getPlayerShopNewVisitor(MapleCharacter c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(HexTool.getByteArrayFromHexString("04 02"));
		addCharLook(mplew, c, true);
		mplew.writeMapleAsciiString(c.getName());
		return mplew.getPacket();
	}

	public static MaplePacket getPlayerShopItemUpdate(MaplePlayerShop shop) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
		mplew.write(0x15);
		mplew.write(shop.getItems().size());
		for (MaplePlayerShopItem item : shop.getItems()) {
			mplew.writeShort(item.getBundles());
			mplew.writeShort(item.getItem().getQuantity());
			mplew.writeInt(item.getPrice());
                        mplew.write(item.getItem().getType());
                        addItemInfo(mplew, item.getItem(), false, true);
		}
		return mplew.getPacket();
	}

	public static MaplePacket updateCharBox(MapleCharacter c) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
		mplew.writeInt(c.getId());
		if (c.getPlayerShop() != null) {
                    addShopBox(mplew, c.getPlayerShop());
		} else {
                    mplew.writeInt(0);
		}
		return mplew.getPacket();
	}
        
        public static MaplePacket shopItemUpdate(IPlayerInteractionManager shop) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x17);
            if (shop.getShopType() == 1) {
                mplew.writeInt(0);
            }
            mplew.write(shop.getItems().size());
            for (net.sf.odinms.server.PlayerInteraction.MaplePlayerShopItem item : shop.getItems()) { // LULUL
                mplew.writeShort(item.getBundles());
                mplew.writeShort(item.getItem().getQuantity());
                mplew.writeInt(item.getPrice());
                addItemInfo(mplew, item.getItem(), true, true);
            }
            return mplew.getPacket();
        }
        
        public static MaplePacket addVisitor(MapleCharacter chr, boolean firstTime) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue()); 
            mplew.write(4);
            mplew.write(1);
            addCharLook(mplew, chr, true);
            mplew.writeMapleAsciiString(chr.getName());
            
            mplew.writeInt(1); // TODO: get stats
            mplew.writeInt(1);
            mplew.writeInt(1);
            mplew.writeInt(1);
            mplew.writeInt(2000);
            
            return mplew.getPacket();
        }
        
        public static MaplePacket getInteraction(MapleCharacter chr, boolean firstTime) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue()); // header.
            IPlayerInteractionManager ips = chr.getInteraction();
            int type = ips.getShopType();
            if (type == 1) {
                mplew.write(HexTool.getByteArrayFromHexString("05 01 04"));
            } else if (type == 2) {
                mplew.write(HexTool.getByteArrayFromHexString("05 02 02"));
            } else if (type == 4) {
                mplew.write(HexTool.getByteArrayFromHexString("05 04 04")); // might be wrong
            }
            mplew.write(ips.isOwner(chr) ? 0 : 1);
            mplew.write(0);
            if (type == 1 || type == 2) {
                addCharLook(mplew, ((MapleMiniGame) ips).getOwner(), true);
                mplew.writeMapleAsciiString(ips.getOwnerName());
            } else {
                addCharLook(mplew, ((net.sf.odinms.server.PlayerInteraction.MaplePlayerShop) ips).getMCOwner(), false);
                mplew.writeMapleAsciiString(ips.getOwnerName());
            }
            for (int i = 0; i < 3; i++) {
                if (ips.getVisitors()[i] != null) {
                    mplew.write(i + 1);
                    addCharLook(mplew, ips.getVisitors()[i], true);
                    mplew.writeMapleAsciiString(ips.getVisitors()[i].getName());
                }
            }
            mplew.write(0xFF);
            if (type == 1 || type == 2) {
                MapleMiniGame minigame = (MapleMiniGame) ips;
                mplew.write(0);
                if (type == 1 || type == 2) {
                    mplew.writeInt(1);
                } else {
                    mplew.writeInt(2);
                }
                mplew.writeInt(minigame.getOmokPoints("wins", true));
                mplew.writeInt(minigame.getOmokPoints("ties", true));
                mplew.writeInt(minigame.getOmokPoints("losses", true));
                mplew.writeInt(2000);
                if (ips.getVisitors()[0] != null) {
                    mplew.write(1);
                    if (type == 1 || type == 2) {
                        mplew.writeInt(1);
                    } else {
                        mplew.writeInt(2);
                    }
                    mplew.writeInt(minigame.getOmokPoints("wins", false));
                    mplew.writeInt(minigame.getOmokPoints("ties", false));
                    mplew.writeInt(minigame.getOmokPoints("losses", false));
                    mplew.writeInt(2000);
                }
                mplew.write(0xFF);
            }
            mplew.writeMapleAsciiString(ips.getDescription());
            if (type == 1 || type == 2) {
                mplew.write(ips.getItemType());
                mplew.write(0);
                mplew.write(0);
            }
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGame(MapleCharacter chr, int slot) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            MapleMiniGame game = (MapleMiniGame) chr.getInteraction();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(5); 
            mplew.write(chr.getInteraction().getShopType()); 
            mplew.write(4); // max slots is actually 4 in v40 but ya
            mplew.write(slot);
            if (slot == 0) {
                mplew.write(0);
                addCharLook(mplew, chr, true);
                mplew.writeMapleAsciiString(chr.getName());
            } else {
                MapleCharacter owner = game.getOwner();
                mplew.write(0);
                addCharLook(mplew, owner, true);
                mplew.writeMapleAsciiString(owner.getName());
                mplew.write(1);
                addCharLook(mplew, chr, true);
                mplew.writeMapleAsciiString(chr.getName());
            }
            mplew.write(0xFF);
            
            mplew.write(0); // Slot 0 -- Owner
            mplew.writeInt(game.getShopType()); // Game Type (1 = Omok)
            mplew.writeInt(game.getOmokPoints("wins", true)); // Wins
            mplew.writeInt(game.getOmokPoints("ties", true)); // Ties
            mplew.writeInt(game.getOmokPoints("losses", true)); // Losses
            mplew.writeInt(2000); // Score

            if (slot >= 1) {
                mplew.write(1); // Slot 1 -- Opponent
                mplew.writeInt(game.getShopType()); // Game Type (1 = Omok)
                mplew.writeInt(game.getOmokPoints("wins", false)); // Wins
                mplew.writeInt(game.getOmokPoints("ties", false)); // Ties
                mplew.writeInt(game.getOmokPoints("losses", false)); // Losses
                mplew.writeInt(2000); // Score
            }
            mplew.write(0xFF);
            
            mplew.writeMapleAsciiString(game.getDescription());
            mplew.write(0); // Piece type
            
            mplew.write(0);
            mplew.write(0);
            return mplew.getPacket();
        }
        
        public static MaplePacket shopChat(String message, int slot) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(HexTool.getByteArrayFromHexString("06 08"));
            mplew.write(slot);
            mplew.writeMapleAsciiString(message);
            return mplew.getPacket();
        }

        public static MaplePacket shopErrorMessage(int error, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x0A);
            mplew.write(type);
            mplew.write(error);
            return mplew.getPacket();
        }
        
        public static MaplePacket shopVisitorLeave(int slot) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x0A);
            if (slot > 0) { // owner != visitor
                mplew.write(slot);
            }
            return mplew.getPacket();
        }
        
        public static MaplePacket getMiniBoxFull() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(3);
            mplew.write(2);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameStart(int loser) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x23);
            mplew.write(loser == 0 ? 1 : 0);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameSkipTurn(int slot) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x39);
            mplew.write(slot);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameReady() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x20);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameUnReady() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x21);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameRequestTie() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x18);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameDenyTie() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x19);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameMoveOmok(int move1, int move2, int move3) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x26);
            mplew.writeInt(move1);
            mplew.writeInt(move2);
            mplew.write(move3);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameWin(MapleMiniGame game, int person) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(HexTool.getByteArrayFromHexString("38 00"));
            mplew.write(person);
            mplew.writeInt(1); // start of owner; unknown
            mplew.writeInt(game.getOmokPoints("wins", true)); // wins
            mplew.writeInt(game.getOmokPoints("ties", true)); // ties
            mplew.writeInt(game.getOmokPoints("losses", true) + 1); // losses
            mplew.writeInt(2000); // points
            mplew.writeInt(1); // start of visitor; unknown
            mplew.writeInt(game.getOmokPoints("wins", false) + 1); // wins
            mplew.writeInt(game.getOmokPoints("ties", false)); // ties
            mplew.writeInt(game.getOmokPoints("losses", false)); // losses
            mplew.writeInt(2000); // points
            game.setOmokPoints(person + 1);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameTie(MapleMiniGame game) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(HexTool.getByteArrayFromHexString("38 01"));
            mplew.writeInt(1); // unknown
            mplew.writeInt(game.getOmokPoints("wins", true)); // wins
            mplew.writeInt(game.getOmokPoints("ties", true) + 1); // ties
            mplew.writeInt(game.getOmokPoints("losses", true)); // losses
            mplew.writeInt(2000); // points
            mplew.writeInt(1); // start of visitor; unknown
            mplew.writeInt(game.getOmokPoints("wins", false)); // wins
            mplew.writeInt(game.getOmokPoints("ties", false) + 1); // ties
            mplew.writeInt(game.getOmokPoints("losses", false)); // losses
            mplew.writeInt(2000); // points
            game.setMatchCardPoints(3);
            return mplew.getPacket();
        }

        public static MaplePacket getMiniGameForfeit(MapleMiniGame game, int person) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(HexTool.getByteArrayFromHexString("38 02"));
            mplew.write(person);
            mplew.writeInt(1); // start of owner; unknown
            mplew.writeInt(game.getOmokPoints("wins", true)); // wins
            mplew.writeInt(game.getOmokPoints("ties", true)); // ties
            mplew.writeInt(game.getOmokPoints("losses", true) + 1); // losses
            mplew.writeInt(2000); // points
            mplew.writeInt(1); // start of visitor; unknown
            mplew.writeInt(game.getOmokPoints("wins", false) + 1); // wins
            mplew.writeInt(game.getOmokPoints("ties", false)); // ties
            mplew.writeInt(game.getOmokPoints("losses", false)); // losses
            mplew.writeInt(2000); // points
            game.setOmokPoints(person + 1);
            return mplew.getPacket();
        }

        public static MaplePacket getMatchCardStart(MapleMiniGame game) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x23);
            mplew.write(game.getLoser() == 1 ? 0 : 1);
            int times;
            if (game.getMatchesToWin() > 10) {
                times = 30;
            } else if (game.getMatchesToWin() > 6) {
                times = 20;
            } else {
                times = 12;
            }
            mplew.write(times);
            for (int i = 1; i <= times; i++) {
                mplew.writeInt(game.getCardId(i));
            }
            return mplew.getPacket();
        }

        public static MaplePacket getMatchCardSelect(int turn, int slot, int firstslot, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(0x3E);
            mplew.write(turn);
            if (turn == 1) {
                mplew.write(slot);
            } else if (turn == 0) {
                mplew.write(slot);
                mplew.write(firstslot);
                mplew.write(type);
            }
            return mplew.getPacket();
        }
        
        // Maple Events
        
        public static MaplePacket leftKnockBack() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x9D); // LEFT_KNOCK_BACK
            return mplew.getPacket();
        }
        
        public static MaplePacket rollSnowball(int type, int rollDistance0, int rollDistance1, int startPoint0, int startPoint1) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x9A); // ROLL_SNOWBALL
            mplew.write(type); // 0 = normal, 1 = rolls from start to end, 2 = btm invis, 3 = top invis
            if (type == 2) {
                MapleSnowball.isInvis0 = true;
            } else if (type == 3) {
                MapleSnowball.isInvis1 = true;
            }
            mplew.write(rollDistance0); // bottom snowball
            mplew.write(startPoint0); // stage
            mplew.write(0);
            mplew.write(rollDistance1); // top snowball
            mplew.write(startPoint1); // stage
            mplew.writeLong(0);
            return mplew.getPacket(); 
        }

        public static MaplePacket hitSnowBall(int team, int damage) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x9B); // HIT_SNOWBALL
            mplew.write(team);// 0 is down, 1 is up
            mplew.writeInt(damage); // fixedDamage = 10
            return mplew.getPacket();
        }

        public static MaplePacket snowballMessage(int team, int message) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x9C); // SNOWBALL_MESSAGE
            mplew.write(team);// 0 is down, 1 is up
            mplew.writeInt(message);
            return mplew.getPacket();
        }
        
        public static MaplePacket showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x34); // OX_QUIZ
            mplew.write(askQuestion ? 1 : 0);
            mplew.write(questionSet);
            mplew.writeInt(questionId);
            return mplew.getPacket();
        }
        
        public static MaplePacket getEventInstructions() { // same as v117.2
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x35); // SHOW_EVENT_INSTRUCTIONS
            mplew.write(0);
            return mplew.getPacket();
        }
        
        public static MaplePacket showForcedEquip(byte team) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x2C); // FORCED_MAP_EQUIP
            if (team > -1) {
                mplew.writeShort(team); // 0x00 = red, 0x01 = blue
            }
            return mplew.getPacket();
        }
        
        public static MaplePacket coconutScore(int team1, int team2) {
           MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
           mplew.write(0x9D); // COCONUT_SCORE
           mplew.writeShort(team1);
           mplew.writeShort(team2);
           return mplew.getPacket();
       }

       public static MaplePacket hitCoconut(boolean spawn, int id, int type) {
           MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
           mplew.write(0x9C); // HIT_COCONUT
           if (spawn) {
                mplew.write(HexTool.getByteArrayFromHexString("00 80 00 00 00"));
            } else {
                mplew.writeInt(id);
                mplew.write(type); // What action to do for the coconut.
            }
            return mplew.getPacket();
        }
        
        // TODO: Fix inv full packet and remove mob hp packet
        
	public static MaplePacket getShowInventoryFull() { // TODO: fix if inventory is full, we haven't tested this yet.
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.write(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
		mplew.write(0);
		mplew.write(0xFF);
		mplew.writeLong(0);
		return mplew.getPacket();
	}

	/**
	 * 
	 * @param oid
	 * @param remhp in %
	 * @return
	 */
	public static MaplePacket showMonsterHP(int oid, int remhppercentage) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue()); // not in v40b. TODO: remove (;
		mplew.writeInt(oid);
		mplew.write(remhppercentage);
		return mplew.getPacket();
	}
}
