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

import javax.script.Invocable;
import javax.script.ScriptEngine;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.client.messages.commands.AbstractCommandScriptManager;
import net.sf.odinms.client.messages.commands.DonatorCommand;
import net.sf.odinms.client.messages.commands.PlayerCommand;
import net.sf.odinms.constants.ServerConstants.PlayerGMRank;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class GeneralchatHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String text = slea.readMapleAsciiString();
        if (StringUtil.countCharacters(text, '@') > 4 || StringUtil.countCharacters(text, '%') > 4 ||
                StringUtil.countCharacters(text, '+') > 6 || StringUtil.countCharacters(text, '$') > 6 ||
                StringUtil.countCharacters(text, '&') > 6 || StringUtil.countCharacters(text, '~') > 6 ||
                StringUtil.countCharacters(text, 'W') > 6) {
                text = "DISREGARD THAT I SUCK COCK";
        }
        boolean commandWorked = false;
            // @ for players, and ! allowed if isGM(), allow SKIP if a player goes "!!" or "@@"++ for fun. :)
            if ((text.startsWith("@")) || (text.startsWith("!") && c.getPlayer().isGM())) { // this way players can use ! and not access commands. :)
                boolean allowed = true;
                String[] args = text.split(" ");
                int domain;
                String commandType;
                if (args[0].charAt(0) == '@') { // player
                    domain = PlayerGMRank.NORMAL.getLevel();
                    commandType = "player";
                } else if (args[0].charAt(0) == '!') { // donor and above = ! 
                    domain = PlayerGMRank.DONATOR.getLevel();
                    commandType = "donor";
                    if (c.getPlayer().getGMLevel() < 1) { // this isn't fucking points omfg.
                        allowed = false;
                    }
                } else if (args[0].charAt(0) == '!') {
                    domain = PlayerGMRank.SUPERDONATOR.getLevel();
                    commandType = "sdonor";
                    if (c.getPlayer().getGMLevel() < 2) { // ./rage -.-
                        allowed = false;
                    }
                } else if (args[0].charAt(0) == '!') { // intern = 3, right? o-o
                    domain = PlayerGMRank.INTERN.getLevel();
                    commandType = "intern";
                    if (c.getPlayer().getGMLevel() < 3) {
                        allowed = false;
                    }
                } else if (args[0].charAt(0) == '!' && c.getPlayer().getGMLevel() >= 4) { // GM = 4? i think so anyways lol
                    domain = PlayerGMRank.GM.getLevel();
                    commandType = "gm";
                    if (c.getPlayer().getGMLevel() < 4) {
                        allowed = false;
                    }
                } else { //impossible but just in case
                    return;
                }
                //String[] commandTypes = {"player", "donor", "sdonor", "intern", "gm"};
                //String prefix = "@#$%!";
                //int domain = prefix.indexOf(args[0].substring(0, 1));
                String name = args[0].replace(args[0].substring(0, 1), "");
                //String commandType = commandTypes[domain];
                if (!c.getPlayer().hasGmLevel(domain)) {
                    //chr.showMessage("You do not have the privileges to use that command.");
                    //return;
                    allowed = false;
                }
                if (allowed) {
                    c.getPlayer().setCommandArgs(args);
                    Invocable iv = AbstractCommandScriptManager.getInvocableCommand(commandType, name, c);
                    final ScriptEngine scriptengine = (ScriptEngine) iv;
                    final AbstractCommandScriptManager acm = new AbstractCommandScriptManager();
                    try {
                        if (iv != null) {
                            acm.putCms(c, acm);
                            scriptengine.put("c", c);
                            scriptengine.put("acm", acm);
                            scriptengine.put("args", args);
                            iv.invokeFunction("start");
                            commandWorked = true;
                        } else {
                            commandWorked = false;
                            iv = AbstractCommandScriptManager.getInvocableCommand(commandType, "nocommand", c); //safe disposal

                        }
                    } catch (final Exception e) {
                        System.err.println("Error executing Command script, Command : " + name + "." + e);
                    } finally {
                        acm.dispose(c, commandType, name);
                    }
                }
            }
            if (!commandWorked) {
                switch (text.charAt(0)) {
                    case '@':
                        PlayerCommand.executePlayerCommands(c, text.split(" "));
                        commandWorked = true;
                        break;
                    case '!': // should make Donor+ all ! o-o
                        DonatorCommand.executeDonatorCommand(c, text.split(" "));
                        commandWorked = true;
                        break;
                    default:
                        commandWorked = false;
                        break;
                }
            }
            if (!commandWorked) {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer(), text)); // TODO: custom gm text
            }
    }
}