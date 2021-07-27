package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.constants.ServerConstants.CommandType;

/**
 * Interface for the executable part of a {@link CommandObject}.
 *
 * @author Emilyx3
 */
public abstract class CommandExecute {

    /**
     * The method executed when this command is used.
     *
     * @param c the client executing this command
     * @param splitted the command and any arguments attached
     *
     * @return 1 if you want to log the command, 0 if not. TODO: USE {@link #ReturnValue}
     */
    public abstract int execute(MapleClient c, String[] splitted);
    //1 = Success
    //0 = Something Went Wrong

    enum ReturnValue {
        DONT_LOG,
        LOG;
    }

    public CommandType getType() {
        return CommandType.NORMAL;
    }
}
