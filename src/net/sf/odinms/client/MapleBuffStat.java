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

package net.sf.odinms.client;

import net.sf.odinms.net.LongValueHolder;

public enum MapleBuffStat implements LongValueHolder {
	WATK(0x00000001),
	WDEF(0x00000002),
	MATK(0x00000004),
	MDEF(0x00000008),
	ACC(0x00000010),
	AVOID(0x00000020),
	HANDS(0x00000040),
	SPEED(0x00000080),
	JUMP(0x00000100),
	MAGIC_GUARD(0x00000200),
	DARKSIGHT(0x00000400), // also used by gm hide
	BOOSTER(0x00000800),
	POWERGUARD(0x00001000),
	HYPERBODYHP(0x00002000),
	HYPERBODYMP(0x00004000),
	INVINCIBLE(0x00008000),
	SOULARROW(0x00010000),
	
	SUMMON(0x200000), //hack buffstat for summons ^.- (does/should not increase damage... hopefully <3)
	HOLY_SYMBOL(0x01000000),
	MESOUP(0x2000000),
	SHADOWPARTNER(0x04000000),
	PICKPOCKET(0x08000000),
	PUPPET(0x8000000), // HACK - shares buffmask with pickpocket - odin special ^.-
	MESOGUARD(0x10000000),
	RECOVERY(0x400000000l),
	STANCE(0x1000000000l),
	SHARP_EYES(0x2000000000l),
	MANA_REFLECTION(0x4000000000l),
	MAPLE_WARRIOR(0x800000000l),
	SHADOW_CLAW(0x10000000000l),
	INFINITY(0x20000000000l), 
	HOLY_SHIELD(0x40000000000l),
	HAMSTRING(0x80000000000l),
	BLIND(0x100000000000l),
	CONCENTRATE(0x200000000000l), // another no op buff
	MONSTER_RIDING(0x400000000000l),
        
        STUN(0x00020000),
        POISON(0x00040000),
        SEAL(0x00080000),
        DARKNESS(0x00100000),
        WEAKNESS(0x40000000),
        COMBO(0x00200000),
        WK_CHARGE(0x00400000),
        DRAGONBLOOD(0x00800000), // another funny buffstat...
        ;
	private final long i;

	private MapleBuffStat(long i) {
		this.i = i;
	}

	@Override
	public long getValue() {
		return i;
	}
}
