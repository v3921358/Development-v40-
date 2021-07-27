/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import java.sql.*;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Eric
 */
public class ReportHandler extends AbstractMaplePacketHandler {
    
    final String[] reasons = {
        "Hacking",
        "Botting",
        "Scamming",
        "Fake GM",
        "Harassment",
        "Advertising"
    };

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int reportedCharId = slea.readInt();
        byte reason = slea.readByte();
        String chatlog = "No chatlog";
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info(c.getPlayer().getName() + " reported charid " + reportedCharId);
        int cid = reportedCharId;
        if (addReportEntry(c.getPlayer().getId(), reportedCharId, reason, chatlog)) {
            c.getSession().write(MaplePacketCreator.reportReply((byte) 0));
        } else {
            c.getSession().write(MaplePacketCreator.reportReply((byte) 4));
        }
            for (ChannelServer ch : ChannelServer.getAllInstances()) // all or just current..?
                for (MapleCharacter chrs : ch.getPlayerStorage().getAllCharacters())
                    if (chrs.isGM())
                        chrs.dropMessage(5, c.getPlayer().getName() + " reported " + MapleCharacter.getNameById(cid, 0) + " for " + reasons[reason] + ".");
    }

    private boolean addReportEntry(int reporterId, int victimId, byte reason, String chatlog) {
        try {
            Connection dcon = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = dcon.prepareStatement("INSERT INTO reports VALUES (NULL, CURRENT_TIMESTAMP, ?, ?, ?, ?, 'UNHANDLED')");
            ps.setInt(1, reporterId);
            ps.setInt(2, victimId);
            ps.setInt(3, reason);
            ps.setString(4, chatlog);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            return false;
        }
        return true;
    }
}