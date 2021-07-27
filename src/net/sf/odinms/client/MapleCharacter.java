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

package net.sf.odinms.client;

import java.awt.Point;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.sf.odinms.client.anticheat.CheatTracker;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.database.DatabaseException;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.PacketProcessor;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleMessenger;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.PlayerBuffValueHolder;
import net.sf.odinms.net.world.PlayerCoolDownValueHolder;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.scripting.event.EventInstanceManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePlayerShop;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.MapleShop;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.MapleStorage;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.PlayerInteraction.IPlayerInteractionManager;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.AbstractAnimatedMapleMapObject;
import net.sf.odinms.server.maps.MapleDoor;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapFactory;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.server.maps.SavedLocationType;
import net.sf.odinms.server.quest.MapleCustomQuest;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapleCharacter extends AbstractAnimatedMapleMapObject implements InventoryContainer {
	private static Logger log = LoggerFactory.getLogger(PacketProcessor.class);
	public static final double MAX_VIEW_RANGE_SQ = 850 * 850;
	
	private int world;
	private int accountid;
	private int nxcredit, maplepoints;
	private int rank;
	private int rankMove;
	private int jobRank;
	private int jobRankMove;
	
	private String name;
	private int level;
	private int str, dex, luk, int_;
	private AtomicInteger exp = new AtomicInteger();
	private int hp, maxhp;
	private int mp, maxmp;
	private int mpApUsed, hpApUsed;
	private int hair, face;
	private AtomicInteger meso = new AtomicInteger();
	private int remainingAp, remainingSp;
	
	private int savedLocations[];

	private int fame;
	private long lastfametime;
	private List<Integer> lastmonthfameids;

	// local stats represent current stats of the player to avoid expensive operations
	private transient int localmaxhp, localmaxmp;
	private transient int localstr, localdex, localluk, localint_;
	private transient int magic, watk;
	private transient double speedMod, jumpMod;
	private transient int localmaxbasedamage;

	private int id;
	private MapleClient client;
	private MapleMap map;
	private int initialSpawnPoint;
	// mapid is only used when calling getMapId() with map == null, it is not updated when running in channelserver mode
	private int mapid;
	private MapleShop shop = null;
	private MaplePlayerShop playerShop = null;
        private IPlayerInteractionManager interaction = null;
	private MapleStorage storage = null;
	private MaplePet[] pets = new MaplePet[3];
        private ScheduledFuture<?> fullnessSchedule;
        private ScheduledFuture<?> fullnessSchedule_1;
        private ScheduledFuture<?> fullnessSchedule_2;
	private MapleTrade trade = null;

	private MapleSkinColor skinColor = MapleSkinColor.NORMAL;
	private MapleJob job = MapleJob.BEGINNER;
	private int gender;
        private int gmLevel;
	private boolean hidden;
        private boolean megaHidden;
	private boolean canDoor = true;
	
	private int chair;
	private int itemEffect;
	
	private MapleParty party;
	private EventInstanceManager eventInstance = null;

	private MapleInventory[] inventory;
	private Map<MapleQuest, MapleQuestStatus> quests;
	private Set<MapleMonster> controlled = new LinkedHashSet<MapleMonster>();
	private Set<MapleMapObject> visibleMapObjects = new LinkedHashSet<MapleMapObject>();
	private Map<ISkill, SkillEntry> skills = new LinkedHashMap<ISkill, SkillEntry>();
	private Map<MapleBuffStat, MapleBuffStatValueHolder> effects = new LinkedHashMap<MapleBuffStat, MapleBuffStatValueHolder>();
        private Map<Integer, MapleCoolDownValueHolder> coolDowns = new LinkedHashMap<Integer, MapleCoolDownValueHolder>();    // anticheat related information
	private List<MapleDoor> doors = new ArrayList<MapleDoor>();
	private Map<Integer, MapleSummon> summons = new LinkedHashMap<Integer, MapleSummon>();
	private BuddyList buddylist;
	
	// anticheat related information
	private CheatTracker anticheat;
	private ScheduledFuture<?> dragonBloodSchedule;
        
        // Messenger related information
        private MapleMessenger messenger = null;
        int messengerposition = 4;
        
        // Maple Coconut - Teams for forcing equips
        private byte team; 
	
	private MapleCharacter() {
		setStance(0);
		inventory = new MapleInventory[MapleInventoryType.values().length];
		for (MapleInventoryType type : MapleInventoryType.values()) {
			inventory[type.ordinal()] = new MapleInventory(type, (byte) 100);
		}
		
		savedLocations = new int[SavedLocationType.values().length];
		for (int i = 0; i < SavedLocationType.values().length; i++) {
			savedLocations[i] = -1;
		}
		
		quests = new LinkedHashMap<MapleQuest, MapleQuestStatus>();
		anticheat = new CheatTracker(this);
		setPosition(new Point(0, 0));
	}

	public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) throws SQLException {
		MapleCharacter ret = new MapleCharacter();
		ret.client = client;
		ret.id = charid;

		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
		ps.setInt(1, charid);
		ResultSet rs = ps.executeQuery();
		if (!rs.next()) {
                    throw new RuntimeException("Loading char failed (char not found)");
		}
		ret.name = rs.getString("name");
		ret.level = rs.getInt("level");
		ret.fame = rs.getInt("fame");
		ret.str = rs.getInt("str");
		ret.dex = rs.getInt("dex");
		ret.int_ = rs.getInt("int");
		ret.luk = rs.getInt("luk");
		ret.exp.set(rs.getInt("exp"));
		ret.hp = rs.getInt("hp") > 30000 ? 30000 : rs.getInt("hp");
		ret.maxhp = rs.getInt("maxhp") > 30000 ? 30000 : rs.getInt("maxhp");
		ret.mp = rs.getInt("mp") > 30000 ? 30000 : rs.getInt("mp");
		ret.maxmp = rs.getInt("maxmp") > 30000 ? 30000 : rs.getInt("maxmp");

		ret.remainingSp = rs.getInt("sp");
		ret.remainingAp = rs.getInt("ap");

		ret.meso.set(rs.getInt("meso"));

		ret.gmLevel = rs.getInt("gm");

		ret.skinColor = MapleSkinColor.getById(rs.getInt("skincolor"));
		ret.gender = rs.getInt("gender");
		ret.job = MapleJob.getById(rs.getInt("job"));

		ret.hair = rs.getInt("hair");
		ret.face = rs.getInt("face");

		ret.accountid = rs.getInt("accountid");
                
		ret.mapid = rs.getInt("map");
		ret.initialSpawnPoint = rs.getInt("spawnpoint");
		ret.world = rs.getInt("world");
		
                ret.rank = rs.getInt("rank");
                ret.rankMove = rs.getInt("rankMove");
                ret.jobRank = rs.getInt("jobRank");
                ret.jobRankMove = rs.getInt("jobRankMove");
        
		int buddyCapacity = rs.getInt("buddyCapacity");
		ret.buddylist = new BuddyList(buddyCapacity);
		if (channelserver) {
                    MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
                    ret.map = mapFactory.getMap(ret.mapid);
                    if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                        ret.map = mapFactory.getMap(100000000);
                    }
                    MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                    if (portal == null) {
                        portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                        ret.initialSpawnPoint = 0;
                    }
                    ret.setPosition(portal.getPosition());

                    int partyid = rs.getInt("party");
                    if (partyid >= 0) {
                        try {
                            MapleParty party = client.getChannelServer().getWorldInterface().getParty(partyid);
                            if (party != null && party.getMemberById(ret.id) != null) {
                                    ret.party = party;
                            }
                        } catch (RemoteException e) {
                            client.getChannelServer().reconnectWorld();
                        }
                    }
                    
                    int messengerid = rs.getInt("messengerid");
                    int position = rs.getInt("messengerposition");
                    if (messengerid > 0 && position < 4 && position > -1) {
                        try {
                            WorldChannelInterface wci = ChannelServer.getInstance(client.getChannel()).getWorldInterface();
                            MapleMessenger messenger = wci.getMessenger(messengerid);
                            if (messenger != null) {
                                ret.messenger = messenger;
                                ret.messengerposition = position;
                            }
                        } catch (RemoteException e) {
                            client.getChannelServer().reconnectWorld();
                        }
                    }
		}
		rs.close();
		ps.close();

		String sql = "SELECT * FROM inventoryitems " + "LEFT JOIN inventoryequipment USING (inventoryitemid) WHERE characterid = ?";
		if (!channelserver)
                    sql += " AND inventorytype = " + MapleInventoryType.EQUIPPED.getType();
		ps = con.prepareStatement(sql);
		ps.setInt(1, charid);
		rs = ps.executeQuery();
		while (rs.next()) {
                    MapleInventoryType type = MapleInventoryType.getByType((byte) rs.getInt("inventorytype"));
                    int itemid = rs.getInt("itemid");
                    byte pos = (byte)rs.getInt("position");
                    if (type.equals(MapleInventoryType.EQUIP) || type.equals(MapleInventoryType.EQUIPPED)) {
                        Equip equip = new Equip(itemid, pos, rs.getInt("ringid"));
                        equip.setOwner(rs.getString("owner"));
                        equip.setQuantity((short) rs.getInt("quantity"));
                        equip.setAcc((short) rs.getInt("acc"));
                        equip.setAvoid((short) rs.getInt("avoid"));
                        equip.setDex((short) rs.getInt("dex"));
                        equip.setHands((short) rs.getInt("hands"));
                        equip.setHp((short) rs.getInt("hp"));
                        equip.setInt((short) rs.getInt("int"));
                        equip.setJump((short) rs.getInt("jump"));
                        equip.setLuk((short) rs.getInt("luk"));
                        equip.setMatk((short) rs.getInt("matk"));
                        equip.setMdef((short) rs.getInt("mdef"));
                        equip.setMp((short) rs.getInt("mp"));
                        equip.setSpeed((short) rs.getInt("speed"));
                        equip.setStr((short) rs.getInt("str"));
                        equip.setWatk((short) rs.getInt("watk"));
                        equip.setWdef((short) rs.getInt("wdef"));
                        equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                        equip.setLevel((byte) rs.getInt("level"));
                        ret.getInventory(type).addFromDB(equip);
                    } else {
                        Item item = new Item(rs.getInt("itemid"), (byte) rs.getInt("position"), (short) rs.getInt("quantity"), rs.getInt("petid"));
                        item.setOwner(rs.getString("owner"));
                        ret.getInventory(type).addFromDB(item);
                    }
		}
		rs.close();
		ps.close();

		if (channelserver) {
			ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
			ps.setInt(1, charid);
			rs = ps.executeQuery();
			PreparedStatement pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");
			while (rs.next()) {
				MapleQuest q = MapleQuest.getInstance(rs.getInt("quest"));
				MapleQuestStatus status = new MapleQuestStatus(q, MapleQuestStatus.Status.getById(rs.getInt("status")));
				long cTime = rs.getLong("time");
				if (cTime > -1)
					status.setCompletionTime(cTime * 1000);
				status.setForfeited(rs.getInt("forfeited"));
				ret.quests.put(q, status);
				pse.setInt(1, rs.getInt("queststatusid"));
				ResultSet rsMobs = pse.executeQuery();
				while (rsMobs.next()) {
					status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
				}
				rsMobs.close();
			}
			rs.close();
			ps.close();
			pse.close();
                        
                        ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                        ps.setInt(1, ret.accountid);
                        rs = ps.executeQuery();
                        if (rs.next()) {
                            ret.getClient().setAccountName(rs.getString("name"));
                            ret.nxcredit = rs.getInt("nxcash");
                            ret.maplepoints = rs.getInt("mpoints");
                            if (rs.getInt("banned") > 0) {
                                rs.close();
                                ps.close();
                                ret.getClient().getSession().close();
                                throw new RuntimeException("Loading a banned character");
                            }
                            rs.close();
                            ps.close();
                        } else {
                            rs.close();
                        }
                        ps.close();

			ps = con.prepareStatement("SELECT skillid,skilllevel FROM skills WHERE characterid = ?");
			ps.setInt(1, charid);
			rs = ps.executeQuery();
			while (rs.next()) {
                            ret.skills.put(SkillFactory.getSkill(rs.getInt("skillid")), new SkillEntry(rs.getInt("skilllevel"), 0));
			}
			rs.close();
			ps.close();
			
			ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
			ps.setInt(1, charid);
			rs = ps.executeQuery();
			while (rs.next()) {
				String locationType = rs.getString("locationtype");
				int mapid = rs.getInt("map");
				ret.savedLocations[SavedLocationType.valueOf(locationType).ordinal()] = mapid;
			}
			rs.close();
			ps.close();
			
			ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
			ps.setInt(1, charid);
			rs = ps.executeQuery();
			ret.lastfametime = 0;
			ret.lastmonthfameids = new ArrayList<Integer>(31);
			while (rs.next()) {
				ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
				ret.lastmonthfameids.add(Integer.valueOf(rs.getInt("characterid_to")));
			}
			rs.close();
			ps.close();
			
			ret.buddylist.loadFromDb(charid);
			ret.storage = MapleStorage.loadOrCreateFromDB(ret.accountid);
		}

		ret.recalcLocalStats();
		ret.silentEnforceMaxHpMp();
		return ret;
	}
        
	public static MapleCharacter getDefault (MapleClient client, int chrid) {
            MapleCharacter ret = getDefault(client);
            ret.id = chrid;
            return ret;
	}

	public static MapleCharacter getDefault(MapleClient client) {
		MapleCharacter ret = new MapleCharacter();
		ret.client = client;
		ret.hp = 50;
		ret.maxhp = 50;
		ret.mp = 50;
		ret.maxmp = 50;
		ret.map = null;
		// ret.map = ChannelServer.getInstance(client.getChannel()).getMapFactory().getMap(0);
		ret.exp.set(0);
		ret.gmLevel = 0;
		ret.job = MapleJob.BEGINNER;
		ret.meso.set(0);
		ret.level = 1;
		ret.accountid = client.getAccID();
		ret.buddylist = new BuddyList(25);
                
		ret.recalcLocalStats();
		return ret;
	}
        
        public static int getIdByName(String name, int world) {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            try {
                ps = con.prepareStatement("SELECT id FROM characters WHERE name = ? AND world = ?");
                ps.setString(1, name);
                ps.setInt(2, world);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    ps.close();
                    return -1;
                }
                int id = rs.getInt("id");
                ps.close();
                return id;
            } catch (SQLException e) {
                log.error("ERROR", e);
            }
            return -1;
        }

        public static String getNameById(int id, int world) {
            Connection con = DatabaseConnection.getConnection();
            try {
                PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ? AND world = ?");
                ps.setInt(1, id);
                ps.setInt(2, world);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    ps.close();
                    return null;
                }
                String name = rs.getString("name");
                ps.close();
                return name;
            } catch (SQLException e) {
                log.error("ERROR", e);
            }
            return null;
        }
        
        public void modifyCSPoints(int type, int quantity) {
            modifyCSPoints(type, quantity, false);
        }

        public void modifyCSPoints(int type, int quantity, boolean show) {

            switch (type) {
                case 1:
                    if (nxcredit + quantity < 0) {
                        if (show) {
                            dropMessage(6, "You have gained the max cash. No cash will be awarded.");
                        }
                        return;
                    }
                    nxcredit += quantity;
                    break;
                case 2:
                    if (maplepoints + quantity < 0) {
                        if (show) {
                            dropMessage(6, "You have gained the max maple points. No cash will be awarded.");
                        }
                        return;
                    }
                    maplepoints += quantity;
                    break;
                default:
                    break;
            }
            if (show && quantity != 0) {
                dropMessage(-1, "You have " + (quantity > 0 ? "gained " : "lost ") + quantity + (type == 1 ? " cash." : " maple points."));
                //client.getSession().write(EffectPacket.showForeignEffect(20));
            }
        }
        
        public int getCSPoints(int type) {
            switch (type) {
                case 1:
                    return nxcredit;
                case 2:
                    return maplepoints;
                default:
                    return 0;
            }
        }

	public void saveToDB(boolean update) {
		Connection con = DatabaseConnection.getConnection();
		try {
			// clients should not be able to log back before their old state is saved (see MapleClient#getLoginState) so we are save to switch to a very low isolation level here
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			// connections are thread local now, no need to
			// synchronize anymore =)
			con.setAutoCommit(false);
			PreparedStatement ps;
			if (update) {
				ps = con.prepareStatement("UPDATE characters "
					+ "SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, "
					+ "exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, "
					+ "gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, "
					+ "meso = ?, hpApUsed = ?, mpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ? WHERE id = ?");
			} else {
				ps = con
					.prepareStatement("INSERT INTO characters ("
						+ "level, fame, str, dex, luk, `int`, exp, hp, mp, "
						+ "maxhp, maxmp, sp, ap, gm, skincolor, gender, job, hair, face, map, meso, hpApUsed, mpApUsed, spawnpoint, party, buddyCapacity, accountid, name, world"
						+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			}

			ps.setInt(1, level);
			ps.setInt(2, fame);
			ps.setInt(3, str);
			ps.setInt(4, dex);
			ps.setInt(5, luk);
			ps.setInt(6, int_);
			ps.setInt(7, exp.get());
			ps.setInt(8, hp);
			ps.setInt(9, mp);
			ps.setInt(10, maxhp);
			ps.setInt(11, maxmp);
			ps.setInt(12, remainingSp);
			ps.setInt(13, remainingAp);
			ps.setInt(14, gmLevel);
			ps.setInt(15, skinColor.getId());
			ps.setInt(16, gender);
			ps.setInt(17, job.getId());
			ps.setInt(18, hair);
			ps.setInt(19, face);
			if (map == null) {
				ps.setInt(20, 0);
			} else {
				ps.setInt(20, map.getId());
			}
			ps.setInt(21, meso.get());
			ps.setInt(22, hpApUsed);
			ps.setInt(23, mpApUsed);
			if (map == null) {
				ps.setInt(24, 0);
			} else {
				MaplePortal closest = map.findClosestSpawnpoint(getPosition());
				if (closest != null) {
					ps.setInt(24, closest.getId());
				} else {
					ps.setInt(24, 0);
				}
			}
			if (party != null) {
				ps.setInt(25, party.getId());
			} else {
				ps.setInt(25, -1);
			}
			ps.setInt(26, buddylist.getCapacity());
			
			if (update) {
				ps.setInt(27, id);
			} else {
				ps.setInt(27, accountid);
				ps.setString(28, name);
				ps.setInt(29, world); // TODO store world somewhere ;)
			}
			int updateRows = ps.executeUpdate();
			if (!update) {
				ResultSet rs = ps.getGeneratedKeys();
				if (rs.next()) {
					this.id = rs.getInt(1);
				} else {
					throw new DatabaseException("Inserting char failed.");
				}
			} else if (updateRows < 1) {
				throw new DatabaseException("Character not in database (" + id + ")");
			}
			ps.close();
                        
                        for (int i = 0; i < 3; i++) {
                            if (pets[i] != null) {
                                pets[i].saveToDb();
                            } else {
                                break;
                            }
                        }
                        
			ps = con.prepareStatement("DELETE FROM inventoryitems WHERE characterid = ?");
			ps.setInt(1, id);
			ps.executeUpdate();
			ps.close();
			ps = con.prepareStatement("INSERT INTO inventoryitems"
				+ "(characterid, itemid, inventorytype, position, quantity, owner, petid) " + "VALUES (?, ?, ?, ?, ?, ?, ?)");
			PreparedStatement pse = con.prepareStatement("INSERT INTO inventoryequipment "
				+ "VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			for (MapleInventory iv : inventory) {
                            ps.setInt(3, iv.getType().getType());
                            for (IItem item : iv.list()) {
                                ps.setInt(1, id);
                                ps.setInt(2, item.getItemId());
                                ps.setInt(4, item.getPosition());
                                ps.setInt(5, item.getQuantity());
                                ps.setString(6, item.getOwner());
                                ps.setInt(7, item.getPetId());
                                ps.executeUpdate();
                                ResultSet rs = ps.getGeneratedKeys();
                                int itemid;
                                if (rs.next()) {
                                    itemid = rs.getInt(1);
                                } else {
                                    throw new DatabaseException("Inserting char failed.");
                                }

                                if (iv.getType().equals(MapleInventoryType.EQUIP) || iv.getType().equals(MapleInventoryType.EQUIPPED)) {
                                    pse.setInt(1, itemid);
                                    IEquip equip = (IEquip) item;
                                    pse.setInt(2, equip.getUpgradeSlots());
                                    pse.setInt(3, equip.getLevel());
                                    pse.setInt(4, equip.getStr());
                                    pse.setInt(5, equip.getDex());
                                    pse.setInt(6, equip.getInt());
                                    pse.setInt(7, equip.getLuk());
                                    pse.setInt(8, equip.getHp());
                                    pse.setInt(9, equip.getMp());
                                    pse.setInt(10, equip.getWatk());
                                    pse.setInt(11, equip.getMatk());
                                    pse.setInt(12, equip.getWdef());
                                    pse.setInt(13, equip.getMdef());
                                    pse.setInt(14, equip.getAcc());
                                    pse.setInt(15, equip.getAvoid());
                                    pse.setInt(16, equip.getHands());
                                    pse.setInt(17, equip.getSpeed());
                                    pse.setInt(18, equip.getJump());
                                    pse.setInt(19, equip.getRingId());
                                    pse.executeUpdate();
                                }
                            }
                        }
			ps.close();
			pse.close();
			// psl.close();

			deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
			ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`) " +
				" VALUES (DEFAULT, ?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
			pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
			ps.setInt(1, id);
			for (MapleQuestStatus q : quests.values()) {
				ps.setInt(2, q.getQuest().getId());
				ps.setInt(3, q.getStatus().getId());
				ps.setInt(4, (int) (q.getCompletionTime() / 1000));
				ps.setInt(5, q.getForfeited());
				ps.executeUpdate();
				ResultSet rs = ps.getGeneratedKeys();
				rs.next();
				for (int mob : q.getMobKills().keySet()) {
					pse.setInt(1, rs.getInt(1));
					pse.setInt(2, mob);
					pse.setInt(3, q.getMobKills(mob));
					pse.executeUpdate();
				}
				rs.close();
			}
			ps.close();
			pse.close();
                        
                        ps = con.prepareStatement("UPDATE accounts SET `nxcash` = ?, `mpoints` = ? WHERE id = ?");
                        ps.setInt(1, nxcredit);
                        ps.setInt(2, maplepoints);
                        ps.setInt(3, client.getAccID());
                        ps.executeUpdate();
                        ps.close();


			deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
			ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel) VALUES (?, ?, ?)");
			ps.setInt(1, id);
			for (Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
				ps.setInt(2, skill.getKey().getId());
				ps.setInt(3, skill.getValue().skillevel);
				ps.executeUpdate();
			}
			ps.close();
			
			deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
			ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
			ps.setInt(1, id);
			for (SavedLocationType savedLocationType : SavedLocationType.values()) {
				if (savedLocations[savedLocationType.ordinal()] != -1) {
					ps.setString(2, savedLocationType.name());
					ps.setInt(3, savedLocations[savedLocationType.ordinal()]);
					ps.executeUpdate();
				}
			}
			ps.close();
			
			deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ? AND pending = 0");
			ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 0)");
			ps.setInt(1, id);
			for (BuddylistEntry entry : buddylist.getBuddies()) {
				if (entry.isVisible()) {
					ps.setInt(2, entry.getCharacterId());
					ps.executeUpdate();
				}
			}
			ps.close();

			if (storage != null) {
				storage.saveToDB();
			}

			con.commit();
		} catch (Exception e) {
			log.error(MapleClient.getLogMessage(this, "[charsave] Error saving character data"), e);
			try {
				con.rollback();
			} catch (SQLException e1) {
				log.error(MapleClient.getLogMessage(this, "[charsave] Error Rolling Back"), e);
			}
		} finally {
			try {
				con.setAutoCommit(true);
				con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			} catch (SQLException e) {
				log.error(MapleClient.getLogMessage(this, "[charsave] Error going back to autocommit mode"), e);
			}
		}
	}

	private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
		 PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, id);
		ps.executeUpdate();
		ps.close();
	}
        
        public List<PlayerBuffValueHolder> getAllBuffs() {
            List<PlayerBuffValueHolder> ret = new ArrayList<PlayerBuffValueHolder>();
            for (MapleBuffStatValueHolder mbsvh : effects.values()) {
                ret.add(new PlayerBuffValueHolder(mbsvh.startTime, mbsvh.effect));
            }
            return ret;
        }

	public MapleQuestStatus getQuest(MapleQuest quest) {
		if (!quests.containsKey(quest))
			return new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
		return quests.get(quest);
	}

	public void updateQuest(MapleQuestStatus quest) {
		quests.put(quest.getQuest(), quest);
		if (!(quest.getQuest() instanceof MapleCustomQuest)) {
			if (quest.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
				client.getSession().write(MaplePacketCreator.startQuest(this, (short) quest.getQuest().getId()));
				client.getSession().write(MaplePacketCreator.updateQuestInfo(this, (short) quest.getQuest().getId(), quest.getNpc(), (byte) 6));
			} else if (quest.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {
				client.getSession().write(MaplePacketCreator.completeQuest(this, (short) quest.getQuest().getId()));
			} else if (quest.getStatus().equals(MapleQuestStatus.Status.NOT_STARTED)) {
				client.getSession().write(MaplePacketCreator.forfeitQuest(this, (short) quest.getQuest().getId()));
			}
		}
	}
        
        public void maxAllSkills() {//338 total including gm! 326 without gm
            int[] skill = {1000000, 1000001, 1000002, 1001003, 1001004, 1001005, 2000000, 2000001,
                2001002, 2001003, 2001004, 2001005, 3000000, 3000001, 3000002, 3001003, 3001004, 3001005, 4000000, 4000001, 4001002, 4001003,
                4001334, 4001344, 1100000, 1100001, 1100002, 1100003, 1101004, 1101005, 1101006, 1101007, 1200000, 1200001, 1200002, 1200003,
                1201004, 1201005, 1201006, 1201007, 1300000, 1300001, 1300002, 1300003, 1301004, 1301005, 1301006, 1301007, 2100000, 2101001,
                2101002, 2101003, 2101004, 2101005, 2200000, 2201001, 2201002, 2201003, 2201004, 2201005, 2300000, 2301001, 2301002, 2301003,
                2301004, 2301005, 3100000, 3100001, 3101002, 3101003, 3101004, 3101005, 3200000, 3200001, 3201002, 3201003, 3201004, 3201005,
                4100000, 4100001, 4100002, 4101003, 4101004, 4101005, 4200000, 4200001, 4201002, 4201003, 4201004, 4201005, 1110000, 1110001,
                1111002, 1111003, 1111004, 1111005, 1111006, 1111007, 1111008, 1210000, 1210001, 1211002, 1211003, 1211004, 1211005, 1211006,
                1211007, 1211008, 1211009, 1310000, 1311001, 1311002, 1311003, 1311004, 1311005, 1311006, 1311007, 1311008, 2110000, 2110001,
                2111002, 2111003, 2111004, 2111005, 2111006, 2210000, 2210001, 2211002, 2211003, 2211004, 2211005, 2211006, 2310000, 2311001,
                2311002, 2311003, 2311004, 2311005, 2311006, 3110000, 3110001, 3111002, 3111003, 3111004, 3111005, 3111006, 3210000, 3210001,
                3211002, 3211003, 3211004, 3211005, 3211006, 4110000, 4111001, 4111002, 4111003, 4111004, 4111005, 4111006, 4210000, 4211001,
                4211002, 4211003, 4211004, 4211005, 4211006, 5000000, 5001001, 5001002, 5001003, 5001005};
            for (int a : skill) {
                maxSkillLevel(a);
            }
        }
        
         public void maxSkillLevel(int skillid) {
            int maxlevel = SkillFactory.getSkill(skillid).getMaxLevel();
            changeSkillLevel(SkillFactory.getSkill(skillid), maxlevel, maxlevel);
        }

	public Integer getBuffedValue(MapleBuffStat effect) {
		MapleBuffStatValueHolder mbsvh = effects.get(effect);
		if (mbsvh == null) {
			return null;
		}
		return Integer.valueOf(mbsvh.value);
	}
	
	public boolean isBuffFrom (MapleBuffStat stat, ISkill skill) {
		MapleBuffStatValueHolder mbsvh = effects.get(stat);
		if (mbsvh == null) {
			return false;
		}
		return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
	}
	
	public int getBuffSource (MapleBuffStat stat) {
		MapleBuffStatValueHolder mbsvh = effects.get(stat);
		if (mbsvh == null) {
			return -1;
		}
		return mbsvh.effect.getSourceId();
	}
	
	public void setBuffedValue(MapleBuffStat effect, int value) {
		MapleBuffStatValueHolder mbsvh = effects.get(effect);
		if (mbsvh == null) {
			return;
		}
		mbsvh.value = value;
	}

	public Long getBuffedStarttime(MapleBuffStat effect) {
		MapleBuffStatValueHolder mbsvh = effects.get(effect);
		if (mbsvh == null) {
			return null;
		}
		return Long.valueOf(mbsvh.startTime);
	}

	public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
		MapleBuffStatValueHolder mbsvh = effects.get(effect);
		if (mbsvh == null) {
			return null;
		}
		return mbsvh.effect;
	}
	
	private void prepareDragonBlood(final MapleStatEffect bloodEffect) {
		if (dragonBloodSchedule != null) {
			dragonBloodSchedule.cancel(false);
		}
		dragonBloodSchedule = TimerManager.getInstance().register(new Runnable() {
                    @Override
                    public void run() {
                        addHP(-bloodEffect.getX());
                        getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), bloodEffect.getSourceId(), 5), false);
                    }
		}, 4000, 4000);
	}
        
        public void startFullnessSchedule(final int decrease, final MaplePet pet, int petSlot) {
            ScheduledFuture<?> schedule = TimerManager.getInstance().register(new Runnable() {

                @Override
                public void run() {
                    if (pet != null) {
                        int newFullness = pet.getFullness() - decrease;
                        if (newFullness <= 5) {
                            pet.setFullness(15);
                            unequipPet(pet, true, true);
                        } else {
                            pet.setFullness(newFullness);
                            getClient().getSession().write(MaplePacketCreator.updatePet(pet, true));
                        }
                    }
                }
            }, 60000, 60000);
            switch (petSlot) {
                case 0:
                    fullnessSchedule = schedule;
                case 1:
                    fullnessSchedule_1 = schedule;
                case 2:
                    fullnessSchedule_2 = schedule;
            }
        }

        public void cancelFullnessSchedule(int petSlot) {
            switch (petSlot) {
                case 0:
                    fullnessSchedule.cancel(false);
                case 1:
                    fullnessSchedule_1.cancel(false);
                case 2:
                    fullnessSchedule_2.cancel(false);
            }
        }

	
	public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule) {
		if (effect.isHide()) {
                    this.hidden = true;
                    getMap().broadcastMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
		} else if (effect.isDragonBlood()) {
                    prepareDragonBlood(effect);
		}
		for (Pair<MapleBuffStat, Integer> statup : effect.getStatups()) {
                    effects.put(statup.getLeft(), new MapleBuffStatValueHolder(effect, starttime, schedule, statup.getRight().intValue()));
		}
		recalcLocalStats();
	}

	private List<MapleBuffStat> getBuffStats(MapleStatEffect effect, long startTime) {
		List<MapleBuffStat> stats = new ArrayList<MapleBuffStat>();
		for (Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : effects.entrySet()) {
			MapleBuffStatValueHolder mbsvh = stateffect.getValue();
			if (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime)) {
				stats.add(stateffect.getKey());
			}
		}
		return stats;
	}

	private void deregisterBuffStats(List<MapleBuffStat> stats) {
		List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<MapleBuffStatValueHolder>(stats.size());
		for (MapleBuffStat stat : stats) {
			MapleBuffStatValueHolder mbsvh = effects.get(stat);
			if (mbsvh != null) {
				effects.remove(stat);
				boolean addMbsvh = true;
				for (MapleBuffStatValueHolder contained : effectsToCancel) {
					if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
						addMbsvh = false;
					}
				}
				if (addMbsvh) {
					effectsToCancel.add(mbsvh);
				}
				if (stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.PUPPET) {
					int summonId = mbsvh.effect.getSourceId();
					MapleSummon summon = summons.get(summonId);
					if(summon != null) {
					    getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(this, summonId, true), summon.getPosition());
					    getMap().removeMapObject(summon);
					    removeVisibleMapObject(summon);
					    summons.remove(summonId);
					}
				} else if (stat == MapleBuffStat.DRAGONBLOOD) {
					dragonBloodSchedule.cancel(false);
					dragonBloodSchedule = null;
				}
			}
		}
		for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
			if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).size() == 0) {
				cancelEffectCancelTasks.schedule.cancel(false);
			}
		}
	}

	/**
	 * @param effect
	 * @param overwrite when overwrite is set no data is sent and all the Buffstats in the StatEffect are deregistered
	 * @param startTime
	 */
	public void cancelEffect(MapleStatEffect effect, boolean overwrite, long startTime) {
		List<MapleBuffStat> buffstats;
		if (!overwrite) {
			buffstats = getBuffStats(effect, startTime);
		} else {
			List<Pair<MapleBuffStat, Integer>> statups = effect.getStatups();
			buffstats = new ArrayList<MapleBuffStat>(statups.size());
			for (Pair<MapleBuffStat, Integer> statup : statups) {
				buffstats.add(statup.getLeft());
			}
		}
		deregisterBuffStats(buffstats);
		if (effect.isMagicDoor()) {
			// remove for all on maps
			if (!getDoors().isEmpty()) {
				MapleDoor door = getDoors().iterator().next();
				for (MapleCharacter chr : door.getTarget().getCharacters()) {
					door.sendDestroyData(chr.getClient());
				}
				for (MapleCharacter chr : door.getTown().getCharacters()) {
					door.sendDestroyData(chr.getClient());
				}
				for (MapleDoor destroyDoor : getDoors()) {
					door.getTarget().removeMapObject(destroyDoor);
					door.getTown().removeMapObject(destroyDoor);
				}
				clearDoors();
				silentPartyUpdate();
			}
		}
		
		// check if we are still logged in �.o
		if (!overwrite) {
			cancelPlayerBuffs(buffstats);
			if (effect.isHide() && (MapleCharacter) getMap().getMapObject(getObjectId()) != null) {
				this.hidden = false;
				getMap().broadcastMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);
                                for (int i = 0; i < 3; i++) {
                                if (pets[i] != null) {
                                    getMap().broadcastMessage(this, MaplePacketCreator.showPet(this, pets[i], false, false), false);
                                } else {
                                    break;
                                }
                            }
			}
		}
	}
	
	public void cancelBuffStats(MapleBuffStat ... stat) {
		List<MapleBuffStat> buffStatList = Arrays.asList(stat);
		deregisterBuffStats(buffStatList);
		cancelPlayerBuffs(buffStatList);
	}
	
	public void cancelEffectFromBuffStat(MapleBuffStat stat) {
		cancelEffect(effects.get(stat).effect, false, -1);
	}

	private void cancelPlayerBuffs(List<MapleBuffStat> buffstats) {
		if (getClient().getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) { // are we still connected ?
			recalcLocalStats();
			enforceMaxHpMp();
			getClient().getSession().write(MaplePacketCreator.cancelBuff(buffstats));
			getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignBuff(getId(), buffstats), false);
		}
	}
	
	public void cancelAllBuffs() {
		LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
		for (MapleBuffStatValueHolder mbsvh : allBuffs) {
			cancelEffect(mbsvh.effect, false, mbsvh.startTime);
		}
	}
	
	public void cancelMagicDoor() {
		LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
		for (MapleBuffStatValueHolder mbsvh : allBuffs) {
			if (mbsvh.effect.isMagicDoor()) {
				cancelEffect(mbsvh.effect, false, mbsvh.startTime);
			}
		}		
	}
	
	public void handleOrbgain() {
	    int orbcount = getBuffedValue(MapleBuffStat.COMBO);
		ISkill combo = SkillFactory.getSkill(1111002);

		MapleStatEffect ceffect = null;
		ceffect = combo.getEffect(getSkillLevel(combo));
		
		if (orbcount < ceffect.getX() + 1) {
			int neworbcount = orbcount + 1;
			List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, neworbcount));
			setBuffedValue(MapleBuffStat.COMBO, neworbcount);
			int duration = ceffect.getDuration();
			duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));
			getClient().getSession().write(MaplePacketCreator.giveBuff(1111002, duration, stat));
			getMap().broadcastMessage(this, MaplePacketCreator.giveForeignBuff(getId(), stat), false);
		}
	}
	
	public void handleOrbconsume() {
	    ISkill combo = SkillFactory.getSkill(1111002);
	    MapleStatEffect ceffect = combo.getEffect(getSkillLevel(combo));
	    List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, 1));
	    setBuffedValue(MapleBuffStat.COMBO, 1);
	    int duration = ceffect.getDuration();
	    duration += (int)((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));

	    getClient().getSession().write(MaplePacketCreator.giveBuff(1111002, duration, stat));
	    getMap().broadcastMessage(this, MaplePacketCreator.giveForeignBuff(getId(), stat), false);
	}
	
	private void silentEnforceMaxHpMp() {
		setMp(getMp());
		setHp(getHp(), true);
	}

	private void enforceMaxHpMp() {
		List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>(2);
		if (getMp() > getCurrentMaxMp()) {
			setMp(getMp());
			stats.add(new Pair<MapleStat, Integer>(MapleStat.MP, Integer.valueOf(getMp())));
		}
		if (getHp() > getCurrentMaxHp()) {
			setHp(getHp());
			stats.add(new Pair<MapleStat, Integer>(MapleStat.HP, Integer.valueOf(getHp())));
		}
		if (stats.size() > 0) {
			getClient().getSession().write(MaplePacketCreator.updatePlayerStats(stats));
		}
	}

	public MapleMap getMap() {
		return map;
	}
	
	/**
	 * only for tests
	 * 
	 * @param newmap
	 */
	public void setMap(MapleMap newmap) {
		this.map = newmap;
	}

	public int getMapId() {
		if (map != null) {
			return map.getId();
		}
		return mapid;
	}

	public int getInitialSpawnpoint() {
		return initialSpawnPoint;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
        
        public void setLevel(int level) {
                this.level = level;
        }

	public int getLevel() {
		return level;
	}
        
        public int getRank() {
                return rank;
        }
        
        public int getRankMove() {
                return rankMove;
        }
        
        public int getJobRank() {
            return jobRank;
        }
        
        public int getJobRankMove() {
            return jobRankMove;
        }
        
        public void setFame(int fame) {
                this.fame = fame;
        }
        
	public int getFame() {
		return fame;
	}

	public int getStr() {
		return str;
	}

	public int getDex() {
		return dex;
	}

	public int getLuk() {
		return luk;
	}

	public int getInt() {
		return int_;
	}

	public MapleClient getClient() {
		return client;
	}
        
        public void setExp(int exp) {
            this.exp.set(exp);
        }

	public int getExp() {
		return exp.get();
	}

	public int getHp() {
            if (hp > 30000)
                return 30000;
            return hp;
	}
        
        public void setMaxHp(int newhp) {
                this.maxhp = newhp;
                if (this.maxhp > 30000)
                    this.maxhp = 30000;
        }

	public int getMaxHp() {
            if (maxhp > 30000)
                return 30000;
            return maxhp;
	}

	public int getMp() {
            if (mp > 30000)
                return 30000;
            return mp;
	}

        public void setMaxMp(int newmp) {
                this.maxmp = newmp;
                if (this.maxmp > 30000) 
                    this.maxmp = 30000;
        }
        
	public int getMaxMp() {
            if (maxmp > 30000)
                return 30000;
            return maxmp;
	}

	public int getRemainingAp() {
		return remainingAp;
	}

	public int getRemainingSp() {
		return remainingSp;
	}

	public int getMpApUsed() {
		return mpApUsed;
	}

	public void setMpApUsed(int mpApUsed) {
		this.mpApUsed = mpApUsed;
	}

	public int getHpApUsed() {
		return hpApUsed;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHpApUsed(int hpApUsed) {
		this.hpApUsed = hpApUsed;
	}

	public MapleSkinColor getSkinColor() {
		return skinColor;
	}

	public MapleJob getJob() {
		return job;
	}

	public int getGender() {
		return gender;
	}

	public int getHair() {
		return hair;
	}

	public int getFace() {
		return face;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setStr(int str) {
		this.str = str;
		recalcLocalStats();
	}

	public void setDex(int dex) {
		this.dex = dex;
		recalcLocalStats();
	}

	public void setLuk(int luk) {
		this.luk = luk;
		recalcLocalStats();
	}

	public void setInt(int int_) {
		this.int_ = int_;
		recalcLocalStats();
	}

	public void setHair(int hair) {
		this.hair = hair;
	}

	public void setFace(int face) {
		this.face = face;
	}

	public void setRemainingAp(int remainingAp) {
		this.remainingAp = remainingAp;
	}

	public void setRemainingSp(int remainingSp) {
		this.remainingSp = remainingSp;
	}

	public void setSkinColor(MapleSkinColor skinColor) {
		this.skinColor = skinColor;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}
	
	public CheatTracker getCheatTracker() {
		return anticheat;
	}
	
	public BuddyList getBuddylist() {
		return buddylist;
	}
	
	public void addFame(int famechange) {
		this.fame += famechange;
	}
        
        public int[] getVIPRockMaps() {
            int[] rockmaps = new int[] {999999999, 999999999, 999999999, 999999999, 999999999};
            try {
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT mapid FROM trocklocations WHERE cid = ?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                int i = 0;
                while (rs.next()) {
                    rockmaps[i] = (rs.getInt("mapid"));
                    i++;
                }
                rs.close();
                ps.close();
            } catch (SQLException se) {
                return null;
            }
            return rockmaps;
        }
        
        public void sendNote(String to, String msg) {
            sendNote(to, getName(), msg);
        }
        
        public static void sendNote(String to, String name, String msg) {
            try {
                Connection con = DatabaseConnection.getConnection();
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, to);
                    ps.setString(2, name);
                    ps.setString(3, msg);
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("Unable to send note" + e);
            }
        }
        
        public void showNote() throws SQLException {
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();

            rs.last();
            int count = rs.getRow();
            rs.first();

            client.getSession().write(MaplePacketCreator.showNotes(rs, count));
            ps.close();
        }
        
        public void changeMap(int map, int portal) {
            MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
            changeMap(warpMap, warpMap.getPortal(portal));
        }
	
        public void changeMap(final int to) {
            MapleMap map = ChannelServer.getInstance(getClient().getChannel()).getMapFactory().getMap(to);
            changeMapInternal(map, map.getPortal(0).getPosition(), MaplePacketCreator.getWarpToMap(map, 0, this));
        }
        
	public void changeMap(final MapleMap to, final Point pos) {
		/*getClient().getSession().write(MaplePacketCreator.spawnPortal(map.getId(), to.getId(), pos));
		if (getParty() != null) {
			getClient().getSession().write(MaplePacketCreator.partyPortal(map.getId(), to.getId(), pos));
		}*/
		MaplePacket warpPacket = MaplePacketCreator.getWarpToMap(to, 0x80, this);
		changeMapInternal(to, pos, warpPacket);
	}

	public void changeMap(final MapleMap to, final MaplePortal pto) {
		MaplePacket warpPacket = MaplePacketCreator.getWarpToMap(to, pto.getId(), this);
		changeMapInternal(to, pto.getPosition(), warpPacket);
	}
	
	private void changeMapInternal(final MapleMap to, final Point pos, MaplePacket warpPacket) {
		warpPacket.setOnSend(new Runnable() {
			@Override
			public void run() {
                            IPlayerInteractionManager interaction = MapleCharacter.this.getInteraction();
                            if (interaction != null) {
                                if (interaction.isOwner(MapleCharacter.this)) {
                                    if (interaction.getShopType() == 2) {
                                        interaction.removeAllVisitors(3, 1);
                                        // interaction.closeShop(((MaplePlayerShop) interaction).returnItems(getClient())); //?
                                    } else if (interaction.getShopType() == 3 || interaction.getShopType() == 4) {
                                        interaction.removeAllVisitors(3, 1);
                                    }
                                } else {
                                    interaction.removeVisitor(MapleCharacter.this);
                                }
                            }
                            MapleCharacter.this.setInteraction(null);
                            map.removePlayer(MapleCharacter.this);
                            if (getClient().getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
                                map = to;
                                setPosition(pos);
                                to.addPlayer(MapleCharacter.this);
                                if (party != null) {
                                    silentPartyUpdate();
                                    getClient().getSession().write(MaplePacketCreator.updateParty(getClient().getChannel(), party, PartyOperation.SILENT_UPDATE, null));
                                    updatePartyMemberHP();
                                }
                            }
                        }
		});
		getClient().getSession().write(warpPacket);
	}
	
	public void leaveMap() {
		controlled.clear();
		visibleMapObjects.clear();
		if(chair != 0) {
			chair = 0;
		}
	}

	public void changeJob(MapleJob newJob) {
		this.job = newJob;
		this.remainingSp++;
		updateSingleStat(MapleStat.AVAILABLESP, this.remainingSp);
		updateSingleStat(MapleStat.JOB, newJob.getId());
		getMap().broadcastMessage(this, MaplePacketCreator.showJobChange(getId()), false);
		silentPartyUpdate();
	}
	
	public void gainAp(int ap) {
		this.remainingAp += ap;
		updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
	}

	public void changeSkillLevel(ISkill skill, int newLevel, int newMasterlevel) {
		skills.put(skill, new SkillEntry(newLevel, newMasterlevel));
		this.getClient().getSession().write(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel));
	}

	public void setHp(int newhp) {
		setHp(newhp, false);
	}
	
	private void setHp(int newhp, boolean silent) {
		int oldHp = hp;
		int thp = newhp;
		if (thp < 0) {
			thp = 0;
		}
		if (thp > localmaxhp) {
			thp = localmaxhp;
		}
		this.hp = thp;
		
		if (!silent) {
			updatePartyMemberHP();
		}
		if (oldHp > hp && !isAlive()) {
			playerDead();
		}
	}

	private void playerDead() {
		if (getEventInstance() != null) {
			getEventInstance().playerKilled(this);
		}
		cancelAllBuffs();
		getClient().getSession().write(MaplePacketCreator.enableActions());
	}

	public void updatePartyMemberHP() {
		if (party != null) {
			int channel = client.getChannel();
			for (MaplePartyCharacter partychar : party.getMembers()) {
				if (partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
					MapleCharacter other = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
					if (other != null) {
						other.getClient().getSession().write(
							MaplePacketCreator.updatePartyMemberHP(getId(), this.hp, localmaxhp));
					}
				}
			}
		}
	}

	public void receivePartyMemberHP() {
		if (party != null) {
			int channel = client.getChannel();
			for (MaplePartyCharacter partychar : party.getMembers()) {
				if (partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
					MapleCharacter other = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
					if (other != null) {
						getClient().getSession().write(MaplePacketCreator.updatePartyMemberHP(other.getId(), other.getHp(), other.getCurrentMaxHp()));
					}
				}
			}
		}
	}

	public void setMp(int newmp) {
		int tmp = newmp;
		if (tmp < 0) {
			tmp = 0;
		}
		if (tmp > localmaxmp) {
			tmp = localmaxmp;
		}
		this.mp = tmp;
                if (mp > 30000)
                    this.mp = 30000;
	}

	/**
	 * Convenience function which adds the supplied parameter to the current hp then directly does a updateSingleStat.
	 * 
	 * @see MapleCharacter#setHp(int)
	 * @param delta
	 */
	public void addHP(int delta) {
		setHp(hp + delta);
		updateSingleStat(MapleStat.HP, hp);
	}

	/**
	 * Convenience function which adds the supplied parameter to the current mp then directly does a updateSingleStat.
	 * 
	 * @see MapleCharacter#setMp(int)
	 * @param delta
	 */
	public void addMP(int delta) {
		setMp(mp + delta);
		updateSingleStat(MapleStat.MP, mp);
	}

	/**
	 * Updates a single stat of this MapleCharacter for the client. This method only creates and sends an update packet,
	 * it does not update the stat stored in this MapleCharacter instance.
	 * 
	 * @param stat
	 * @param newval
	 * @param itemReaction
	 */
	public void updateSingleStat(MapleStat stat, int newval, boolean itemReaction) {
		Pair<MapleStat, Integer> statpair = new Pair<MapleStat, Integer>(stat, Integer.valueOf(newval));
		MaplePacket updatePacket = MaplePacketCreator.updatePlayerStats(Collections.singletonList(statpair),
			itemReaction);
		client.getSession().write(updatePacket);
	}

	public void updateSingleStat(MapleStat stat, int newval) {
		updateSingleStat(stat, newval, false);
	}
	
	public void gainExp(int gain, boolean show, boolean inChat, boolean white) {
		if (getLevel() < 200) { // lv200 is max and has 0 exp required to level
			int newexp = this.exp.addAndGet(gain);
			updateSingleStat(MapleStat.EXP, newexp);
		}
		if (show) { // still show the expgain even if it's not there
			client.getSession().write(MaplePacketCreator.getShowExpGain(gain, inChat, white));
		}
		while (level < 200 && exp.get() >= ExpTable.getExpNeededForLevel(level + 1)) {
			levelUp();
		}
	}
	
	public void silentPartyUpdate() {
		if (party != null) {
			try {
				getClient().getChannelServer().getWorldInterface().updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new MaplePartyCharacter(MapleCharacter.this));
			} catch (RemoteException e) {
				log.error("REMOTE THROW", e);
				getClient().getChannelServer().reconnectWorld();
			}
		}
	}

	public void gainExp(int gain, boolean show, boolean inChat) {
		gainExp(gain, show, inChat, true);
	}

	public boolean isGM() {
		return gmLevel > 0;
	}
        
        public void setGMLevel(int level) {
                gmLevel = level;
        }
	
	public int getGMLevel() {
		return gmLevel;
	}

	public boolean hasGmLevel(int level) {
		return gmLevel >= level;
	}
	
	public MapleInventory getInventory(MapleInventoryType type) {
		return inventory[type.ordinal()];
	}

	public MapleShop getShop() {
		return shop;
	}

	public void setShop(MapleShop shop) {
		this.shop = shop;
	}

	public int getMeso() {
		return meso.get();
	}

	public int getSavedLocation(SavedLocationType type) {
		return savedLocations[type.ordinal()];
	}

	public void saveLocation(SavedLocationType type) {
		savedLocations[type.ordinal()] = getMapId();
	}
	
	public void clearSavedLocation(SavedLocationType type) {
		savedLocations[type.ordinal()] = -1;
	}

	public void gainMeso(int gain, boolean show) {
		gainMeso(gain, show, false, false);
	}

	public void gainMeso(int gain, boolean show, boolean enableActions) {
		gainMeso(gain, show, enableActions, false);
	}

	public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
		if (meso.get() + gain < 0) {
                    client.getSession().write(MaplePacketCreator.enableActions());
                    return;
		}
		int newVal = meso.addAndGet(gain);
		updateSingleStat(MapleStat.MESO, newVal, enableActions);
		if (show) {
                    client.getSession().write(MaplePacketCreator.getShowMesoGain(gain, inChat));
                    client.getSession().write(MaplePacketCreator.enableActions());
		}
	}

	/**
	 * Adds this monster to the controlled list. The monster must exist on the Map.
	 * 
	 * @param monster
	 */
	public void controlMonster(MapleMonster monster, boolean aggro) {
		monster.setController(this);
		controlled.add(monster);
		client.getSession().write(MaplePacketCreator.controlMonster(monster, false, aggro));
	}

	public void stopControllingMonster(MapleMonster monster) {
		controlled.remove(monster);
	}

	public Collection<MapleMonster> getControlledMonsters() {
		return Collections.unmodifiableCollection(controlled);
	}

	public int getNumControlledMonsters() {
		return controlled.size();
	}

	@Override
	public String toString() {
		return "Character: " + this.name;
	}

	public int getAccountID() {
		return accountid;
	}

	public void mobKilled(int id) {
		for (MapleQuestStatus q : quests.values()) {
			if (q.getStatus() == MapleQuestStatus.Status.COMPLETED || q.getQuest().canComplete(this, null))
				continue;
			if (q.mobKilled(id) && !(q.getQuest() instanceof MapleCustomQuest)) {
				client.getSession().write(MaplePacketCreator.updateQuestMobKills(q));
				if (q.getQuest().canComplete(this, null)) {
					client.getSession().write(MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
				}
			}
		}
	}

	public final List<MapleQuestStatus> getStartedQuests() {
		List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
		for (MapleQuestStatus q : quests.values()) {
			if (q.getStatus().equals(MapleQuestStatus.Status.STARTED) && !(q.getQuest() instanceof MapleCustomQuest))
				ret.add(q);
		}
		return Collections.unmodifiableList(ret);
	}

	public final List<MapleQuestStatus> getCompletedQuests() {
		List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
		for (MapleQuestStatus q : quests.values()) {
			if (q.getStatus().equals(MapleQuestStatus.Status.COMPLETED) && !(q.getQuest() instanceof MapleCustomQuest))
				ret.add(q);
		}
		return Collections.unmodifiableList(ret);
	}

	public MaplePlayerShop getPlayerShop() {
		return playerShop;
	}

	public void setPlayerShop(MaplePlayerShop playerShop) {
		this.playerShop = playerShop;
	}
        
        public IPlayerInteractionManager getInteraction() {
            return interaction;
        }

        public void setInteraction(IPlayerInteractionManager box) {
            interaction = box;
        }

	public Map<ISkill, SkillEntry> getSkills() {
		return Collections.unmodifiableMap(skills);
	}

	public int getSkillLevel(ISkill skill) {
		SkillEntry ret = skills.get(skill);
		if (ret == null) {
			return 0;
		}
		return ret.skillevel;
	}
	
	public int getMasterLevel(ISkill skill) {
		SkillEntry ret = skills.get(skill);
		if (ret == null) {
			return 0;
		}
		return ret.masterlevel;
	}

	// the equipped inventory only contains equip... I hope
	public int getTotalDex() {
		return localdex;
	}

	public int getTotalInt() {
		return localint_;
	}

	public int getTotalStr() {
		return localstr;
	}

	public int getTotalLuk() {
		return localluk;
	}

	public int getTotalMagic() {
		return magic;
	}
	
	public double getSpeedMod() {
		return speedMod;
	}
	
	public double getJumpMod() {
		return jumpMod;
	}
	
	public int getTotalWatk() {
		return watk;
	}

	private static int rand(int lbound, int ubound) {
		return (int) ((Math.random() * (ubound - lbound + 1)) + lbound);
	}

	public void levelUp() {
		ISkill improvingMaxHP = SkillFactory.getSkill(1000001);
		ISkill improvingMaxMP = SkillFactory.getSkill(2000001);

		int improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
		int improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
		remainingAp += 5;
		if (job == MapleJob.BEGINNER) {
			// info from the odin what's working thread, thanks
			maxhp += rand(14, 16);
			maxmp += rand(10, 12);
		} else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.THIEF) || job.isA(MapleJob.GM)) {
			// info from bowman forum at sleepywood, thanks guys
			maxhp += rand(20, 24);
			maxmp += rand(14, 16);
		} else if (job.isA(MapleJob.MAGICIAN)) {
			// made up
			maxhp += rand(10, 14);
			maxmp += rand(20, 24);
		} else if (job.isA(MapleJob.WARRIOR)) {
			// made up
			maxhp += rand(22, 26);
			maxmp += rand(4, 7);
		}

		if (improvingMaxHPLevel > 0) {
			maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
		}
		if (improvingMaxMPLevel > 0) {
			maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
		}
		maxmp += getTotalInt() / 10;
		exp.addAndGet(-ExpTable.getExpNeededForLevel(level + 1));
		if (level == 200) {
			exp.set(0);
		}

		maxhp = Math.min(30000, maxhp);
		maxmp = Math.min(30000, maxmp);

		level += 1;
		List<Pair<MapleStat, Integer>> statup = new ArrayList<Pair<MapleStat, Integer>>(8);
		statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLEAP, Integer.valueOf(remainingAp)));
		statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, Integer.valueOf(maxhp)));
		statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, Integer.valueOf(maxmp)));
		statup.add(new Pair<MapleStat, Integer>(MapleStat.HP, Integer.valueOf(maxhp)));
		statup.add(new Pair<MapleStat, Integer>(MapleStat.MP, Integer.valueOf(maxmp)));
		statup.add(new Pair<MapleStat, Integer>(MapleStat.EXP, Integer.valueOf(exp.get())));
		statup.add(new Pair<MapleStat, Integer>(MapleStat.LEVEL, Integer.valueOf(level)));

		if (job != MapleJob.BEGINNER) {
			remainingSp += 3;
			statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLESP, Integer.valueOf(remainingSp)));
		}

		setHp(maxhp);
		setMp(maxmp);
		getClient().getSession().write(MaplePacketCreator.updatePlayerStats(statup));
		getMap().broadcastMessage(this, MaplePacketCreator.showLevelup(getId()), false);
		recalcLocalStats();
		silentPartyUpdate();
	}
        
        private boolean incs = false;
        public void setInCS(boolean yesno) {
            this.incs = yesno;
        }

        public boolean inCS() {
            return this.incs;
        }
        
        public MapleMessenger getMessenger() {
            return messenger;
        }

        public void setMessenger(MapleMessenger messenger) {
            this.messenger = messenger;
        }

        public void checkMessenger() {
            if (messenger != null && messengerposition < 4 && messengerposition > -1) {
                try {
                    WorldChannelInterface wci = ChannelServer.getInstance(client.getChannel()).getWorldInterface();
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(client.getPlayer(), messengerposition);
                    wci.silentJoinMessenger(messenger.getId(), messengerplayer, messengerposition);
                    wci.updateMessenger(getClient().getPlayer().getMessenger().getId(), getClient().getPlayer().getName(), getClient().getChannel());
                } catch (RemoteException e) {
                    client.getChannelServer().reconnectWorld();
                }
            }
        }

        public int getMessengerPosition() {
            return messengerposition;
        }

        public void setMessengerPosition(int position) {
            this.messengerposition = position;
        }

	public void tempban(String reason, Calendar duration, int greason) {
		if (lastmonthfameids == null) {
			throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
		}
		tempban(reason, duration, greason, client.getAccID());
		client.getSession().close();
	}
	
	public static boolean tempban(String reason, Calendar duration, int greason, int accountid) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
			Timestamp TS = new Timestamp(duration.getTimeInMillis());
			ps.setTimestamp(1, TS);
			ps.setString(2, reason);
			ps.setInt(3, greason);
			ps.setInt(4, accountid);
			ps.executeUpdate();
			ps.close();
			return true;
		} catch (SQLException ex) {
			log.error("Error while tempbanning", ex);
		}
		return false;
	}

	public void ban(String reason) {
		if (lastmonthfameids == null) {
			throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
		}
		try {
			getClient().banMacs();
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
			ps.setInt(1, 1);
			ps.setString(2, reason);
			ps.setInt(3, accountid);
			ps.executeUpdate();
			ps.close();
			ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
			String[] ipSplit = client.getSession().getRemoteAddress().toString().split(":");
			ps.setString(1, ipSplit[0]);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException ex) {
			log.error("Error while banning", ex);
		}
		client.getSession().close();
	}

	public static boolean ban(String id, String reason, boolean accountId) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps;
			if (id.matches("/[0-9]{1,3}\\..*")) {
				ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
				ps.setString(1, id);
				ps.executeUpdate();
				ps.close();
				return true;
			}
			if (accountId) {
				ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
			} else {
				ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
			}
			boolean ret = false;
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
				psb.setString(1, reason);
				psb.setInt(2, rs.getInt(1));
				psb.executeUpdate();
				psb.close();
				ret = true;
			}
			rs.close();
			ps.close();
			return ret;
		} catch (SQLException ex) {
			log.error("Error while banning", ex);
		}
		return false;
	}

	/**
	 * Oid of players is always = the cid
	 */
	@Override
	public int getObjectId() {
		return getId();
	}

	/**
	 * Throws unsupported operation exception, oid of players is read only
	 */
	@Override
	public void setObjectId(int id) {
		throw new UnsupportedOperationException();
	}

	public MapleStorage getStorage() {
		return storage;
	}

	public int getCurrentMaxHp() {
		return localmaxhp;
	}

	public int getCurrentMaxMp() {
		return localmaxmp;
	}
	
	public int getCurrentMaxBaseDamage() {
		return localmaxbasedamage;
	}
	
	public int calculateMaxBaseDamage (int watk) {
		int maxbasedamage;
		if (watk == 0) {
			maxbasedamage = 1;
		} else {
			IItem weapon_item = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
			if (weapon_item != null) {
				MapleWeaponType weapon = MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId());
				int mainstat;
				int secondarystat;
				if (weapon == MapleWeaponType.BOW || weapon == MapleWeaponType.CROSSBOW) {
					mainstat = localdex;
					secondarystat = localstr;
				} else if (getJob().isA(MapleJob.THIEF) && (weapon == MapleWeaponType.CLAW || weapon == MapleWeaponType.DAGGER)) {
					mainstat = localluk;
					secondarystat = localdex + localstr;
				} else {
					mainstat = localstr;
					secondarystat = localdex;
				}
				maxbasedamage = (int) (((weapon.getMaxDamageMultiplier() * mainstat + secondarystat) / 100.0) * watk);
				 //just some saveguard against rounding errors, we want to a/b for this
				maxbasedamage += 10;
			} else {
				maxbasedamage = 0;
			}
		}
		return maxbasedamage;
	}
        
        public final boolean canHold(final int itemid) {
            return getInventory(ServerConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
        }
        
        public void unequipEverything() {
            MapleInventory equipped = this.getInventory(MapleInventoryType.EQUIPPED);
            List<Byte> position = new ArrayList<Byte>();
            for (IItem item : equipped.list()) {
                position.add((byte)item.getPosition());
                if (!canHold(item.getItemId())) {
                    client.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
            }
            for (byte pos : position) {
                 MapleInventoryManipulator.unequip(client, pos, getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        }
	
	public void addVisibleMapObject (MapleMapObject mo) {
		visibleMapObjects.add(mo);
	}
	
	public void removeVisibleMapObject (MapleMapObject mo) {
		visibleMapObjects.remove(mo);
	}
	
	public boolean isMapObjectVisible (MapleMapObject mo) {
		return visibleMapObjects.contains(mo);
	}
	
	public Collection<MapleMapObject> getVisibleMapObjects () {
		return Collections.unmodifiableCollection(visibleMapObjects);
	}
	
	public boolean isAlive() {
		return this.hp > 0;
	}
	
	@Override
	public void sendDestroyData(MapleClient client) {
            client.getSession().write(MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
	}

	@Override
	public void sendSpawnData(MapleClient client) {
            if (!this.isHidden()) {
                client.getSession().write(MaplePacketCreator.spawnPlayerMapobject(this));
                for (int i = 0; i < 3; i++) {
                    if (pets[i] != null) {
                        client.getSession().write(MaplePacketCreator.showPet(this, pets[i], false, false));
                    } else {
                        break;
                    }
                }
            }
	}
	
	private void recalcLocalStats() {
		int oldmaxhp = localmaxhp;
		localmaxhp = getMaxHp();
		localmaxmp = getMaxMp();
		localdex = getDex();
		localint_ = getInt();
		localstr = getStr();
		localluk = getLuk();
		int speed = 100;
		int jump = 100;
		magic = localint_;
		watk = 0;
		for (IItem item : getInventory(MapleInventoryType.EQUIPPED)) {
                    IEquip equip = (IEquip) item;
                    if (localmaxhp + equip.getHp() > 30000) {
                        localmaxhp = 30000;
                    } else {
                        localmaxhp += equip.getHp();
                    }
                    if (localmaxmp + equip.getMp() > 30000) {
                        localmaxmp = 30000;
                    } else {
                        localmaxmp += equip.getMp();
                    }
                    localdex += equip.getDex();
                    localint_ += equip.getInt();
                    localstr += equip.getStr();
                    localluk += equip.getLuk();
                    magic += equip.getMatk() + equip.getInt();
                    watk += equip.getWatk();
                    speed += equip.getSpeed();
                    jump += equip.getJump();
		}
		magic = Math.min(magic, 2000);
		Integer hbhp = getBuffedValue(MapleBuffStat.HYPERBODYHP);
		if (hbhp != null) {
                    localmaxhp += (hbhp.doubleValue() / 100) * localmaxhp;
		}
		Integer hbmp = getBuffedValue(MapleBuffStat.HYPERBODYMP);
		if (hbmp != null) {
                    localmaxmp += (hbmp.doubleValue() / 100) * localmaxmp;
		}
		localmaxhp = Math.min(30000, localmaxhp);
		localmaxmp = Math.min(30000, localmaxmp);
		Integer watkbuff = getBuffedValue(MapleBuffStat.WATK);
		if (watkbuff != null) {
                    watk += watkbuff.intValue();
		}
		Integer matkbuff = getBuffedValue(MapleBuffStat.MATK);
		if (matkbuff != null) {
                    magic += matkbuff.intValue();
		}
		Integer speedbuff = getBuffedValue(MapleBuffStat.SPEED);
		if (speedbuff != null) {
                    speed += speedbuff.intValue();
		}
		Integer jumpbuff = getBuffedValue(MapleBuffStat.JUMP);
		if (jumpbuff != null) {
                    jump += jumpbuff.intValue();
		}
		if (speed > 140) {
			speed = 140;
		}
		if (jump > 123) {
			jump = 123;
		}
		speedMod = speed / 100.0;
		jumpMod = jump / 100.0;
		localmaxbasedamage = calculateMaxBaseDamage(watk);
		if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
                    updatePartyMemberHP();
		}
	}

	public void equipChanged() {
                saveToDB(true);
		getMap().broadcastMessage(this, MaplePacketCreator.updateCharLook(this), false);
		recalcLocalStats();
		enforceMaxHpMp();
                if (getClient().getPlayer().getMessenger() != null) {
                    WorldChannelInterface wci = ChannelServer.getInstance(getClient().getChannel()).getWorldInterface();
                    try {
                        wci.updateMessenger(getClient().getPlayer().getMessenger().getId(), getClient().getPlayer().getName(), getClient().getChannel());
                    } catch (RemoteException e) {
                        getClient().getChannelServer().reconnectWorld();
                    }
                }
	}
        
        private transient AtomicInteger inst = new AtomicInteger(0);
        public int getConversation() {
            return inst.get();
        }

        public void setConversation(int inst) {
            this.inst.set(inst);
        }
        
        private String[] commandArgs;
    
        public void setCommandArgs(String[] args) {
            commandArgs = args;
        }

        public String[] getCommandArgs(){
            return commandArgs;
        }
        
        public int getItemQuantity(int itemid, boolean checkEquipped) {
            int possesed = inventory[ServerConstants.getInventoryType(itemid).ordinal()].countById(itemid);
            if (checkEquipped) {
                possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
            }
            return possesed;
        }


	public MaplePet getPet(int index) {
            return pets[index];
        }

        public void addPet(MaplePet pet) {
            for (int i = 0; i < 3; i++) {
                if (pets[i] == null) {
                    pets[i] = pet;
                    return;
                }
            }
        }

        public void removePet(MaplePet pet, boolean shift_left) {
            int slot = -1;
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    if (pets[i].getUniqueId() == pet.getUniqueId()) {
                        pets[i] = null;
                        slot = i;
                        break;
                    }
                }
            }
            if (shift_left) {
                if (slot > -1) {
                    for (int i = slot; i < 3; i++) {
                        if (i != 2) {
                            pets[i] = pets[i + 1];
                        } else {
                            pets[i] = null;
                        }
                    }
                }
            }
        }

        public int getNoPets() {
            int ret = 0;
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    ret++;
                } else {
                    break;
                }
            }
            return ret;
        }

        public int getPetIndex(MaplePet pet) {
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    if (pets[i].getUniqueId() == pet.getUniqueId()) {
                        return i;
                    }
                } else {
                    break;
                }
            }
            return -1;
        }

        public int getPetIndex(int petId) {
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    if (pets[i].getUniqueId() == petId) {
                        return i;
                    }
                } else {
                    break;
                }
            }
            return -1;
        }

        public int getNextEmptyPetIndex() {
            for (int i = 0; i < 3; i++) {
                if (pets[i] == null) {
                    return i;
                }
            }
            return 3;
        }

        public MaplePet[] getPets() {
            return pets;
        }

        public void unequipAllPets() {
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    unequipPet(pets[i], true);
                    cancelFullnessSchedule(i);
                } else {
                    break;
                }
            }
        }

        public void unequipPet(MaplePet pet, boolean shift_left) {
            unequipPet(pet, shift_left, false);
        }

        public void unequipPet(MaplePet pet, boolean shift_left, boolean hunger) {
            cancelFullnessSchedule(getPetIndex(pet));
            pet.saveToDb();
            getMap().broadcastMessage(this, MaplePacketCreator.showPet(this, pet, true, hunger), true);
            List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>();
            stats.add(new Pair<MapleStat, Integer>(MapleStat.PET, Integer.valueOf(0)));
            getClient().getSession().write(MaplePacketCreator.petStatUpdate(this));
            getClient().getSession().write(MaplePacketCreator.enableActions());
            removePet(pet, shift_left);
        }

        public void shiftPetsRight() {
            if (pets[2] == null) {
                pets[2] = pets[1];
                pets[1] = pets[0];
                pets[0] = null;
            }
        }

	
	public FameStatus canGiveFame(MapleCharacter from) {
		if (lastfametime >= System.currentTimeMillis() - 60*60*24*1000) {
			return FameStatus.NOT_TODAY;
		} else if (lastmonthfameids.contains(Integer.valueOf(from.getId()))) {
			return FameStatus.NOT_THIS_MONTH;
		} else {
			return FameStatus.OK;
		}
	}
	
	public void hasGivenFame(MapleCharacter to) {
		lastfametime = System.currentTimeMillis();
		lastmonthfameids.add(Integer.valueOf(to.getId()));
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con
				.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
			ps.setInt(1, getId());
			ps.setInt(2, to.getId());
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			log.error("ERROR writing famelog for char " + getName() + " to " + to.getName(), e);
		}
	}
	
	public MapleParty getParty() {
		return party;
	}

	public int getWorld() {
		return world;
	}

	public void setWorld(int world) {
		this.world = world;
	}

	public void setParty(MapleParty party) {
		this.party = party;
	}

	public MapleTrade getTrade() {
		return trade;
	}

	public void setTrade(MapleTrade trade) {
		this.trade = trade;
	}

	public EventInstanceManager getEventInstance() {
		return eventInstance;
	}

	public void setEventInstance(EventInstanceManager eventInstance) {
		this.eventInstance = eventInstance;
	}
        
        public byte getTeam() {
            return team;
        }

        public void setTeam(byte team) {
            this.team = team;
        }
	
	public void addDoor(MapleDoor door) {
		doors.add(door);
	}
	
	public void clearDoors() {
		doors.clear();
	}
	
	public List<MapleDoor> getDoors() {
		return new ArrayList<MapleDoor>(doors);
	}
	
	public boolean canDoor() {
		return canDoor;
	}
	
	public void disableDoor() {
		canDoor = false;
		TimerManager tMan = TimerManager.getInstance();
		tMan.schedule(new Runnable() {
			@Override
			public void run() {
				canDoor = true;
			}
		}, 5000);
	}
	
	public Map<Integer, MapleSummon> getSummons() {
		return summons;
	}
	
	public int getChair() {
		return chair;
	}

	public int getItemEffect() {
		return itemEffect;
	}

	public void setChair(int chair) {
		this.chair = chair;
	}

	public void setItemEffect(int itemEffect) {
		this.itemEffect = itemEffect;
	}
	
	@Override
	public Collection<MapleInventory> allInventories() {
		return Arrays.asList(inventory);
	}
	
	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.PLAYER;
	}
        
        public void dropMessage(String msg) {
            dropMessage(6, msg);
        }
        
        public void dropMessage(int type, String message) {
            MaplePacket packet = MaplePacketCreator.serverNotice(type, message);
            client.getSession().write(packet);
        }

	private NumberFormat nf = new DecimalFormat("#,###,###,###");

	public boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
		MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
		MapleInventory iv = inventory[type.ordinal()];
		int possesed = iv.countById(itemid);
		if (checkEquipped) {
			possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
		}
		if (greaterOrEquals) {
			return possesed >= quantity;
		} else {
			return possesed == quantity;
		}
	}
        
        public void setMegaHide(boolean yn) {
            this.megaHidden = yn;
        }
        
        public boolean isMegaHidden() {
            return this.megaHidden;
        }
        
        public void toggleHide(boolean login, boolean yes) {
            if (isGM()) {
                if (!yes) {
                    this.hidden = false;
                    // dispelSkill(9101004);
                    updatePartyMemberHP();
                    equipChanged();
                    getMap().broadcastMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);
                    dropMessage(6, "Hide Deactivated.");
                } else {
                    this.hidden = true;
                    if (!login) {
                        if (getGMLevel() > 99 && isMegaHidden()) {
                            dropMessage(5, "[Warning] Super Hide is enabled, which means GMs can't see you.");
                            for (MapleCharacter chr : this.getMap().getCharacters()) {
                                if (chr.getGMLevel() < 99) {
                                    chr.getClient().getSession().write(MaplePacketCreator.removePlayerFromMap(getId()));
                                }
                            }
                        } else {
                            getMap().broadcastNONGMMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
                        }
                    }
                    dropMessage(6, "Hide Activated.");
                }
                client.getSession().write(MaplePacketCreator.enableActions());
            }
        }

	private static class MapleBuffStatValueHolder {
		public MapleStatEffect effect;
		public long startTime;
		public int value;
		public ScheduledFuture<?> schedule;

		public MapleBuffStatValueHolder(MapleStatEffect effect, long startTime, ScheduledFuture<?> schedule, int value) {
			super();
			this.effect = effect;
			this.startTime = startTime;
			this.schedule = schedule;
			this.value = value;
		}
	}
        
        public void addCooldown(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
        if (this.coolDowns.containsKey(Integer.valueOf(skillId))) {
            this.coolDowns.remove(skillId);
        }
        this.coolDowns.put(Integer.valueOf(skillId), new MapleCoolDownValueHolder(skillId, startTime, length, timer));
    }

    public void removeCooldown(int skillId) {
        if (this.coolDowns.containsKey(Integer.valueOf(skillId))) {
            this.coolDowns.remove(Integer.valueOf(skillId));
            //client.getSession().write(MaplePacketCreator.skillCooldown(skillId, 0));
        }
    }

    public boolean skillisCooling(int skillId) {
        return this.coolDowns.containsKey(Integer.valueOf(skillId));
    }

    public void giveCoolDowns(final List<PlayerCoolDownValueHolder> cooldowns) {
        for (PlayerCoolDownValueHolder cooldown : cooldowns) {
            int time = (int) ((cooldown.length + cooldown.startTime) - System.currentTimeMillis());
            ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, cooldown.skillId), time);
            addCooldown(cooldown.skillId, System.currentTimeMillis(), time, timer);
        }
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        int time = (int) ((length + starttime) - System.currentTimeMillis());
        ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, skillid), time);
        addCooldown(skillid, System.currentTimeMillis(), time, timer);
    }

    public List<PlayerCoolDownValueHolder> getAllCooldowns() {
        List<PlayerCoolDownValueHolder> ret = new ArrayList<PlayerCoolDownValueHolder>();
        for (MapleCoolDownValueHolder mcdvh : coolDowns.values()) {
            ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
        }
        return ret;
    }

    public static class CancelCooldownAction implements Runnable {

        private int skillId;
        private WeakReference<MapleCharacter> target;

        public CancelCooldownAction(MapleCharacter target, int skillId) {
            this.target = new WeakReference<MapleCharacter>(target);
            this.skillId = skillId;
        }

        @Override
        public void run() {
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.removeCooldown(skillId);
            }
        }
    }
        
        public static class MapleCoolDownValueHolder {

        public int skillId;
        public long startTime;
        public long length;
        public ScheduledFuture<?> timer;

        public MapleCoolDownValueHolder(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
            super();
            this.skillId = skillId;
            this.startTime = startTime;
            this.length = length;
            this.timer = timer;
        }
    }
	
	public static class SkillEntry {
		public int skillevel;
		public int masterlevel;
		
		public SkillEntry(int skillevel, int masterlevel) {
			this.skillevel = skillevel;
			this.masterlevel = masterlevel;
		}
		
		@Override
		public String toString() {
			return skillevel + ":" + masterlevel;
		}
	}
	
	public enum FameStatus {
		OK, NOT_TODAY, NOT_THIS_MONTH
	}
}