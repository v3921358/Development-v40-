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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.CashItemFactory;
import net.sf.odinms.server.CashItemInfo;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Acrylic (Terry Han)
 */
public class BuyCSItemHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        System.out.println(slea.toString());
        int action = slea.readByte();
        if (action == 0x02) {
            boolean maplePoints = slea.readByte() > 0;
            int snCS = slea.readInt();
            CashItemInfo item = CashItemFactory.getItem(snCS);
            if (c.getPlayer().getCSPoints(maplePoints ? 2 : 1) >= item.getPrice()) {
                c.getPlayer().modifyCSPoints(maplePoints ? 2 : 1, -item.getPrice());
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                AutobanManager.getInstance().autoban(c, "Trying to purchase from the CS when they have no NX");
                return;
            }
            if(item.getId() >= 5390000 && item.getId() <= 5390002){
                c.getPlayer().dropMessage(1, "You may not purchase this item");
                return;
            }
            if (item.getId() >= 5000000 && item.getId() <= 5000100) {
                int petId = MaplePet.createPet(item.getId());
                if (petId == -1) {
                    return;
                }
                MapleInventoryManipulator.addById(c, item.getId(), (short) 1, null, petId); // big todo >_>
            } else {
                MapleInventoryManipulator.addById(c, item.getId(), (short) item.getCount(), "");
            }
            c.getSession().write(MaplePacketCreator.showBoughtCSItem(c.getPlayer(), item));
            c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if (action == 0x04) {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("DELETE FROM wishlist WHERE charid = ?");
                ps.setInt(1, c.getPlayer().getId());
                ps.executeUpdate();
                ps.close();
                int i = 10;
                while (i > 0) {
                    int sn = slea.readInt();
                    if (sn != 0) {
                        ps = con.prepareStatement("INSERT INTO wishlist(charid, sn) VALUES(?, ?) ");
                        ps.setInt(1, c.getPlayer().getId());
                        ps.setInt(2, sn);
                        ps.executeUpdate();
                        ps.close();
                    }
                    i--;
                }
            } catch (SQLException se) {
            }
            c.getSession().write(MaplePacketCreator.sendWishList(c.getPlayer().getId(), true));
        } else if (action == 6) { // Increase Inventory Space
            slea.skip(1);
            byte toCharge = slea.readByte();
            int weirdData = slea.readInt(); // Figured this is probably useless now, since the action changed.
            byte toIncrease = slea.readByte();
            if (c.getPlayer().getCSPoints(toCharge) >= 4000) { // 48 is max.
                c.getPlayer().modifyCSPoints(toCharge, 4000);
                //if (c.getPlayer().getSlots(toIncrease) < 48) {
                    if (toIncrease >= 1 && toIncrease <= 4) {
                        //c.getPlayer().gainSlots(toIncrease, 4);
                    }
                //}
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else if (action == 7) { // Increase slot space
            slea.skip(1);
            byte toCharge = slea.readByte();
            int toIncrease = slea.readInt();
//            if (c.getPlayer().getCSPoints(toCharge) >= 4000 && c.getPlayer().getStorage().getSlots() < 48) { // 48 is max.
                c.getPlayer().modifyCSPoints(toCharge, 4000);
                if (toIncrease == 0) { // Increase Storage
                    //c.getPlayer().getStorage().gainSlots((byte)4);
                }
                c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
		c.getSession().write(MaplePacketCreator.enableActions());
//            }
      /*  } else if (action == 15) { //put item in cash inventory
            int uniqueid = (int) slea.readLong();
            MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
            Item item = c.getPlayer().getInventory(type).findByUniqueId(uniqueid);
            if (item != null && item.getQuantity() > 0 && item.getUniqueId() > 0 && c.getPlayer().getCashInventory().getItemsSize() < 100) {
                Item item_ = item.copy();
                MapleInventoryManipulator.removeFromSlot(c, type, item.getPosition(), item.getQuantity(), false);
                if (item_.getPet() != null) {
                    c.getPlayer().removePetCS(item_.getPet());
                }
                item_.setPosition((byte) 0);
                c.getPlayer().getCashInventory().addToInventory(item_);
                //warning: this d/cs
                //c.getSession().write(MTSCSPacket.confirmToCSInventory(item, c.getAccID(), c.getPlayer().getCashInventory().getSNForItem(item)));
            }*/
        } else if (action == 0x0A) {
            long rid = slea.readLong();
            int cashid = (int) rid;
            byte inv = slea.readByte();
            short slot = slea.readShort();
            System.out.println("CashID: "  + cashid + " Inv: " + inv + " Slot: " + slot);
            c.getSession().write(MaplePacketCreator.sendItemInventory(null, slot));
            CashItemInfo item = CashItemFactory.getItem(cashid);
            if (ServerConstants.isPet(item.getId())) {
                return; // todo
            } else {
                //Item itema = new Item(item.getId(), (byte)slot, (short)1);
            }
            
            c.getSession().write(MaplePacketCreator.enableActions());            
            
//            int snCS = slea.readInt();
//            if (c.getPlayer().getMeso() >= item.getPrice()) {
//                c.getPlayer().gainMeso(-item.getPrice(), false);
//                MapleInventoryManipulator.addById(c, item.getId(), (short) item.getCount(), "");
//            } else {
//                AutobanManager.getInstance().autoban(c, "Trying to purchase from the CS with an insufficient amount");
//                return;
//            }
        }
    }
}