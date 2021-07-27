package net.sf.odinms.net.channel.handler;

import java.util.Random;

import net.sf.odinms.client.ExpTable;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class PetFoodHandler extends AbstractMaplePacketHandler {
    //private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PetFoodHandler.class);
    
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        // 8B 00 4D CB 1C 00 00 00 00 00 00 19
        //MaplePet pet = c.getPlayer().getPet(0);
        MapleCharacter player = c.getPlayer();
        if (player.getNoPets() == 0) {
            return;
        }
        slea.readShort();
        int itemId = slea.readInt();
        int previousFullness = 100;
        int slot = 0;
        
            if (player.getPet(slot) != null) {
                if (player.getPet(slot).getFullness() < previousFullness) {
                    previousFullness = player.getPet(slot).getFullness();
                }
            }
        
        MaplePet pet = player.getPet(slot);

        boolean gainCloseness = false;

        Random rand = new Random();
        int random = rand.nextInt(101);
        if (random <= 50) {
            gainCloseness = true;
        }
        if (pet.getFullness() < 100) {
            int newFullness = pet.getFullness() + 30;
            if (newFullness > 100) {
                newFullness = 100;
            }
            pet.setFullness(newFullness);
            if (gainCloseness && pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + ServerConstants.PET_EXP_RATE;
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.getSession().write(MaplePacketCreator.showOwnPetLevelUp(c.getPlayer().getPetIndex(pet)));
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showPetLevelUp(c.getPlayer(), c.getPlayer().getPetIndex(pet)));
                }
            }
            c.getSession().write(MaplePacketCreator.updatePet(pet, true));
            player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.commandResponse(c.getPlayer().getId(), (byte) 1, false, true), true);
        } else {
            if (gainCloseness) {
                int newCloseness = pet.getCloseness() - 1;
                if (newCloseness < 0) {
                    newCloseness = 0;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness < ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel(pet.getLevel() - 1);
                }
            }
            c.getSession().write(MaplePacketCreator.updatePet(pet, true));
            player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.commandResponse(c.getPlayer().getId(), (byte) 1, false, true), true);
        }
        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId, 1, true, false);
    }
}