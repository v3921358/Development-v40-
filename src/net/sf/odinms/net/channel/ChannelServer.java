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

package net.sf.odinms.net.channel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.MapleServerHandler;
import net.sf.odinms.net.PacketProcessor;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.mina.MapleCodecFactory;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.net.world.remote.WorldRegistry;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.scripting.event.EventScriptManager;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.ShutdownServer;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.MapleMapFactory;
import net.sf.odinms.tools.MaplePacketCreator;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelServer implements Runnable, ChannelServerMBean {
        private int channel; // this is our new unique key, fuck odin's "channel key"
    
	private static int uniqueID = 1;
	private int port;
	private static final Logger log = LoggerFactory.getLogger(ChannelServer.class);
	private static WorldRegistry worldRegistry;
	private PlayerStorage players = new PlayerStorage();
	private ChannelWorldInterface cwi ;
	private WorldChannelInterface wci = null;
	private IoAcceptor acceptor;
	private String ip;
	private boolean shutdown = false;
	private boolean finishedShutdown = false;

	private MapleMapFactory mapFactory;
	private EventScriptManager eventSM;
        private int eventMap = -1;
	private static Map<Integer, ChannelServer> instances = new HashMap<Integer, ChannelServer>();
	
	private Boolean worldReady = true;

	private ChannelServer(int channel) {
		mapFactory = new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Map.wz")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")));
		this.channel = channel;
	}
	
	public static WorldRegistry getWorldRegistry() {
		return worldRegistry;
	}
	
	public void reconnectWorld() {
		// check if the connection is really gone
		try {
			wci.isAvailable();
		} catch (RemoteException ex) {
			synchronized (worldReady) {
				worldReady = false;
			}
			synchronized (cwi) {
				synchronized (worldReady) {
					if (worldReady) return;
				}
				log.warn("Reconnecting to world server");
				synchronized (wci) {
					// completely re-establish the rmi connection
					try {
						Registry registry = LocateRegistry.getRegistry(ServerConstants.ip, Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
						worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
						cwi = new ChannelWorldInterfaceImpl(this);
						wci = worldRegistry.registerChannelServer(channel, cwi);
						DatabaseConnection.getConnection();
						wci.serverReady();
					} catch (Exception e) {
						log.error("Reconnecting failed", e);
					}
					worldReady = true;
				}
			}
			synchronized (worldReady) {
				worldReady.notifyAll();
			}
		}
	}

	@Override
	public void run() {
		try {
                    cwi = new ChannelWorldInterfaceImpl(this);
                    wci = worldRegistry.registerChannelServer(channel, cwi);
                    eventSM = new EventScriptManager(this, ServerConstants.Events.split(","));
                    DatabaseConnection.getConnection();
		} catch (Exception e) {
                    throw new RuntimeException(e);
		}
		
		port = ((ServerConstants.PORT - 1) + channel);
		ip = ServerConstants.ip + ":" + port;
		
		ByteBuffer.setUseDirectBuffers(false);
		ByteBuffer.setAllocator(new SimpleByteBufferAllocator());

		acceptor = new SocketAcceptor();

		SocketAcceptorConfig cfg = new SocketAcceptorConfig();
		cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
		TimerManager tMan = TimerManager.getInstance();
		tMan.start();
		tMan.register(AutobanManager.getInstance(), 60000);

		try {
                    MapleServerHandler serverHandler = new MapleServerHandler(PacketProcessor.getProcessor(PacketProcessor.Mode.CHANNELSERVER), channel);
                    acceptor.bind(new InetSocketAddress(port), serverHandler, cfg);
                    wci.serverReady();
                    eventSM.init();
		} catch (IOException e) {
                    System.out.println("Binding to port " + port + " failed (ch: " + getChannel() + ") | Error: " + e);
		}
	}

	public void shutdown() {
		// dc all clients by hand so we get sessionClosed...
		shutdown = true;
		List<CloseFuture> futures = new LinkedList<CloseFuture>();
		Collection<MapleCharacter> allchars = players.getAllCharacters();
		MapleCharacter chrs[] = allchars.toArray(new MapleCharacter[allchars.size()]);
		for (MapleCharacter chr : chrs) {
			if (chr.getTrade() != null) {
				MapleTrade.cancelTrade(chr);
			}
			if (chr.getEventInstance() != null) {
				chr.getEventInstance().playerDisconnected(chr);
			}
			chr.saveToDB(true);
			if (chr.getCheatTracker() != null)
				chr.getCheatTracker().dispose();
			removePlayer(chr);
		}
		for (MapleCharacter chr : chrs) {
			futures.add(chr.getClient().getSession().close());
		}
		for (CloseFuture future : futures) {
			future.join(500);
		}
		finishedShutdown = true;
		
		wci = null;
		cwi = null;
	}
	
	public void unbind() {
		acceptor.unbindAll();
	}
	
	public boolean hasFinishedShutdown() {
		return finishedShutdown;
	}

	public MapleMapFactory getMapFactory() {
		return mapFactory;
	}
	
	public static ChannelServer newInstance(int channel) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
		ChannelServer instance = new ChannelServer(channel);
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		mBeanServer.registerMBean(instance, new ObjectName("net.sf.odinms.net.channel:type=ChannelServer,name=ChannelServer" + uniqueID++));
		instances.put(channel, instance);
		return instance;
	}

	public static ChannelServer getInstance(int channel) {
		return instances.get(channel);
	}

	public void addPlayer(MapleCharacter chr) {
		players.registerPlayer(chr);
	}

	public IPlayerStorage getPlayerStorage() {
		return players;
	}

	public void removePlayer(MapleCharacter chr) {
		players.deregisterPlayer(chr);
	}

	public int getConnectedClients() {
		return players.getAllCharacters().size();
	}

	public void setServerMessage(String newMessage) {
		ServerConstants.SERVER_MESSAGE = newMessage;
		broadcastPacket(MaplePacketCreator.serverMessage(newMessage));
	}

	public void broadcastPacket(MaplePacket data) {
		for (MapleCharacter chr : players.getAllCharacters()) {
                    chr.getClient().getSession().write(data);
		}
	}
        
        public void broadcastGMPacket(MaplePacket data) {
            for (MapleCharacter chr : players.getAllCharacters()) {
                if (chr.isGM()) {
                    chr.getClient().getSession().write(data);
                }
            }
        }
        
        public List<MapleCharacter> getPartyMembers(MapleParty party) {
            List<MapleCharacter> partym = new LinkedList<MapleCharacter>();
            for (net.sf.odinms.net.world.MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getChannel() == getChannel()) { // Make sure the thing doesn't get duplicate plays due to ccing bug.
                    MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null) {
                        partym.add(chr);
                    }
                }
            }
            return partym;
        }
	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		if (instances.containsKey(channel))
			instances.remove(channel);
		instances.put(channel, this);
		this.channel = channel;
		this.mapFactory.setChannel(channel);
	}
	
	public static Collection<ChannelServer> getAllInstances() {
		return Collections.unmodifiableCollection(instances.values());
	}
	
	public String getIP() {
		return ip;
	}
	
	public String getIP(int channel) {
		try {
			return getWorldInterface().getIP(channel);
		} catch (RemoteException e) {
			log.error("Lost connection to world server", e);
			throw new RuntimeException("Lost connection to world server");
		}
	}
	
	public WorldChannelInterface getWorldInterface() {
		synchronized (worldReady) {
			while (!worldReady) {
				try {
					worldReady.wait();
				} catch (InterruptedException e) {}
			}
		}
		return wci;
	}

	public boolean isShutdown() {
		return shutdown;
	}
	
	@Override
	public void shutdown(int time) {
		broadcastPacket(MaplePacketCreator.serverNotice(0, "The world will be shut down in " + (time / 60000) +	" minutes, please log off safely"));
		TimerManager.getInstance().schedule(new ShutdownServer(getChannel()), time);
	}
	
	@Override
	public void shutdownWorld(int time) {
		try {
			getWorldInterface().shutdown(time);
		} catch (RemoteException e) {
			reconnectWorld();
		}
	}
	
	public int getLoadedMaps() {
		return mapFactory.getLoadedMaps();
	}

	public EventScriptManager getEventSM() {
		return eventSM;
	}
	
	public void reloadEvents() {
		eventSM.cancel();
		eventSM = new EventScriptManager(this, ServerConstants.Events.split(","));
		eventSM.init();
	}
        
        public int getEventMap() {
            return eventMap;
        }
        
        public void setEventMap(int map) {
            this.eventMap = map;
        }
	
	public static void main(String args[]) throws FileNotFoundException, IOException, NotBoundException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
		Registry registry = LocateRegistry.getRegistry(ServerConstants.ip, Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
		worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
		for (int i = 1; i < (ServerConstants.Channels + 1); i++) { // increment from 1.. i guess.
			newInstance(i).run();
		}
		DatabaseConnection.getConnection(); // touch - so we see database problems early...
	}
}