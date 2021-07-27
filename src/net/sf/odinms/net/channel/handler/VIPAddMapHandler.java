package net.sf.odinms.net.channel.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author XoticMS
 */
public class VIPAddMapHandler extends AbstractMaplePacketHandler {

    private static Logger log = LoggerFactory.getLogger(VIPAddMapHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        
        Connection con = DatabaseConnection.getConnection();
        int operation = slea.readByte();
        MapleCharacter player = c.getPlayer();

        switch (operation) {
            case 0: // Remove map
                int mapid = slea.readInt();
                try {
                    PreparedStatement ps = con.prepareStatement("DELETE FROM trocklocations WHERE cid = ? AND mapid = ?");
                    ps.setInt(1, player.getId());
                    ps.setInt(2, mapid);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException lawl) {
                }
                break;
            case 1: // Add map
                try {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO trocklocations (`cid`, `mapid`) VALUES (?, ?)");
                    ps.setInt(1, player.getId());
                    ps.setInt(2, player.getMapId());
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException lawl) {
                }
                break;
            default:
                log.info("Unhandled VIP Rock operation: " + slea.toString());
                return;
        }
        c.getSession().write(MaplePacketCreator.refreshVIPRockMapList(player.getVIPRockMaps(), operation == 1 ? (byte)0x03 : 0x02));
    }
}