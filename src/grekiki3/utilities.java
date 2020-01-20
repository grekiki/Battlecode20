package grekiki3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
	public static int pkey = 1234;// Privatni kljuc za blockchain
	public static int inf = Integer.MAX_VALUE / 2;
}

/**
 * Razred z raznimi pomoznimi funkcijami.
 * 
 * @author grego
 *
 */
class Util {
	public static Random r = new Random(c.pkey);
	public static Direction[] dir = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
			Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };

	/**
	 * Returns the round elevation gets flooded.
	 * 
	 * @param elevation
	 * @return
	 */
	int roundFlooded(int elevation) {
		switch (elevation) {
		case 0:
			return 0;
		case 1:
			return 256;
		case 2:
			return 464;
		case 3:
			return 677;
		case 4:
			return 931;
		case 5:
			return 1210;
		case 6:
			return 1413;
		case 7:
			return 1546;
		case 8:
			return 1640;
		case 9:
			return 1713;
		case 10:
			return 1771;
		case 11:
			return 1819;
		case 12:
			return 1861;
		case 13:
			return 1893;
		case 14:
			return 1929;
		case 15:
			return 1957;
		case 16:
			return 1983;
		case 17:
			return 2007;
		case 18:
			return 2028;
		case 19:
			return 2048;
		case 20:
			return 2067;
		case 21:
			return 2084;
		case 22:
			return 2100;
		case 23:
			return 2115;
		case 24:
			return 5459;
		case 25:
			return 2143;
		case 26:
			return 2155;
		case 27:
			return 2168;
		case 28:
			return 2179;
		case 29:
			return 2190;
		default:
			return 2201;
		}
	}

	public Direction getRandomDirection() {
		return dir[(int) Math.floor(dir.length * Math.random())];
	}

	public static int d_inf(MapLocation l1, MapLocation l2) {// d neskoncno metrika
		return Math.max(Math.abs(l2.x - l1.x), Math.abs(l2.y - l1.y));
	}

	public static Direction rotateLeft(Direction aim, int lturns) {
		if (lturns == 0) {
			return aim;
		}
		if (lturns > 0) {
			Direction a2 = aim;
			for (int q = 0; q < lturns; q++) {
				a2 = a2.rotateLeft();
			}
			return a2;
		} else {
			Direction a2 = aim;
			for (int q = 0; q < -lturns; q++) {
				a2 = a2.rotateRight();
			}
			return a2;
		}
	}

	public static MapLocation closest(MapLocation[] q, MapLocation source) {
		int closest = c.inf;
		int i = q.length;
		MapLocation ans = null;
		while (i-- > 0) {
			int d2 = source.distanceSquaredTo(q[i]);
			if (d2 < closest) {
				closest = d2;
				ans = q[i];
			}
		}
//		if (ans == null) {
//			System.out.println("Nabljizja lokacija iz prazne mnozice?");
//		}
		return ans;
	}

	public static MapLocation closest(Iterable<? extends MapLocation> somethings, MapLocation source)
			throws GameActionException {
		MapLocation ans = null;
		int dist = c.inf;
		for (MapLocation m : somethings) {
			int d2 = source.distanceSquaredTo(m);
			if (d2 < dist) {
				dist = d2;
				ans = m;
			}
		}
//		if(ans==null){
//			System.out.println("Nabljizja lokacija iz prazne mnozice?");
//		}
		return ans;
	}

	public static MapLocation randomPoint(int h, int w) {
		return new MapLocation(r.nextInt(w), r.nextInt(h));
	}

	public static MapLocation closest(vector_set_gl q, MapLocation source) {
		int closest = c.inf;
		int i = q.size;
		MapLocation ans = null;
		while (i-- > 0) {
//			System.out.println(i+" "+q.size+" "+q.get(i)+" "+Arrays.toString(q.q));
			int d2 = source.distanceSquaredTo(q.get(i));
			if (d2 < closest) {
				closest = d2;
				ans = q.get(i);
			}
		}
//		if (ans == null) {
//			System.out.println("Nabljizja lokacija iz prazne mnozice?");
//		}
		return ans;
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
 * prej�nji blok ni poln, potem placamo 1. Druga�e matchamo najcenejsi paket
 * v prejsnjem bloku.
 * 
 * @author grego
 *
 */
class blockchain {
	final int PRIVATE_KEY = 123456789;
	/**
	 * Na tem mestu je dosegljivo surovinsko polje
	 */
	final int LOC_SUROVINA = 1;
	/**
	 * Tukaj surovine ni vec
	 */
	final int LOC_SUROVINA_PRAZNO = 2;
	/**
	 * Tukaj je surovinsko polje, ki pa morda ni dosegljivo
	 */
	final int LOC_SLABA_SUROVINA = 3;
	/**
	 * Tukaj je surovinsko polje ki ni bilo dosegljivo, sedaj pa je
	 */
	final int NADGRADNJA_SUROVINE = 4;
	/**
	 * Tukaj je refinerija
	 */
	final int LOC_RAFINERIJA = 10;
	/**
	 * Tukaj je tovarna dronov
	 */
	final int LOC_TOVARNA_DRONOV = 20;
	/**
	 * Lokacija domace baze
	 */
	final int LOC_HOME_HQ = 30;
	/**
	 * Enota zeli prevoz z dronom iz prve do druge lokacije
	 */
	final int LOC2_DRONE = 100;

	private final int LOC_MAX = 99;// do 99 je en mapLocation
	private final int LOC2_MAX = 999;// od 100 do 999 sta 2

	List<paket> messages; // TODO lahko bi uporabili Heap (prioritetna vrsta glede na ceno)
	RobotController rc;

	int rounds_read = 0; // Koliko rund smo ze prebrali?

	blockchain(RobotController rc) {
		this.rc = rc;
		messages = new ArrayList<paket>();
	}

	public void handle_location(int type, MapLocation pos) {
	}

	public void handle_location2(int type, MapLocation m1, MapLocation m2) {
	}

	public void handle_packet(int type, int[] message) {
	}

	// VSAKIC KO POSLJEMO SPOROCILO, JE POTREBNO KLICATI checkQueue
	public void send_location(int type, MapLocation pos) throws GameActionException {
		int[] msg = { PRIVATE_KEY, type, pos.x, pos.y, 0, 0, 0 };
		sendMsg(new paket(msg, 1));
	}

	public void send_location2(int type, MapLocation p1, MapLocation p2) throws GameActionException {
		int[] msg = { PRIVATE_KEY, type, p1.x, p1.y, p2.x, p2.y, 0 };
		sendMsg(new paket(msg, 1));
	}

	public boolean check_private_key(int[] msg) {
		// TODO bolje lahko sifriramo nasa sporocila.
		return (msg[0] == PRIVATE_KEY);
	}

	public void parse_transaction(int[] msg) {
		if (!check_private_key(msg))
			return;

		int type = msg[1];
		if (type >= LOC2_MAX) {
			handle_packet(type, msg);
		} else if (type >= LOC_MAX) {
			MapLocation m1 = new MapLocation(msg[2], msg[3]);
			MapLocation m2 = new MapLocation(msg[4], msg[5]);
			handle_location2(LOC2_DRONE, m1, m2);
		} else {
			MapLocation m = new MapLocation(msg[2], msg[3]);
			handle_location(type, m);
		}
	}

	public void read_round(int round) throws GameActionException {
		Transaction[] transactions = rc.getBlock(round);
		for (Transaction tr : transactions) {
			int[] msg = tr.getMessage();
			parse_transaction(msg);
		}
	}

	/**
	 * Prebere naslednjo rundo ali pove da je ze bila prebrana
	 * 
	 * @return
	 * @throws GameActionException
	 */
	public boolean read_next_round() throws GameActionException {
		// Trenutne runde ne moremo prebrati ...
		if (rounds_read < rc.getRoundNum() - 1) {
			rounds_read++;
			read_round(rounds_read);
			return true;
		} else {
			return false;
		}
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

	// TO-DO preveri ce se splaca optimizirati. Vrsta z math.min je prepogosta :)
	public void checkQueue() throws GameActionException {
		int minCost = c.inf;
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
				// System.out.println("Poslano");
				i--;
			}
		}
		// System.out.println(messages.size() + " paketov caka");
	}
}