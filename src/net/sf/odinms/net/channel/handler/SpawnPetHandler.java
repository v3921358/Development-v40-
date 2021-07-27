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

import java.util.Random;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.PetDataFactory;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class SpawnPetHandler extends AbstractMaplePacketHandler {

    /*	TODO:
     *	1. Move the equpping into a function.
     */
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte)slea.readShort();
        MapleCharacter player = c.getPlayer();
        MaplePet pet = MaplePet.loadFromDb(player.getInventory(MapleInventoryType.CASH).getItem(slot).getItemId(), slot, player.getInventory(MapleInventoryType.CASH).getItem(slot).getPetId());
        if (player.getPetIndex(pet) != -1) {
            player.unequipPet(pet, true);
        } else {
            if (player.getPet(0) != null) {
                player.unequipPet(player.getPet(0), false);
            }
            Point pos = player.getPosition();
            pos.y -= 12;
            pet.setPos(pos);
            pet.setFh(player.getMap().getFootholds().findBelow(pet.getPos()).getId());
            pet.setStance(0);
            player.addPet(pet);

            player.getMap().broadcastMessage(player, MaplePacketCreator.showPet(player, pet, false), true);
            List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>();
            stats.add(new Pair<MapleStat, Integer>(MapleStat.PET, Integer.valueOf(pet.getUniqueId())));
            c.getSession().write(MaplePacketCreator.petStatUpdate(player));
            c.getSession().write(MaplePacketCreator.enableActions());
            int hunger = PetDataFactory.getHunger(pet.getItemId());
            player.startFullnessSchedule(hunger, pet, player.getPetIndex(pet));
        }
    }
}