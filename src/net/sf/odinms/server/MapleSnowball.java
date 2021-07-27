package net.sf.odinms.server;

import java.awt.Point;

/**
 *
 * @author Bassoe
 * Edited by acEvolution to suit his needs :P
 */

// TODO : Award points when snowman is hit once. 50 points enables a team to sabotage the other by stunning the whole team.
public class MapleSnowball {
    
    private Point position;
    private int team;
    public static boolean isInvis0 = false, isInvis1 = false;

    public MapleSnowball(int team_) {
        this.team = team_;
        switch (team_) {
            case 0:
                this.position = new Point(0, 155);
                break;
            case 1:
                this.position = new Point(0, -84);
                break;
            default:
                this.position = new Point(0, 0);
                break;
        }
    }

    public int getTeam() {
        return team;
    }


    public Point getPosition() {
        return position;
    }

    
    public void setPositionX(int pos) {
        this.position.x = pos;    
    }
}