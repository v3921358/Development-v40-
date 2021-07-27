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

/*
Edited by :
Junxian of ZanoMS/JunMS
POkkax of RageZone 
Pokkax of OdinMS


Credits:   [GM]Fatality of FusionMS
	   (crushing2 of Ragezone)
*/
function start() {
	cm.sendSimple ("Hello, please choose your category.\r\n#L0#Capes#l\r\n#L1#Faces#l\r\n#L2#Eye Accessories#l\r\n#L3#Face Accessories#l\r\n#L4#Gloves#l\r\n#L5#Hats#l\r\n#L6#Hats 2#l\r\n#L7#Overall#l\r\n#L8#Tops#l\r\n#L9#Pants#l\r\n#L10#Rings#l\r\n#L11#Shields#l\r\n#L12#Shoes#l\r\n#L13#Weapons#l");
}

function action(mode, type, selection) {
	cm.dispose();
	if (selection == 0) {
		cm.openShop (100);
	} else if (selection == 1) {
		cm.openShop (101);
	} else if (selection == 2) {
		cm.openShop (102);
	} else if (selection == 3) {
		cm.openShop (103);
	} else if (selection == 4) {
		cm.openShop (104);
	} else if (selection == 5) {
		cm.openShop (105);
	} else if (selection == 6) {
		cm.openShop (106);
	} else if (selection == 7) {
		cm.openShop (107);
	} else if (selection == 8) {
		cm.openShop (108);
	} else if (selection == 9) {
		cm.openShop (109);
	} else if (selection == 10) {
		cm.openShop (110);
	} else if (selection == 11) {
		cm.openShop (111);
	} else if (selection == 12) {
		cm.openShop (112);
	} else if (selection == 13) {
		cm.openShop (113);
	} else {
		cm.dispose();
	}
}