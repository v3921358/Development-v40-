/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Eric
 */
public class StrangeBossHandler extends AbstractMaplePacketHandler {
    
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        // Something to do with bosses, but to avoid bat spam for unhandled packets..
    }
}
