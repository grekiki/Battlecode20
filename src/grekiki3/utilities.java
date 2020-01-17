package grekiki3;

import java.util.ArrayList;
import java.util.Arrays;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Transaction;

/**
 * Razred z konstantami. Ima krajse ime ker bo morda pogosto uporabljen
 * 
 * @author gregor
 *
 */
class c {
	public static int pkey = 1234;// Privatni klju� za blockchain

}

/**
 * Razred z raznimi pomo�nimi funkcijami.
 * 
 * @author grego
 *
 */
class Util {
	public static Direction[] dir = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
			Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };
	public static int inf = Integer.MAX_VALUE / 2;

	public static Direction getRandomDirection() {
		return dir[(int) Math.floor(dir.length * Math.random())];
	}

	public static int d_inf(MapLocation l1, MapLocation l2) {// d neskoncno metrika
		return Math.max(Math.abs(l2.x - l1.x), Math.abs(l2.y - l1.y));
	}
}

/**
 * Ta razred vsebuje paket ki bi ga radi spravili v blockchain
 * 
 * @author gregor
 *
 */
class paket {
	int[] data;
	int cost;

	paket(int[] a, int b) {
		data = a;
		cost = b;
	}
}

/**
 * Ta razred bo skrbel da se vsi paketi ki jih �elimo poslati, dejansko
 * po�ljejo. Vsako potezo naj bi se poklical blockchain.checkQueue() da se
 * znebimo paketov iz seznama. Razred bo preveril cene v prej�njem bloku, in
 * glede na le te dolo�il koliko pla�ati da pridemo v naslednji blok. �e
 * prej�nji blok ni poln, potem placamo 1. Druga�e matchamo najcenejsi paket v
 * prejsnjem bloku.
 * 
 * @author grego
 *
 */
class blockchain {
	ArrayList<paket> messages;
	RobotController rc;

	blockchain(RobotController rc) {
		this.rc = rc;
		messages = new ArrayList<paket>();
	}

	public void sendMsg(paket p) throws GameActionException {
		if (rc.canSubmitTransaction(p.data, p.cost)) {
			rc.submitTransaction(p.data, p.cost);
//			System.out.println(rc.getLocation() + " je poslal posto");
			messages.add(new paket(p.data, p.cost));// Paket dodamo v sseznam ker ne vemo �e je dejansko bil poslan
		} else {
			messages.add(new paket(p.data, 1));// ok ta gotovo ni bil poslan
		}
	}
	
	//TO-DO preveri ce se splaca optimizirati. Vrsta z math.min je prepogosta :)
	public void checkQueue() throws GameActionException {
		int minCost = Util.inf;
		Transaction[] t = rc.getBlock(rc.getRoundNum() - 1);
		if (t.length != 7) {
			minCost = 1;
		} else {
			for (int i = 0; i < messages.size(); i++) {
				paket p = messages.get(i);
				for (Transaction tt : t) {
					// Za vsak paket pogledamo ce je prisel na vrsto
					if (Arrays.equals(tt.getMessage(), p.data)) {
						messages.remove(p);// ce je ga odstranimo
						i--;
						break;
					}
					minCost = Math.min(minCost, tt.getCost() + 1);// hkrati si belezimo ceno najcenejsega paketa
				}
			}
		}
		for (int i = 0; i < messages.size(); i++) {// Pakete posljemo znova,
			paket p = messages.get(i);
			if (p.cost >= minCost) {// Placali smo ze dovolj (to se ne bi smelo zgoditi?)
				continue;
			}
			int[] msgg = p.data;
			if (rc.canSubmitTransaction(msgg, minCost)) {
				rc.submitTransaction(msgg, minCost);
				p.cost = minCost;
//				System.out.println("Poslano");
				i--;
			}
		}
//		System.out.println(msg.size() + " paketov caka");
	}

}