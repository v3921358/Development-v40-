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

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePlayerShop;
import net.sf.odinms.server.MaplePlayerShopItem;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.PlayerInteraction.IPlayerInteractionManager;
import net.sf.odinms.server.PlayerInteraction.MapleMiniGame;
import net.sf.odinms.server.PlayerInteraction.MapleMiniGame.MiniGameType;
import net.sf.odinms.server.PlayerInteraction.PlayerInteractionManager;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public class PlayerInteractionHandler extends AbstractMaplePacketHandler {
	private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PlayerInteractionHandler.class);	
	
	private enum Action {
		CREATE(0),
		INVITE(2),
		DECLINE(3),
		VISIT(4),
		CHAT(6),
		EXIT(0xA),
		OPEN(0xB),
		SET_ITEMS(0xD),
		SET_MESO(0xE),
		CONFIRM(0xF),
		ADD_ITEM(0x12),
		BUY(0x14),
		REMOVE_ITEM(0x18),
                REQUEST_TIE(0x19),
                ANSWER_TIE(45),
                GIVE_UP(46),
                EXIT_AFTER_GAME(50),
                CANCEL_EXIT(51),
                READY(0x20),
                UN_READY(0x21),
                MOVE_OMOK(58),
                START(0x23),
                SKIP(57),
                SELECT_CARD(62);
		
		final byte code;
		
		private Action(int code) {
			this.code = (byte) code;
		}
		
		public byte getCode() {
			return code;
		}
	}

        @Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
            System.out.println("INTERACTION PACKET: " + slea.toString());
		byte mode = slea.readByte();
		if (mode == Action.CREATE.getCode()) {
			byte createType = slea.readByte();
			if (createType == 3) { // trade
                            MapleTrade.startTrade(c.getPlayer());
                        } else if (createType == 1 || createType == 2) { // omok
                            String desc = slea.readMapleAsciiString();
                            String pass = null;
                            if (slea.readByte() == 1) {
                                pass = slea.readMapleAsciiString();
                            }
                            int type = slea.readByte();
                            IPlayerInteractionManager game = new MapleMiniGame(c.getPlayer(), type, desc);
                            c.getPlayer().setInteraction(game);
                            if (createType == 1) {
                                ((MapleMiniGame) game).setGameType(MiniGameType.OMOK);
                            } else if (createType == 2) {
                                if (type == 0) {
                                    ((MapleMiniGame) game).setMatchesToWin(6);
                                } else if (type == 1) {
                                    ((MapleMiniGame) game).setMatchesToWin(10);
                                } else if (type == 2) {
                                    ((MapleMiniGame) game).setMatchesToWin(15);
                                }
                                ((MapleMiniGame) game).setGameType(MiniGameType.MATCH_CARDS);
                            }
                            c.getPlayer().getMap().addMapObject((PlayerInteractionManager) game);
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendInteractionBox(c.getPlayer()));
                            ((MapleMiniGame) game).sendSpawnData(c);
			} else if (createType == 4) { // shop
				String desc = slea.readMapleAsciiString();
				slea.skip(3);
                                slea.readInt(); // item id
				MaplePlayerShop shop = new MaplePlayerShop(c.getPlayer(), desc);
				c.getPlayer().setPlayerShop(shop);
				c.getPlayer().getMap().addMapObject(shop);
				shop.sendShop(c);
			}
		} else if (mode == Action.INVITE.getCode()) {
			int otherPlayer = slea.readInt();
			MapleCharacter otherChar = c.getPlayer().getMap().getCharacterById(otherPlayer);
			MapleTrade.inviteTrade(c.getPlayer(), otherChar);
		} else if (mode == Action.DECLINE.getCode()) {
			MapleTrade.declineTrade(c.getPlayer());
		} else if (mode == Action.VISIT.getCode()) {
			// we will ignore the trade oids for now
			if (c.getPlayer().getTrade() != null && c.getPlayer().getTrade().getPartner() != null) {
				MapleTrade.visitTrade(c.getPlayer(), c.getPlayer().getTrade().getPartner().getChr());
			} else {
				int oid = slea.readInt();
                                MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
                                if (ob instanceof IPlayerInteractionManager && c.getPlayer().getInteraction() == null) {
                                    IPlayerInteractionManager ips = (IPlayerInteractionManager) ob;
                                    //if (ips.getShopType() == 4) {
                                        //if (((MaplePlayerShop) ips).isBanned(c.getPlayer().getName())) {
                                          //  c.getPlayer().dropMessage(1, "You have been banned from this store.");
                                          //  return;
                                        //}
                                    //}
                                    if (ips.getFreeSlot() == -1) {
                                        c.getSession().write(MaplePacketCreator.getMiniBoxFull());
                                        return;
                                    }
                                    c.getPlayer().setInteraction(ips);
                                    ips.addVisitor(c.getPlayer());
                                    ((MapleMiniGame)ips).getOwner().getClient().getSession().write(MaplePacketCreator.addVisitor(c.getPlayer(), true));
                                    c.getSession().write(MaplePacketCreator.getInteraction(c.getPlayer(), false));
                                }
			}
		} else if (mode == Action.CHAT.getCode()) { // chat lol
			if (c.getPlayer().getTrade() != null) {
				c.getPlayer().getTrade().chat(slea.readMapleAsciiString());
                        } else if (c.getPlayer().getInteraction() != null) {
                            IPlayerInteractionManager ips = c.getPlayer().getInteraction();
                            String message = slea.readMapleAsciiString();
                            ips.broadcast(MaplePacketCreator.shopChat(c.getPlayer().getName() + " : " + message, ips.isOwner(c.getPlayer()) ? 0 : ips.getVisitorSlot(c.getPlayer()) + 1), true);
                        } else {
				MaplePlayerShop shop = c.getPlayer().getPlayerShop();
				if (shop != null) {
                                    shop.chat(c, slea.readMapleAsciiString());
				}
			}
		} else if (mode == Action.EXIT.getCode()) {
			if (c.getPlayer().getTrade() != null) {
                            MapleTrade.cancelTrade(c.getPlayer());
			} else if (c.getPlayer().getPlayerShop() != null) {
                            MaplePlayerShop shop = c.getPlayer().getPlayerShop();
                            if (shop != null) {
                                c.getPlayer().setPlayerShop(null);
                                if (shop.isOwner(c.getPlayer())) {
                                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.updateCharBox(c.getPlayer()));
                                    // return the items not sold
                                    for (MaplePlayerShopItem item : shop.getItems()) {
                                        IItem iItem = item.getItem().copy();
                                        iItem.setQuantity((short) (item.getBundles() * iItem.getQuantity()));
                                        StringBuilder logInfo = new StringBuilder("Returning items not sold in ");
                                        logInfo.append(c.getPlayer().getName());
                                        logInfo.append("'s shop");
                                        MapleInventoryManipulator.addFromDrop(c, iItem, logInfo.toString());
                                    }
                                } else {
                                    shop.removeVisitor(c.getPlayer());
                                }
                            }
                        } else {
                            MapleMiniGame ips = (MapleMiniGame) c.getPlayer().getInteraction();                            
                            if (ips != null) {
                                if (ips.isOwner(c.getPlayer())) {
                                    if (ips.getShopType() == 1 || ips.getShopType() == 2) { // omok
                                        ips.closeShop(true);
                                        ips.removeAllVisitors(3, 1);
                                    }
                                } else {
                                    ips.removeVisitor(c.getPlayer());
                                }
                            }
                            c.getPlayer().setInteraction(null);
			}
		} else if (mode == Action.OPEN.getCode()) {
                    MaplePlayerShop shop = c.getPlayer().getPlayerShop();
                    if (shop != null && shop.isOwner(c.getPlayer())) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.updateCharBox(c.getPlayer()));
                    }
                } else if (mode == Action.READY.getCode()) {
                    ((MapleMiniGame)c.getPlayer().getInteraction()).broadcast(MaplePacketCreator.getMiniGameReady(), true);
                } else if (mode == Action.UN_READY.getCode()) {
                    ((MapleMiniGame)c.getPlayer().getInteraction()).broadcast(MaplePacketCreator.getMiniGameUnReady(), true);
                } else if (mode == Action.START.getCode()) {
                    MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
                    if (game.getGameType() == MiniGameType.OMOK) {
                        //game.broadcast(MaplePacketCreator.getMiniGameStart(game.getLoser()), true);
                        c.getSession().write(MaplePacketCreator.getMiniGameStart(game.getLoser()));
                    } else if (game.getGameType() == MiniGameType.MATCH_CARDS) {
                        game.shuffleList();
                        game.broadcast(MaplePacketCreator.getMatchCardStart(game), true);
                    }
                    //c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendInteractionBox(game.getOwner()));
                    game.setStarted(true);
                } else if (mode == Action.GIVE_UP.getCode()) {
                    MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
                    if (game.getGameType() == MiniGameType.OMOK) {
    //                    game.broadcast(MaplePacketCreator.getMiniGameForfeit(game, game.isOwner(c.getPlayer()) ? 0 : 1), true);
                    } else if (game.getGameType() == MiniGameType.MATCH_CARDS) {
                        if (game.isOwner(c.getPlayer())) {
      //                      game.broadcast(MaplePacketCreator.getMiniGameWin(game, 1), true);
                        } else {
        //                    game.broadcast(MaplePacketCreator.getMiniGameWin(game, 0), true);
                        }
                    }
                } else if (mode == Action.REQUEST_TIE.getCode()) {
                    MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
                    if (game.isOwner(c.getPlayer())) {
                        game.getVisitors()[0].getClient().getSession().write(MaplePacketCreator.getMiniGameRequestTie());
                    } else {
                        game.getOwner().getClient().getSession().write(MaplePacketCreator.getMiniGameRequestTie());
                    }
                } else if (mode == Action.ANSWER_TIE.getCode()) {
                    MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
                    if (slea.readByte() == 1) {
                      //  game.broadcast(MaplePacketCreator.getMiniGameTie(game), true);
                    } else {
                        game.broadcast(MaplePacketCreator.getMiniGameDenyTie(), true);
                    }
                } else if (mode == Action.SKIP.getCode()) {
                    IPlayerInteractionManager game = c.getPlayer().getInteraction();
                    //game.broadcast(MaplePacketCreator.getMiniGameSkipTurn(game.isOwner(c.getPlayer()) ? 0 : 1), true);
                } else if (mode == Action.MOVE_OMOK.getCode()) {
                    int x = slea.readInt(); // x point
                    int y = slea.readInt(); // y point
                    int type = slea.readByte(); // piece ( 1 or 2; Owner has one piece, visitor has another, it switches every game.)
                    ((MapleMiniGame) c.getPlayer().getInteraction()).setPiece(x, y, type, c.getPlayer());
                } else if (mode == Action.SELECT_CARD.getCode()) {
                    int turn = slea.readByte(); // 1st turn = 1; 2nd turn = 0
                    int slot = slea.readByte(); // slot
                    MapleMiniGame game = (MapleMiniGame) c.getPlayer().getInteraction();
                    int firstslot = game.getFirstSlot();
                    if (turn == 1) {
                        game.setFirstSlot(slot);
                        //game.broadcast(MaplePacketCreator.getMatchCardSelect(turn, slot, firstslot, turn), !game.isOwner(c.getPlayer()));
                    } else if (game.getCardId(firstslot + 1) == game.getCardId(slot + 1)) {
                        if (game.isOwner(c.getPlayer())) {
                            //game.broadcast(MaplePacketCreator.getMatchCardSelect(turn, slot, firstslot, 2), true);
                            game.setOwnerPoints();
                        } else {
                          //  game.broadcast(MaplePacketCreator.getMatchCardSelect(turn, slot, firstslot, 3), true);
                            game.setVisitorPoints();
                        }
                    } else {
                        //game.broadcast(MaplePacketCreator.getMatchCardSelect(turn, slot, firstslot, game.isOwner(c.getPlayer()) ? 0 : 1), true);
                    }
		} else if (mode == Action.SET_MESO.getCode()) {
			c.getPlayer().getTrade().setMeso(slea.readInt());
		} else if (mode == Action.SET_ITEMS.getCode()) {
			MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
			MapleInventoryType ivType = MapleInventoryType.getByType(slea.readByte());
			IItem item = c.getPlayer().getInventory(ivType).getItem((byte) slea.readShort());
			short quantity = slea.readShort();
			byte targetSlot = slea.readByte();
			if (c.getPlayer().getTrade() != null) {
				if ((quantity <= item.getQuantity() && quantity >= 0) || ii.isThrowingStar(item.getItemId())) {
					IItem tradeItem = item.copy();
					if (ii.isThrowingStar(item.getItemId())) {
						tradeItem.setQuantity(item.getQuantity());
						MapleInventoryManipulator.removeFromSlot(c, ivType, item.getPosition(), item.getQuantity(), true);
					} else {
						tradeItem.setQuantity(quantity);
						MapleInventoryManipulator.removeFromSlot(c, ivType, item.getPosition(), quantity, true);
					}
					tradeItem.setPosition(targetSlot);
					c.getPlayer().getTrade().addItem(tradeItem);
				} else if (quantity < 0) {
					log.info("[h4x] {} Trading negative amounts of an item", c.getPlayer().getName());
				}
			}
		} else if (mode == Action.CONFIRM.getCode()) {
			MapleTrade.completeTrade(c.getPlayer());
		} else if (mode == Action.ADD_ITEM.getCode()) {
			MaplePlayerShop shop = c.getPlayer().getPlayerShop();
			if (shop != null && shop.isOwner(c.getPlayer())) {
				MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                                System.out.println("Type: " + type.toString());
				byte slot = (byte) slea.readShort();
                                System.out.println("Slot: " + slot);
				short bundles = slea.readShort();
                                System.out.println("Bundles: " + bundles);
				short perBundle = slea.readShort();
                                System.out.println("Per Bundle: " + perBundle);
				int price = slea.readInt();
                                System.out.println("Price: " + price);
				IItem ivItem = c.getPlayer().getInventory(type).getItem(slot);
				if (ivItem != null && ivItem.getQuantity() >= bundles * perBundle) {
					IItem sellItem = ivItem.copy();
					sellItem.setQuantity(perBundle);
					MaplePlayerShopItem item = new MaplePlayerShopItem(shop, sellItem, bundles, price);
					shop.addItem(item);
					// can be put in addItem without faek o.o
					MapleInventoryManipulator.removeFromSlot(c, type, slot, (short) (bundles * perBundle), true);
					c.getSession().write(MaplePacketCreator.getPlayerShopItemUpdate(shop));
				}
				
			}			
		} else if (mode == Action.REMOVE_ITEM.getCode()) {
			MaplePlayerShop shop = c.getPlayer().getPlayerShop();
			if (shop != null && shop.isOwner(c.getPlayer())) {
                            int slot = slea.readShort();
                            MaplePlayerShopItem item = shop.getItems().get(slot);
                            IItem ivItem = item.getItem().copy();
                            shop.removeItem(slot);
                            ivItem.setQuantity((short) (item.getBundles() * ivItem.getQuantity()));
                            StringBuilder logInfo = new StringBuilder("Taken out from player shop by ");
                            logInfo.append(c.getPlayer().getName());
                            MapleInventoryManipulator.addFromDrop(c, ivItem, logInfo.toString());
                            c.getSession().write(MaplePacketCreator.getPlayerShopItemUpdate(shop));
			}
		}
	}

}
