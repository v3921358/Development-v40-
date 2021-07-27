package net.sf.odinms.server.PlayerInteraction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author XoticStory
 */
public class HiredMerchant extends PlayerInteractionManager {

    private boolean open;
    public ScheduledFuture<?> schedule = null;
    private MapleMap map;
    private int itemId;

    public HiredMerchant(MapleCharacter owner, int itemId, String desc) {
        super(owner, itemId % 10, desc, 3);
        this.itemId = itemId;
        this.map = owner.getMap();
        this.schedule = TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                HiredMerchant.this.closeShop(true);
            }
        }, 1000 * 60 * 60 * 24);
    }

    public byte getShopType() {
        return -1;
    }

    @Override
    public void buy(MapleClient c, int item, short quantity) {
        MaplePlayerShopItem pItem = items.get(item);
        if (pItem.getBundles() > 0) {
            synchronized (items) {
                IItem newItem = pItem.getItem().copy();
                newItem.setQuantity(quantity);
                if (c.getPlayer().getMeso() >= pItem.getPrice() * quantity) {
                    if (MapleInventoryManipulator.addFromDrop(c, newItem, "")) {
                        Connection con = DatabaseConnection.getConnection();
                        try {
                            PreparedStatement ps = con.prepareStatement("UPDATE characters SET MerchantMesos = MerchantMesos + " + pItem.getPrice() * quantity + " WHERE id = ?");
                            ps.setInt(1, getOwnerId());
                            ps.executeUpdate();
                            ps.close();
                        } catch (SQLException se) {
                            se.printStackTrace();
                        }
                        c.getPlayer().gainMeso(-pItem.getPrice() * quantity, false);
                        pItem.setBundles((short) (pItem.getBundles() - quantity));
                    } else {
                        c.getPlayer().dropMessage(1, "Your inventory is full.");
                    }
                } else {
                    c.getPlayer().dropMessage(1, "You do not have enough mesos.");
                }
            }
        }
    }

    @Override
    public void closeShop(boolean saveItems) {
        map.removeMapObject(this);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET HasMerchant = 0 WHERE id = ?");
            ps.setInt(1, getOwnerId());
            ps.executeUpdate();
            ps.close();
            if (saveItems) {
                saveItems();
            }
        } catch (SQLException se) {
        }
        schedule.cancel(false);
    }

    public boolean isOpen() {
        return this.open;
    }

    public void setOpen(boolean set) {
        this.open = set;
    }

    public MapleMap getMap() {
        return map;
    }

    public int getItemId() {
        return itemId;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MapleMapObjectType getType() {
        return null;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
    }
}