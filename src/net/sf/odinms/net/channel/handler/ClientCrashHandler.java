/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Eric
 */
public class ClientCrashHandler extends AbstractMaplePacketHandler {
    private static Logger log = LoggerFactory.getLogger(ClientCrashHandler.class);
    
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        log.info("\nCLIENT CRASHED WITH : " + slea.toString() + "\n");
    }
}
