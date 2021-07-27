/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
               Matthias Butz <matze@odinms.de>
               Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author kevintjuh93 + bassoe + eric
 */
public class HitCoconutHandler extends AbstractMaplePacketHandler {
    
    // New instances.. 
    public HitCoconutHandler() {
    }
    
    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        /*CB 00 A6 00 06 01
         * A6 00 = coconut id
         * 06 01 = ?
         */
        int id = slea.readShort();
        MapleMap map = c.getPlayer().getMap();
        MapleCoconuts nut = map.getCoconut(id);
        if (!nut.isHittable()) return;
        if (System.currentTimeMillis() < nut.getHitTime()) return;

        if (nut.getHits() > 2 && Math.random() < 0.4 && !nut.isStopped()) {
            if (Math.random() < 0.008 && map.getStopped() > 0) {
                nut.setStopped(true);
                map.stopCoconut();
                map.broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 1));
                return;
            }

            nut.setHittable(false); // for sure :)
            nut.resetHits(); // For next event (without restarts)

            if (Math.random() < 0.05 && map.getBombings() > 0) {
                map.broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 2));
                map.bombCoconut();
            } else if (map.getFalling() > 0) {
                map.broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 3));
                map.fallCoconut();
                        if (c.getPlayer().getTeam() == 0) {
                            map.addMapleScore();
                            map.broadcastMessage(MaplePacketCreator.serverNotice(5, c.getPlayer().getName() + " of Team Maple knocks down a coconut."));
                        } else {
                            map.addStoryScore();
                            map.broadcastMessage(MaplePacketCreator.serverNotice(5, c.getPlayer().getName() + " of Team Story knocks down a coconut."));
                        }
                    map.broadcastMessage(MaplePacketCreator.coconutScore(map.getMapleScore(), map.getStoryScore()));
            }
        } else {
            nut.hit();
            map.broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 1));
        }
    }
    
    public class MapleCoconuts {
        private int id;
        private int hits = 0;
        private boolean hittable = false;
        private boolean stopped = false;
        private long hittime = System.currentTimeMillis();

        public MapleCoconuts(int id) {
            this.id = id;
        }

        public void hit() {
            this.hittime = System.currentTimeMillis() + 1000; // test
            hits++;
        }

        public int getHits() {
            return hits;
        }

        public void resetHits() {
            hits = 0;
        }

        public boolean isHittable() {
            return hittable;
        }

        public void setHittable(boolean hittable) {
            this.hittable = hittable;
        }

        public boolean isStopped() {
            return stopped;
        }

        public void setStopped(boolean stopped) {
            this.stopped = stopped;
        }

        public long getHitTime() {
            return hittime;
        }
    }
    
    public class MapleCoconut {
        private MapleMap map = null;

        public MapleCoconut(MapleMap map) {
            this.map = map;
        }

        public void startEvent() {
            //map.startEvent(); // don't need it lol
            map.broadcastMessage(MaplePacketCreator.hitCoconut(true, 0, 0));
            map.setCoconutsHittable(true);
            map.broadcastMessage(MaplePacketCreator.getClock(300));
            
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    if (map.getId() == 109080000) {
                        if (map.getMapleScore() == map.getStoryScore()) {
                            bonusTime();
                        } else if (map.getMapleScore() > map.getStoryScore()) {
                            for (MapleCharacter chr : map.getCharacters()) {
                                if (chr.getTeam() == 0) {
                                    chr.getClient().getSession().write(MaplePacketCreator.showEffect("event/coconut/victory"));
                                    chr.getClient().getSession().write(MaplePacketCreator.playSound("Coconut/Victory"));
                                } else {
                                    chr.getClient().getSession().write(MaplePacketCreator.showEffect("event/coconut/lose"));
                                    chr.getClient().getSession().write(MaplePacketCreator.playSound("Coconut/Failed"));
                                }
                            }
                            warpOut(0);
                        } else {
                            for (MapleCharacter chr : map.getCharacters()) {
                                 if (chr.getTeam() == 1) {
                                     chr.getClient().getSession().write(MaplePacketCreator.showEffect("event/coconut/victory"));
                                    chr.getClient().getSession().write(MaplePacketCreator.playSound("Coconut/Victory"));
                                } else {
                                     chr.getClient().getSession().write(MaplePacketCreator.showEffect("event/coconut/lose"));
                                    chr.getClient().getSession().write(MaplePacketCreator.playSound("Coconut/Failed"));
                                }
                            }
                            warpOut(1);
                        }
                    }
                }
            }, 300000);
        }

        public void bonusTime() {
            map.broadcastMessage(MaplePacketCreator.getClock(120));
            
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                     if (map.getMapleScore() == map.getStoryScore()) {
                         for (MapleCharacter chr : map.getCharacters()) {
                             chr.getClient().getSession().write(MaplePacketCreator.showEffect("event/coconut/lose"));
                            chr.getClient().getSession().write(MaplePacketCreator.playSound("Coconut/Failed"));
                        }
                        warpOut(-1);
                    } else if (map.getMapleScore() > map.getStoryScore()) {
                        for (MapleCharacter chr : map.getCharacters()) {
                            if (chr.getTeam() == 0) {
                                chr.getClient().getSession().write(MaplePacketCreator.showEffect("event/coconut/victory"));
                                chr.getClient().getSession().write(MaplePacketCreator.playSound("Coconut/Victory"));
                            } else {
                                chr.getClient().getSession().write(MaplePacketCreator.showEffect("event/coconut/lose"));
                                chr.getClient().getSession().write(MaplePacketCreator.playSound("Coconut/Failed"));
                            }
                        }
                        warpOut(0);
                     } else {
                        for (MapleCharacter chr : map.getCharacters()) {
                            if (chr.getTeam() == 1) {
                                chr.getClient().getSession().write(MaplePacketCreator.showEffect("event/coconut/victory"));
                                chr.getClient().getSession().write(MaplePacketCreator.playSound("Coconut/Victory"));
                            } else {
                                chr.getClient().getSession().write(MaplePacketCreator.showEffect("event/coconut/lose"));
                                chr.getClient().getSession().write(MaplePacketCreator.playSound("Coconut/Failed"));
                            }
                        }
                        warpOut(1);
                    }            
                }
            }, 120000);
        }

        public void warpOut(final int winteam) {
            map.setCoconutsHittable(false);
            TimerManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    for (MapleCharacter chr : map.getCharacters()) {
                        if (chr.getTeam() == winteam) {
                            chr.changeMap(109050000);
                        } else {
                            chr.changeMap(109050001);
                        }
                    }
                    map.resetCoconutScore();
                    map.setCoconut(null);
                }
            }, 12000);
        }
    }
}  