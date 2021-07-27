package net.sf.odinms.net.channel.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseChairHandler extends AbstractMaplePacketHandler {
	private static Logger log = LoggerFactory.getLogger(UseItemHandler.class);

	public UseChairHandler() {
	}

        @Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		short chair = slea.readShort();
		c.getPlayer().setChair(chair);
                c.getSession().write(MaplePacketCreator.showChair(chair)); // v40 has no chairs, this is client-sided too
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showChair(chair), false);
		c.getSession().write(MaplePacketCreator.enableActions());
	}
}