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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.odinms.net.world.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.CharacterIdChannelPair;
import net.sf.odinms.net.world.MapleMessenger;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.PlayerBuffValueHolder;
import net.sf.odinms.net.world.PlayerCoolDownValueHolder;

/**
 *
 * @author Matze
 */
public interface WorldChannelInterface extends Remote, WorldChannelCommonOperations {
	public void serverReady() throws RemoteException;
	public String getIP(int channel) throws RemoteException;

	public int find(String charName) throws RemoteException;
	public int find(int characterId) throws RemoteException;
	public Map<Integer, Integer> getConnected() throws RemoteException;
	
	MapleParty createParty (MaplePartyCharacter chrfor) throws RemoteException;
	MapleParty getParty(int partyid) throws RemoteException;
	public void updateParty (int partyid, PartyOperation operation, MaplePartyCharacter target) throws RemoteException;
	public void partyChat(int partyid, String chattext, String namefrom) throws RemoteException;
	
	public boolean isAvailable() throws RemoteException;
	public ChannelWorldInterface getChannelInterface(int channel) throws RemoteException;
	
	public WorldLocation getLocation(String name) throws RemoteException;
	public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int [] characterIds) throws RemoteException;
        
        MapleMessenger createMessenger(MapleMessengerCharacter chrfor) throws RemoteException;
        MapleMessenger getMessenger(int messengerid) throws RemoteException;
        public void leaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException;
        public void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromchannel) throws RemoteException;
        public void silentJoinMessenger(int messengerid, MapleMessengerCharacter target, int position) throws RemoteException;
        public void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException;
        public void messengerChat(int messengerid, String chattext, String namefrom) throws RemoteException;
        public void declineChat(String target, String namefrom) throws RemoteException;
        public void updateMessenger(int messengerid, String namefrom, int fromchannel) throws RemoteException;
        public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException;

        public void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) throws RemoteException;
        public List<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) throws RemoteException;
        public void addCooldownsToStorage(int chrid, List<PlayerCoolDownValueHolder> toStore) throws RemoteException;
        public List<PlayerCoolDownValueHolder> getCooldownsFromStorage(int chrid) throws RemoteException;
}
