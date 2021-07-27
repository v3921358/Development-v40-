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

package net.sf.odinms.net.world;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Matze
 */
public class WorldServer {

	private static WorldServer instance = null;
	private static Logger log = LoggerFactory.getLogger(WorldServer.class);
	private int worldId;
	private Properties worldProp = new Properties();
	
	private WorldServer() {
	}
	
	public synchronized static WorldServer getInstance() {
            if (instance == null) instance = new WorldServer();
            return instance;
	}

	public int getWorldId() {
            return worldId;
	}

	public Properties getWorldProp() {
            return worldProp;
	}
	
	public static void main(String[] args) {
		try {
                    Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
                    registry.rebind("WorldRegistry", WorldRegistryImpl.getInstance());
		} catch (RemoteException ex) {
                    log.error("Could not initialize RMI system", ex);
		}
	}
}
