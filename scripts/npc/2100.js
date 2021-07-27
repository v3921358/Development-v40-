/**
 * @author: Eric
 * @npc: Heena
 * @func: Server Welcome NPC
 * @todo: Update NPC structuring for optional use of the action function
*/

function start() {
	cm.sendOk("Welcome to #eDevelopment#n v40 Beta Edition!");
}

function action(mode, type, selection) {
	cm.dispose();
}