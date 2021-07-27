package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.MaplePacketCreator;

public class MapleMapEffect {
	private String msg;
	private int itemId;
	private boolean active = true;
        private boolean jukebox = false;
        private boolean kite = false;
        private MapleCharacter kiteCarrier;
		
	public MapleMapEffect(String msg, int itemId) {
            this.msg = msg;
            this.itemId = itemId;
	}
	
	public void setActive(boolean active){
            this.active = active;
	}
        
        public void setCarrier(MapleCharacter carrier){
            this.kiteCarrier = carrier;
	}
        
        public MapleCharacter getCarrier() {
            return this.kiteCarrier;
        }
        
        public void setJukebox(boolean actie) {
            this.jukebox = actie;
        }
        
        public boolean isJukebox() {
            return this.jukebox;
        }
        
        public void setKite(boolean actie) {
            this.kite = actie;
        }
        
        public boolean isKite() {
            return this.kite;
        }
	
	public MaplePacket makeDestroyData() {
            return kite ? MaplePacketCreator.removeKiteEffect(getCarrier().getId()) : jukebox ? MaplePacketCreator.startJukebox(0, "") : MaplePacketCreator.removeMapEffect();
	}
	
	public MaplePacket makeStartData() {
            return kite ? MaplePacketCreator.startKiteEffect(getCarrier(), itemId, msg) : jukebox ? MaplePacketCreator.startJukebox(itemId, msg) : MaplePacketCreator.startMapEffect(msg, itemId, active);
	}
	
	public void sendStartData(MapleClient client) {
            client.getSession().write(makeStartData());
	}
}
