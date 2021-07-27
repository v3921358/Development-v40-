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

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleSnowball;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class CloseRangeDamageHandler extends AbstractDealDamageHandler {
        public int startPoint0 = 0, startPoint1 = 0;
    
	private boolean isFinisher(int skillId) {
            return skillId >= 1111003 && skillId <= 1111006;
	}
	
	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
                MapleCharacter player = c.getPlayer();
                
                int extraDistance0 = startPoint0 * 256, extraDistance1 = startPoint1 * 256;
                if (player.getMapId() == 109060000) {
                    int team = c.getPlayer().getPosition().y > -80 ? 0 : 1;
                    int distance0 = player.getMap().getSnowBall(0).getPosition().x, distance1 = player.getMap().getSnowBall(1).getPosition().x;
                    int playerPos = (player.getPosition().x - 400) / 3; // Proportional to snowballpos
                    int damage = Math.random() < 0.01 ? 10 : 0;
                    final MapleSnowball ball = player.getMap().getSnowBall(team);
                    if (ball != null)
                        if (ball.getPosition().x == 65) { // Going to stage 1
                            if (damage == 10) {
                                player.getMap().broadcastMessage(MaplePacketCreator.hitSnowBall(team, damage));
                                ball.setPositionX((ball.getPosition().x + 1));
                                player.getMap().broadcastMessage(MaplePacketCreator.rollSnowball(0, distance0 - extraDistance0, distance1 - extraDistance1, startPoint0, startPoint1));
                            } else {
                                player.getMap().broadcastMessage(MaplePacketCreator.hitSnowBall(team, damage));
                            }
                        } else if (ball.getPosition().x == 255 || ball.getPosition().x == 511 || ball.getPosition().x == 767) { // Other stages
                            if (damage == 10) {
                                player.getMap().broadcastMessage(MaplePacketCreator.hitSnowBall(team, damage));
                                ball.setPositionX((ball.getPosition().x + 1));
                                player.getMap().broadcastMessage(MaplePacketCreator.rollSnowball(0, distance0 - extraDistance0, distance1 - extraDistance1, startPoint0, startPoint1));
                                if (team == 0) {
                                    startPoint0++;
                                } else {
                                    startPoint1++;
                                }
                            } else {
                                player.getMap().broadcastMessage(MaplePacketCreator.hitSnowBall(team, damage));
                            }
                        } else if (ball.getPosition().x == 899) { // Crossing the finishing line
                            player.getMap().broadcastMessage(MaplePacketCreator.hitSnowBall(team, 10));  
                            player.getMap().getSnowBall(0).setPositionX(0);
                            player.getMap().getSnowBall(1).setPositionX(0);
                            player.getMap().broadcastMessage(MaplePacketCreator.rollSnowball(0, 0, 0, 0, 0));
                            for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                                chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "Congratulations! Team " + ball.getTeam() + " has won the Snowball Event!"));
                                chr.changeMap(c.getPlayer().getClient().getChannelServer().getMapFactory().getMap(team == ball.getTeam() ? 109050000 : 910000000), c.getPlayer().getClient().getChannelServer().getMapFactory().getMap(team == ball.getTeam() ? 109050000 : 910000000).getPortal(0));
                            }
                    } else {
                        if (playerPos >= ball.getPosition().x) { // In case lag happens and the person is able to get pass the snowball
                            c.getSession().write(MaplePacketCreator.leftKnockBack());
                            c.getSession().write(MaplePacketCreator.enableActions());
                        } else if (playerPos < ball.getPosition().x && playerPos >= (ball.getPosition().x - 40)) {
                            player.getMap().broadcastMessage(MaplePacketCreator.hitSnowBall(team, 10));
                            ball.setPositionX((ball.getPosition().x + 1));
                            player.getMap().broadcastMessage(MaplePacketCreator.rollSnowball(0, distance0 - extraDistance0, distance1 - extraDistance1, startPoint0, startPoint1));
                        } else if (player.getPosition().x < -360 && player.getPosition().x > -560) {
                            player.getMap().broadcastMessage(MaplePacketCreator.hitSnowBall(team + 2, 15)); // Hitting the snowman
                        }
                    }
                    return; // no need to parse, we don't even allow skills i don't think
                }
            
		AttackInfo attack = parseDamage(slea, (byte)0);
		
		MaplePacket packet = MaplePacketCreator.closeRangeAttack(player.getId(), attack.skill, attack.stance, attack.direction, attack.numAttackedAndDamage, attack.allDamage);
		player.getMap().broadcastMessage(player, packet, false, true);
                
		// handle combo orbconsume
		int numFinisherOrbs = 0;
		Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
		if (isFinisher(attack.skill)) {
                    if (comboBuff != null) {
                        numFinisherOrbs = comboBuff.intValue() - 1; 
                    }
                    player.handleOrbconsume();
		} else if (attack.numAttacked > 0 && comboBuff != null) {
                    // handle combo orbgain
                    if (attack.skill != 1111008) { // shout should not give orbs
                        player.handleOrbgain();
                    }
		}
		// handle sacrifice hp loss
		if(attack.numAttacked > 0 && attack.skill == 1311005) {
		    int totDamageToOneMonster = attack.allDamage.get(0).getRight().get(0).intValue(); // sacrifice attacks only 1 mob with 1 attack
                    player.setHp(player.getHp() - totDamageToOneMonster * attack.getAttackEffect(player).getX() / 100);
		    player.updateSingleStat(MapleStat.HP, player.getHp());
		}
		// handle charged blow
		if (attack.numAttacked > 0 && attack.skill == 1211002) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
		}

		int maxdamage = c.getPlayer().getCurrentMaxBaseDamage();
		int attackCount = 1;
		if (attack.skill != 0) {
                    MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());
                    attackCount = effect.getAttackCount();
                    maxdamage *= effect.getDamage() / 100.0;
                    maxdamage *= attackCount;
		}
		maxdamage = Math.min(maxdamage, 99999);
		if (attack.skill == 4211006) {
                    maxdamage = 700000;
		} else if (numFinisherOrbs > 0) {
                    maxdamage *= numFinisherOrbs;
		} else if (comboBuff != null) {
                    ISkill combo = SkillFactory.getSkill(1111002);
                    int comboLevel = player.getSkillLevel(combo);
                    MapleStatEffect comboEffect = combo.getEffect(comboLevel);
                    double comboMod = 1.0 + (comboEffect.getDamage() / 100.0 - 1.0) * (comboBuff.intValue() - 1);
                    maxdamage *= comboMod;
		}
		if (numFinisherOrbs == 0 && isFinisher(attack.skill)) {
                    return; // can only happen when lagging o.o
		}
		if (isFinisher(attack.skill)) {
                    maxdamage = 99999; // FIXME reenable damage calculation for finishers
		}
		applyAttack(attack, player, maxdamage, attackCount);
	}
}
