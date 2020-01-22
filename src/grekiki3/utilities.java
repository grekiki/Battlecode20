package grekiki3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import battlecode.common.*;

/**
 * Razred z konstantami. Ima krajse ime ker bo morda pogosto uporabljen
 * 
 * @author gregor
 *
 */
class c {
	public static int inf = Integer.MAX_VALUE / 2;
}

/**
 * Razred z raznimi pomoznimi funkcijami.
 * 
 * @author grego
 *
 */
class Util {
	public static Random r = new Random(1234);
	public static Direction[] dir = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };

	/**
	 * Returns the round elevation gets flooded.
	 * 
	 * @param elevation
	 * @return
	 */
	public static int roundFlooded(int elevation) {
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

	public static Direction getRandomDirection() {
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

	public static MapLocation closest(Iterable<? extends MapLocation> somethings, MapLocation source) throws GameActionException {
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
			if (q.get(i) == null) {
				continue;
			}
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

	public static Direction tryMove(RobotController rc, Direction dir) throws GameActionException {
		if (rc.canMove(dir) && rc.canSenseLocation(rc.getLocation().add(dir)) && !rc.senseFlooding(rc.getLocation().add(dir))) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateLeft())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateLeft()))) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateRight())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateRight()))) {
			return dir.rotateRight();
		} else if (rc.canMove(dir.rotateLeft().rotateLeft()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateLeft().rotateLeft()))
				&& !rc.senseFlooding(rc.getLocation().add(dir.rotateLeft().rotateLeft()))) {
			return dir.rotateLeft().rotateLeft();
		} else if (rc.canMove(dir.rotateRight().rotateRight()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateRight().rotateRight()))
				&& !rc.senseFlooding(rc.getLocation().add(dir.rotateRight().rotateRight()))) {
			return dir.rotateRight().rotateRight();
		} else {
			return null;
		}
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
	final int LOC_REFINERIJA = 10;
	/**
	 * Tukaj je tovarna dronov
	 */
	final int LOC_TOVARNA_DRONOV = 20;
	final int LOC_WATER = 25;
	final int LOC_ENEMY_NETGUN = 26;
	final int LOC_ALLY_NETGUN = 27;
	/**
	 * Tukaj zelimo imeti tovarno dronov
	 */
	final int BUILD_TOVARNA_DRONOV = 21;
	/**
	 * Tukaj zelimo imeti tovarno landscaperjev
	 */
	final int BUILD_TOVARNA_LANDSCAPERJEV = 22;
	/**
	 * Lokacija domace baze
	 */
	final int LOC_HOME_HQ = 30;
	final int LOC_ENEMY_HQ = 31;
	/**
	 * Enota zeli prevoz z dronom iz prve do druge lokacije
	 */
	final int LOC2_DRONE = 100;
	final int LOC2_DRONE_COMPLETE = 101;
	/**
	 * Miner z ID-jem int2 mora pomagati bazi in vsakih 10 potez obvestiti o tem da
	 * je se ziv.
	 */
	final int MINER_HELP_HQ = 1000;
	/**
	 * Miner z ID-jem int2 ima nalogo zru�iti enemyHQ
	 */
	final int MINER_RUSH = 1001;
	/**
	 * Enota z ID int2 je ziva
	 */
	final int UNIT_ALIVE = 1100;
	/**
	 * Baza ima stanje int1
	 */
	final int BASE_STRATEGY = 1200;

	private final int LOC_MAX = 99;// do 99 je en mapLocation
	private final int LOC2_MAX = 999;// od 100 do 999 sta 2

	List<paket> messages; // TODO lahko bi uporabili Heap (prioritetna vrsta glede na ceno)
	RobotController rc;
	int lastRoundOfQueueCheck = -1;
	int rounds_read = 0; // Koliko rund smo ze prebrali?

	blockchain(RobotController rc) {
		this.rc = rc;
		messages = new ArrayList<paket>();
	}

	public void handle_location(int type, MapLocation pos) {
	}

	public void handle_location2(int type, MapLocation m1, MapLocation m2, int id) {
	}

	public void handle_packet(int type, int[] message) {
	}

	public void send_location(int type, MapLocation pos) throws GameActionException {
		int[] msg = { PRIVATE_KEY, type, pos.x, pos.y, 0, 0, 0 };
		sendMsg(new paket(msg, 1));
	}

	public void send_location2(int type, MapLocation p1, MapLocation p2, int id) throws GameActionException {
		int[] msg = { PRIVATE_KEY, type, p1.x, p1.y, p2.x, p2.y, id };
		sendMsg(new paket(msg, 1));
	}

	public void send_packet(int type, int[] packet) throws GameActionException {
		sendMsg(new paket(packet, 1));
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
			int id = msg[6];
			handle_location2(type, m1, m2, id);
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
		if (lastRoundOfQueueCheck != rc.getRoundNum()) {
			lastRoundOfQueueCheck = rc.getRoundNum();
		} else {
			return;
		}
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

abstract class BasePathFinder {
	protected int LOOKAHEAD_STEPS = 5;
	protected int UNIT_MAX_WAIT = 2;

	protected static final int NO_WALL = 0; // Ne sledi zidu.
	protected static final int LEFT_WALL = 1; // Zid je na levi.
	protected static final int RIGHT_WALL = 2;

	protected RobotController rc;

	protected MapLocation goal;
	protected MapLocation closest; // Uporablja se pri bug navigation.
	protected MapLocation bug_wall; // Kje je zid, ki mu sledimo?
	protected int bug_wall_tangent = NO_WALL; // Na kateri strani je zid, ki mu sledimo?
	protected MapLocation tangent_shortcut; // Pomozna bliznjica.
	protected boolean ignore_units = true;
	protected int unit_wait_time = 0;
	protected int shortcut_steps_taken = 0;

	BasePathFinder(RobotController rc) {
		this.rc = rc;
	}

	protected boolean is_unit_obstruction(MapLocation at) {
		if (rc.canSenseLocation(at)) {
			try {
				RobotInfo robot = rc.senseRobotAtLocation(at);
				return robot != null && robot.getID() != rc.getID();
			} catch (GameActionException e) {
				return false;
			}
		}
		return false;
	}

	abstract boolean can_move(MapLocation from, Direction dir) throws GameActionException;

	protected Direction fuzzy(MapLocation dest) {
		MapLocation cur = rc.getLocation();
		Direction straight = cur.directionTo(dest);
		if (rc.canMove(straight))
			return straight;
		Direction left = straight.rotateLeft();
		if (rc.canMove(left))
			return left;
		Direction right = straight.rotateRight();
		if (rc.canMove(right))
			return right;
		left = left.rotateLeft();
		if (rc.canMove(left))
			return left;
		right = right.rotateRight();
		if (rc.canMove(right))
			return right;
		return null;
	}

	protected Direction fuzzy_step(MapLocation cur, MapLocation dest) throws GameActionException {
		Direction straight = cur.directionTo(dest);
		if (can_move(cur, straight))
			return straight;
		Direction left = straight.rotateLeft();
		if (can_move(cur, left))
			return left;
		Direction right = straight.rotateRight();
		if (can_move(cur, right))
			return right;
		left = left.rotateLeft();
		if (can_move(cur, left))
			return left;
		right = right.rotateRight();
		if (can_move(cur, right))
			return right;
		return null;
	}

	protected Direction fuzzy_step_short(MapLocation cur, MapLocation dest) throws GameActionException {
		Direction straight = cur.directionTo(dest);
		if (can_move(cur, straight))
			return straight;
		Direction left = straight.rotateLeft();
		if (can_move(cur, left))
			return left;
		Direction right = straight.rotateRight();
		if (can_move(cur, right))
			return right;
		return null;
	}

	protected Direction bug_step(MapLocation cur, MapLocation dest, int wall) throws GameActionException {
		Direction dir = fuzzy_step(cur, dest);
		if (dir != null && cur.add(dir).distanceSquaredTo(dest) < closest.distanceSquaredTo(dest)) {
			bug_wall = null;
			return dir;
		}

		// Ne moremo blizje, zato se drzimo zidu.
		// Drzimo se lahko leve ali desne strani: parameter 'wall'.
		Direction bug_wall_dir;
		if (bug_wall == null)
			bug_wall_dir = cur.directionTo(dest);
		else
			bug_wall_dir = cur.directionTo(bug_wall);

		if (wall == LEFT_WALL) {
			// V smeri urinega kazalca
			Direction right = bug_wall_dir;
			for (int i = 0; i < 8; ++i) {
				if (can_move(cur, right)) {
					bug_wall = cur.add(right.rotateLeft());
					return right;
				}
				right = right.rotateRight();
			}
		} else {
			// Nasprotna smer urinega kazalca
			Direction left = bug_wall_dir;
			for (int i = 0; i < 8; ++i) {
				if (can_move(cur, left)) {
					bug_wall = cur.add(left.rotateRight());
					return left;
				}
				left = left.rotateLeft();
			}
		}

		// To se lahko zgodi samo, ce je obkoljen ...
		return null;
	}

	protected Object[] bug_step_simulate(MapLocation cur, MapLocation dest, int wall, int steps) throws GameActionException {
		// Vrne [0]: direction po prvem koraku
		// [1]: wall loc po prvem koraku
		// [2]: wall loc po zadnjem koraku
		// [3]: koncna lokacija
		Object[] result = new Object[4];

		MapLocation prev_closest = closest;
		MapLocation prev_bug_wall = bug_wall;

		MapLocation end = cur;
		for (int i = 0; i < steps; ++i) {
			Direction dir = bug_step(end, dest, wall);
			if (i == 0) {
				result[0] = dir;
				result[1] = bug_wall;
			}
			if (dir == null) {
//				rc.setIndicatorDot(end, 255, 255, 0);
				break;
			}

			end = end.add(dir);
			if (end.distanceSquaredTo(dest) < closest.distanceSquaredTo(dest)) {
				closest = end;
			}
//			rc.setIndicatorDot(end, (255 / steps) * i, wall * 100, 255);

			if (is_at_goal(end, dest)) {
//				rc.setIndicatorDot(dest, 0, 255, 0);
				break;
			}
		}

		result[2] = bug_wall;
		result[3] = end;

		bug_wall = prev_bug_wall;
		closest = prev_closest;

		return result;
	}

	public boolean exists_path(MapLocation cur, MapLocation dest) throws GameActionException {
		Direction dir = fuzzy_step_short(cur, dest);
		while (!cur.equals(dest)) {
			if (cur.isWithinDistanceSquared(dest, 2)) {
				return true;
			}
			if (dir == null || !can_move(cur, dir))
				return false;
			cur = cur.add(dir);
			dir = fuzzy_step_short(cur, dest);
//			rc.setIndicatorDot(cur, 255, 0, 0);
		}
		return true;
	}

	protected boolean exists_fuzzy_path(MapLocation cur, MapLocation dest, int max_steps) throws GameActionException {
		Direction dir = fuzzy_step(cur, dest);
		for (int steps = 0; dir != null && !cur.equals(dest) && steps < max_steps; ++steps) {
			if (!can_move(cur, dir))
				return false;
			cur = cur.add(dir);
			dir = fuzzy_step(cur, dest);
		}
		return cur.equals(dest);
	}

	protected Direction run_simulation(MapLocation cur, Object[] simulation, int wall) throws GameActionException {
		MapLocation end = (MapLocation) simulation[3];
		bug_wall_tangent = wall;
		if (exists_fuzzy_path(cur, end, LOOKAHEAD_STEPS - 1)) {
			tangent_shortcut = end;
			bug_wall = (MapLocation) simulation[2];
			return fuzzy_step(cur, tangent_shortcut);
		}
		bug_wall = (MapLocation) simulation[1];
		return (Direction) simulation[0];
	}

	protected Direction tangent_bug(MapLocation dest) throws GameActionException {
		// Odlocimo se med levo in desno stranjo in potem
		// nadaljujemo po izbrani poti.
		// Ce najdemo bliznjico, gremo do nje po najkrajsi poti
		// in potem nadaljujemo pot.

		MapLocation cur = rc.getLocation();
		if (cur.equals(tangent_shortcut)) {
			tangent_shortcut = null;
			shortcut_steps_taken = 0;
		}
		if (tangent_shortcut != null) {
			// Naj bi obstajala fuzzy pot do tam ...?
			if (shortcut_steps_taken <= LOOKAHEAD_STEPS) {
				Direction dir = fuzzy_step(cur, tangent_shortcut);
				if (dir != null && can_move(cur, dir)) {
					shortcut_steps_taken++;
					return dir;
				}
			}
			// Zgubili smo se ali pa je ovira ...
			reset_tangent();
		}

		if (bug_wall != null && can_move(cur, cur.directionTo(bug_wall))) {
			reset_tangent();
		}

		// Stran zidu je ze izbrana
		// Simularmo pot z izbranim zidom
		if (bug_wall_tangent != NO_WALL) {
			// bug_step(cur, dest, bug_wall_tangent);
			Object[] simulation = bug_step_simulate(cur, dest, bug_wall_tangent, LOOKAHEAD_STEPS);
			return run_simulation(cur, simulation, bug_wall_tangent);
		}

		// Odlocimo se med levo in desno stranjo
		Object[] left_simulation = bug_step_simulate(cur, dest, LEFT_WALL, LOOKAHEAD_STEPS);
		Object[] right_simulation = bug_step_simulate(cur, dest, RIGHT_WALL, LOOKAHEAD_STEPS);
		MapLocation left_pos = (MapLocation) left_simulation[3];
		MapLocation right_pos = (MapLocation) right_simulation[3];

		int d1 = right_pos.distanceSquaredTo(dest);
		int d2 = left_pos.distanceSquaredTo(dest);
		if (d1 == d2) {
//			rc.setIndicatorDot(cur, 0, 0, 0);
			// Preverimo, katera smer je blizja po enem koraku
		}
		if (d1 <= d2) {
			return run_simulation(cur, right_simulation, RIGHT_WALL);
		} else {
			return run_simulation(cur, left_simulation, LEFT_WALL);
		}
	}

	protected boolean is_at_goal(MapLocation cur, MapLocation dest) throws GameActionException {
		if (cur.equals(dest))
			return true;
		boolean adj = cur.isAdjacentTo(dest);
		if (adj && can_move(cur, cur.directionTo(dest))) {
			return false;
		}
		return adj;
	}

	protected Object[] save_state() {
		return new Object[] { closest, bug_wall, bug_wall_tangent, tangent_shortcut, shortcut_steps_taken };
	}

	protected void set_state(Object[] state) {
		closest = (MapLocation) state[0];
		bug_wall = (MapLocation) state[1];
		bug_wall_tangent = (int) state[2];
		tangent_shortcut = (MapLocation) state[3];
		shortcut_steps_taken = (int) state[4];
	}

	protected void reset_tangent() {
		tangent_shortcut = null;
		bug_wall_tangent = NO_WALL;
		bug_wall = null;
		closest = rc.getLocation();
		shortcut_steps_taken = 0;
	}

	public void reset() {
		goal = null;
		// closest = rc.getLocation();
		reset_tangent();
	}

	public Direction get_move_direction(MapLocation dest) throws GameActionException {
		MapLocation cur = rc.getLocation();
		if (is_at_goal(cur, dest)) {
			reset();
			return null;
		}

		if (!dest.equals(goal)) {
			reset();
			goal = dest;
		} else {
			if (cur.distanceSquaredTo(dest) < closest.distanceSquaredTo(dest)) {
				closest = cur;
			}
		}

//		if (tangent_shortcut != null)
//			rc.setIndicatorDot(tangent_shortcut, 255, 0, 0);

		if (unit_wait_time >= UNIT_MAX_WAIT) {
			ignore_units = false;
			unit_wait_time = 0;
		} else {
			ignore_units = true;
		}
		Object[] prev_state = save_state();
		Direction dir = tangent_bug(dest);
		if (dir == null || is_unit_obstruction(cur.add(dir))) {
			unit_wait_time++;
			set_state(prev_state);
//			rc.setIndicatorDot(cur.add(dir), 200, 0, 255);
			return null;
		} else {
			unit_wait_time = 0;
		}
		return dir;
	}

	public boolean moveTowards(MapLocation dest) throws GameActionException {
		Direction dir = get_move_direction(dest);

		if (dir != null && rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		return false;
	}
}
