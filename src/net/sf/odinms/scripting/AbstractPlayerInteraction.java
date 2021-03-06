package net.sf.odinms.scripting;

import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleQuestStatus;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.quest.MapleQuest;
import net.sf.odinms.tools.MaplePacketCreator;

public class AbstractPlayerInteraction {
	private MapleClient c;
	
	public AbstractPlayerInteraction(MapleClient c) {
		this.c = c;
	}
	
	protected MapleClient getClient() {
		return c;
	}
	
	public MapleCharacter getPlayer() {
		return c.getPlayer();
	}
	
	public void warp(int map) {
		MapleMap target = getWarpMap(map);
		c.getPlayer().changeMap(target, target.getPortal(0));
	}

	public void warp(int map, int portal) {
		MapleMap target = getWarpMap(map);
		c.getPlayer().changeMap(target, target.getPortal(portal));
	}

	public void warp(int map, String portal) {
		MapleMap target = getWarpMap(map);
		c.getPlayer().changeMap(target, target.getPortal(portal));
	}
	
	private MapleMap getWarpMap(int map) {
		MapleMap target;
		if (getPlayer().getEventInstance() == null) {
			target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(map);
		}
		else {
			target = getPlayer().getEventInstance().getMapInstance(map);
		}
		return target;
	}
	
	public boolean haveItem(int itemid) {
		return haveItem(itemid, 1);
	}
	
	public boolean haveItem(int itemid, int quantity) {
		return haveItem(itemid, quantity, false, true);
	}
	
	public boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
		return c.getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
	}
	
	public MapleQuestStatus.Status getQuestStatus(int id) {
		return c.getPlayer().getQuest(MapleQuest.getInstance(id)).getStatus();
	}
	
	/**
	 * Gives item with the specified id or takes it if the quantity is negative. Note that this does NOT take items from the equipped inventory.
	 * @param id
	 * @param quantity
	 */
	public void gainItem(int id, short quantity) {
		if (quantity >= 0) {
			StringBuilder logInfo = new StringBuilder(c.getPlayer().getName());
			logInfo.append(" received ");
			logInfo.append(quantity);
			logInfo.append(" from a scripted PlayerInteraction (");
			logInfo.append(this.toString());
			logInfo.append(")");
			MapleInventoryManipulator.addById(c, id, quantity, logInfo.toString());
		} else {
			MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
		}
		c.getSession().write(MaplePacketCreator.getShowItemGain(id,quantity, true));
	}
	
	
	public MapleParty getParty() {
		return (c.getPlayer().getParty());
	}
	
	public boolean isLeader() {
		return (getParty().getLeader().equals(new MaplePartyCharacter(c.getPlayer())));
	}
	
	//PQ methods: give items/exp to all party members
	public void givePartyItems(int id, short quantity, List<MapleCharacter> party) {
		for(MapleCharacter chr : party) {
			MapleClient cl = chr.getClient();
			if (quantity >= 0) {
				StringBuilder logInfo = new StringBuilder(cl.getPlayer().getName());
				logInfo.append(" received ");
				logInfo.append(quantity);
				logInfo.append(" from event ");
				logInfo.append(chr.getEventInstance().getName());
				MapleInventoryManipulator.addById(cl, id, quantity, logInfo.toString());
			} else {
				MapleInventoryManipulator.removeById(cl, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
			}
			cl.getSession().write(MaplePacketCreator.getShowItemGain(id,quantity, true));
		}
	}
	
	//PQ gain EXP: Multiplied by channel rate here to allow global values to be input direct into NPCs
	public void givePartyExp(int amount, List<MapleCharacter> party) {
		for(MapleCharacter chr : party) {
			chr.gainExp(amount * ServerConstants.EXP_RATE, true, true);
		}
	}
	
	//remove all items of type from party
	//combination of haveItem and gainItem
	public void removeFromParty(int id, List<MapleCharacter> party) {
		for (MapleCharacter chr : party) {
			MapleClient cl = chr.getClient();
			MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(id);
			MapleInventory iv = cl.getPlayer().getInventory(type);
			int possesed = iv.countById(id);
			
			if (possesed > 0) {
				MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, possesed, true, false);
				cl.getSession().write(MaplePacketCreator.getShowItemGain(id, (short)-possesed, true));
			}
		}
	}
	
	//remove all items of type from character
	//combination of haveItem and gainItem
	public void removeAll(int id) {
		MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(id);
		MapleInventory iv = c.getPlayer().getInventory(type);
		int possesed = iv.countById(id);

		if (possesed > 0) {
			MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, possesed, true, false);
			c.getSession().write(MaplePacketCreator.getShowItemGain(id, (short)-possesed, true));
		}
	}
}
