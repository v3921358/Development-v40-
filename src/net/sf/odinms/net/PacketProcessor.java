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

package net.sf.odinms.net;

import net.sf.odinms.net.channel.handler.BuddylistModifyHandler;
import net.sf.odinms.net.channel.handler.BuyCSItemHandler;
import net.sf.odinms.net.channel.handler.CancelBuffHandler;
import net.sf.odinms.net.channel.handler.CancelChairHandler;
import net.sf.odinms.net.channel.handler.CancelItemEffectHandler;
import net.sf.odinms.net.channel.handler.ChangeChannelHandler;
import net.sf.odinms.net.channel.handler.ChangeMapHandler;
import net.sf.odinms.net.channel.handler.ChangeMapSpecialHandler;
import net.sf.odinms.net.channel.handler.CharInfoRequestHandler;
import net.sf.odinms.net.channel.handler.ClientCrashHandler;
import net.sf.odinms.net.channel.handler.CloseRangeDamageHandler;
import net.sf.odinms.net.channel.handler.DamageSummonHandler;
import net.sf.odinms.net.channel.handler.DenyPartyRequestHandler;
import net.sf.odinms.net.channel.handler.DistributeAPHandler;
import net.sf.odinms.net.channel.handler.DistributeSPHandler;
import net.sf.odinms.net.channel.handler.DoorHandler;
import net.sf.odinms.net.channel.handler.EnterCashShopHandler;
import net.sf.odinms.net.channel.handler.FaceExpressionHandler;
import net.sf.odinms.net.channel.handler.GeneralchatHandler;
import net.sf.odinms.net.channel.handler.GiveFameHandler;
import net.sf.odinms.net.channel.handler.HealOvertimeHandler;
import net.sf.odinms.net.channel.handler.HitCoconutHandler;
import net.sf.odinms.net.channel.handler.ItemMoveHandler;
import net.sf.odinms.net.channel.handler.ItemPickupHandler;
import net.sf.odinms.net.channel.handler.MagicDamageHandler;
import net.sf.odinms.net.channel.handler.MesoDropHandler;
import net.sf.odinms.net.channel.handler.MessengerHandler;
import net.sf.odinms.net.channel.handler.MoveLifeHandler;
import net.sf.odinms.net.channel.handler.MovePetHandler;
import net.sf.odinms.net.channel.handler.MovePlayerHandler;
import net.sf.odinms.net.channel.handler.MoveSummonHandler;
import net.sf.odinms.net.channel.handler.NPCHandler;
import net.sf.odinms.net.channel.handler.NPCMoreTalkHandler;
import net.sf.odinms.net.channel.handler.NPCShopHandler;
import net.sf.odinms.net.channel.handler.NPCTalkHandler;
import net.sf.odinms.net.channel.handler.NoteActionHandler;
import net.sf.odinms.net.channel.handler.PartyOperationHandler;
import net.sf.odinms.net.channel.handler.PartychatHandler;
import net.sf.odinms.net.channel.handler.PetChatHandler;
import net.sf.odinms.net.channel.handler.PetCommandHandler;
import net.sf.odinms.net.channel.handler.PetFoodHandler;
import net.sf.odinms.net.channel.handler.PlayerInteractionHandler;
import net.sf.odinms.net.channel.handler.PlayerLoggedinHandler;
import net.sf.odinms.net.channel.handler.QuestActionHandler;
import net.sf.odinms.net.channel.handler.RangedAttackHandler;
import net.sf.odinms.net.channel.handler.ReactorHitHandler;
import net.sf.odinms.net.channel.handler.ReportHandler;
import net.sf.odinms.net.channel.handler.ScrollHandler;
import net.sf.odinms.net.channel.handler.SpawnPetHandler;
import net.sf.odinms.net.channel.handler.SpecialMoveHandler;
import net.sf.odinms.net.channel.handler.StorageHandler;
import net.sf.odinms.net.channel.handler.StrangeBossHandler;
import net.sf.odinms.net.channel.handler.SummonDamageHandler;
import net.sf.odinms.net.channel.handler.TakeDamageHandler;
import net.sf.odinms.net.channel.handler.TouchingCashShopHandler;
import net.sf.odinms.net.channel.handler.UseCashItemHandler;
import net.sf.odinms.net.channel.handler.UseChairHandler;
import net.sf.odinms.net.channel.handler.UseItemEffectHandler;
import net.sf.odinms.net.channel.handler.UseItemHandler;
import net.sf.odinms.net.channel.handler.UseSummonBagHandler;
import net.sf.odinms.net.channel.handler.VIPAddMapHandler;
import net.sf.odinms.net.channel.handler.WhisperHandler;
import net.sf.odinms.net.handler.KeepAliveHandler;
import net.sf.odinms.net.handler.LoginRequiringNoOpHandler;
import net.sf.odinms.net.login.handler.CharSelectedHandler;
import net.sf.odinms.net.login.handler.CheckCharNameHandler;
import net.sf.odinms.net.login.handler.CreateCharHandler;
import net.sf.odinms.net.login.handler.DeleteCharHandler;
import net.sf.odinms.net.login.handler.LoginChannelSelectHandler;
import net.sf.odinms.net.login.handler.LoginPasswordHandler;
import net.sf.odinms.net.login.handler.LoginWorldSelectHandler;
import net.sf.odinms.net.login.handler.RelogRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PacketProcessor {
	private static Logger log = LoggerFactory.getLogger(PacketProcessor.class);
	public enum Mode {
            LOGINSERVER,
            CHANNELSERVER
	};

	private static PacketProcessor instance;
	private MaplePacketHandler[] handlers;

	private PacketProcessor() {
            int maxRecvOp = 0;
            for (RecvPacketOpcode op : RecvPacketOpcode.values()) {
                if (op.getValue() > maxRecvOp) {
                    maxRecvOp = op.getValue();
                }
            }
            handlers = new MaplePacketHandler[maxRecvOp + 1];
	}

	public MaplePacketHandler getHandler(short packetId) {
            if (packetId > handlers.length) {
                return null;
            }
            MaplePacketHandler handler = handlers[packetId];
            if (handler != null) {
                return handler;
            }
            return null;
	}

	public void registerHandler(RecvPacketOpcode code, MaplePacketHandler handler) {
            handlers[code.getValue()] = handler;
	}

	public synchronized static PacketProcessor getProcessor(Mode mode) {
            if (instance == null) {
                instance = new PacketProcessor();
                instance.reset(mode);
            }
            return instance;
	}

	public void reset(Mode mode) {
            handlers = new MaplePacketHandler[handlers.length];
            registerHandler(RecvPacketOpcode.PONG, new KeepAliveHandler());
            registerHandler(RecvPacketOpcode.CLIENT_CRASH_REPORT, new ClientCrashHandler());
            if (mode == Mode.LOGINSERVER) {
                registerHandler(RecvPacketOpcode.LOGIN_WORLD_SELECT, new LoginWorldSelectHandler());
                registerHandler(RecvPacketOpcode.LOGIN_CHANNEL_SELECT, new LoginChannelSelectHandler());
                registerHandler(RecvPacketOpcode.CHAR_SELECT, new CharSelectedHandler());
                registerHandler(RecvPacketOpcode.LOGIN_PASSWORD, new LoginPasswordHandler());
                registerHandler(RecvPacketOpcode.CHECK_CHAR_NAME, new CheckCharNameHandler());
                registerHandler(RecvPacketOpcode.CREATE_CHAR, new CreateCharHandler());
                registerHandler(RecvPacketOpcode.DELETE_CHAR, new DeleteCharHandler()); // todo

                registerHandler(RecvPacketOpcode.RELOG, new RelogRequestHandler());
            } else if (mode == Mode.CHANNELSERVER) {
                registerHandler(RecvPacketOpcode.CHANGE_CHANNEL, new ChangeChannelHandler());
                registerHandler(RecvPacketOpcode.GENERAL_CHAT, new GeneralchatHandler());
                registerHandler(RecvPacketOpcode.PLAYER_LOGGEDIN, new PlayerLoggedinHandler());
                registerHandler(RecvPacketOpcode.CHANGE_MAP, new ChangeMapHandler());
                registerHandler(RecvPacketOpcode.MOVE_LIFE, new MoveLifeHandler());
                registerHandler(RecvPacketOpcode.MOVE_PLAYER, new MovePlayerHandler());
                registerHandler(RecvPacketOpcode.FACE_EXPRESSION, new FaceExpressionHandler());
                registerHandler(RecvPacketOpcode.CHAR_INFO_REQUEST, new CharInfoRequestHandler());
                registerHandler(RecvPacketOpcode.NPC_ANIMATION, new NPCHandler());
                registerHandler(RecvPacketOpcode.WHISPER, new WhisperHandler());
                registerHandler(RecvPacketOpcode.NPC_TALK, new NPCTalkHandler());
                registerHandler(RecvPacketOpcode.NPC_TALK_MORE, new NPCMoreTalkHandler());
                registerHandler(RecvPacketOpcode.ITEM_MOVE, new ItemMoveHandler());
                registerHandler(RecvPacketOpcode.MESO_DROP, new MesoDropHandler());
                registerHandler(RecvPacketOpcode.CLOSE_RANGE_ATTACK, new CloseRangeDamageHandler());
                registerHandler(RecvPacketOpcode.RANGED_ATTACK, new RangedAttackHandler());
                registerHandler(RecvPacketOpcode.MAGIC_ATTACK, new MagicDamageHandler());
                registerHandler(RecvPacketOpcode.TAKE_DAMAGE, new TakeDamageHandler());
                registerHandler(RecvPacketOpcode.USE_ITEM, new UseItemHandler());
                registerHandler(RecvPacketOpcode.USE_SUMMON_BAG, new UseSummonBagHandler());
                registerHandler(RecvPacketOpcode.USE_RETURN_SCROLL, new UseItemHandler());
                registerHandler(RecvPacketOpcode.USE_UPGRADE_SCROLL, new ScrollHandler());
                registerHandler(RecvPacketOpcode.HEAL_OVER_TIME, new HealOvertimeHandler());
                registerHandler(RecvPacketOpcode.ITEM_PICKUP, new ItemPickupHandler());
                registerHandler(RecvPacketOpcode.SPECIAL_MOVE, new SpecialMoveHandler());
                registerHandler(RecvPacketOpcode.PLAYER_INTERACTION, new PlayerInteractionHandler());
                registerHandler(RecvPacketOpcode.DISTRIBUTE_AP, new DistributeAPHandler());
                registerHandler(RecvPacketOpcode.DISTRIBUTE_SP, new DistributeSPHandler());
                registerHandler(RecvPacketOpcode.GIVE_FAME, new GiveFameHandler());
                registerHandler(RecvPacketOpcode.PARTY_OPERATION, new PartyOperationHandler());
                registerHandler(RecvPacketOpcode.DENY_PARTY_REQUEST, new DenyPartyRequestHandler());
                registerHandler(RecvPacketOpcode.PARTYCHAT, new PartychatHandler());
                registerHandler(RecvPacketOpcode.BUDDYLIST_MODIFY, new BuddylistModifyHandler());
                registerHandler(RecvPacketOpcode.USE_CHAIR, new UseChairHandler());
                registerHandler(RecvPacketOpcode.REPORT, new ReportHandler());
                registerHandler(RecvPacketOpcode.USE_CASH_ITEM, new UseCashItemHandler());
                registerHandler(RecvPacketOpcode.MOVE_SUMMON, new MoveSummonHandler());
                registerHandler(RecvPacketOpcode.SUMMON_ATTACK, new SummonDamageHandler());
                registerHandler(RecvPacketOpcode.DAMAGE_SUMMON, new DamageSummonHandler());
                registerHandler(RecvPacketOpcode.HANDLE_BOSS, new StrangeBossHandler());
                registerHandler(RecvPacketOpcode.MESSENGER, new MessengerHandler());
                registerHandler(RecvPacketOpcode.NPC_SHOP, new NPCShopHandler());
                registerHandler(RecvPacketOpcode.STORAGE, new StorageHandler());
                registerHandler(RecvPacketOpcode.USE_DOOR, new DoorHandler());
                registerHandler(RecvPacketOpcode.CANCEL_BUFF, new CancelBuffHandler());
                registerHandler(RecvPacketOpcode.TELEPORT_ROCK, new VIPAddMapHandler());
                registerHandler(RecvPacketOpcode.NOTE_ACTION, new NoteActionHandler());
                registerHandler(RecvPacketOpcode.HIT_COCONUT, new HitCoconutHandler());  
                // Cash Shop
                registerHandler(RecvPacketOpcode.ENTER_CASH_SHOP, new EnterCashShopHandler());
                registerHandler(RecvPacketOpcode.TOUCHING_CS, new TouchingCashShopHandler());
                registerHandler(RecvPacketOpcode.BUY_CS_ITEM, new BuyCSItemHandler());
                // Pets
                registerHandler(RecvPacketOpcode.SPAWN_PET, new SpawnPetHandler());
                registerHandler(RecvPacketOpcode.MOVE_PET, new MovePetHandler());
                registerHandler(RecvPacketOpcode.PET_CHAT, new PetChatHandler());
                registerHandler(RecvPacketOpcode.PET_COMMAND, new PetCommandHandler());
                registerHandler(RecvPacketOpcode.PET_FOOD, new PetFoodHandler());
                // todo/remove
                registerHandler(RecvPacketOpcode.QUEST_ACTION, new QuestActionHandler());
                registerHandler(RecvPacketOpcode.CANCEL_ITEM_EFFECT, new CancelItemEffectHandler());
                registerHandler(RecvPacketOpcode.CHANGE_MAP_SPECIAL, new ChangeMapSpecialHandler());
                registerHandler(RecvPacketOpcode.USE_ITEMEFFECT, new UseItemEffectHandler());
                registerHandler(RecvPacketOpcode.DAMAGE_REACTOR, new ReactorHitHandler());
            } else {
                throw new RuntimeException("Unknown packet processor mode");
            }
	}
}
