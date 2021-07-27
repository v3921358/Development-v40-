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

package net.sf.odinms.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import net.sf.odinms.constants.ServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Patrick Huy
 * @revisor Eric
 */

public class DatabaseConnection {
    private static ThreadLocal<Connection> con = new ThreadLocalConnection();
    private final static Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    public static Connection getConnection() {
        return con.get();
    }

    public static void closeAll() throws SQLException {
        for (Connection con : ThreadLocalConnection.allConnections) {
            con.close();
        }
    }

    private static class ThreadLocalConnection extends ThreadLocal<Connection> {
        public static Collection<Connection> allConnections = new LinkedList<Connection>();

        @Override
        protected Connection initialValue() {
            try {
                Class.forName("com.mysql.jdbc.Driver"); // touch the mysql driver
            } catch (ClassNotFoundException e) {
                log.error("ERROR", e);
            }
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + ServerConstants.DB_SCHEMA, ServerConstants.DB_USER, ServerConstants.DB_PASS);
                allConnections.add(con);
                return con;
            } catch (SQLException e) {
                log.error("ERROR", e);
                return null;
            }
        }
    }
}
