package grekiki26;

import java.util.ArrayList;
import java.util.Arrays;

import battlecode.common.*;

/*
 * Sifrirni sistem za sporocila. 
 * Prvi int je privatna cifra ki jo uporabimo za sifriranje. 
 * Drugi je tip sporocila.
 * msg[2]...msg[6] je vsebina
 * 
 * Tukaj bo slovar ki pove kaj dolocena stevilka pomeni ce je navedena kot vsebina
 * 
 * msg[1]==1--> msg[2] pove fazo v kateri je trenutno baza. msg[3] pove kateri worker gre gradit bazo
 * 
 * msg[1]==2--> msg[2],msg[3] so koordinate polja surovin.
 * 
 * msg[1]==3--> Polje na msg[2],msg[3] je prazno. 
 * 
 * msg[1]==4--> na msg[2],msg[3] sedaj stoji refinerija. 
 * 
 * msg[1]==5--> na msg[2],msg[3] sedaj stoji tovarna dronov. 
 * 
 * msg[1]==6--> na msg[2],msg[3] so koordinate na�ega hq. 
 * 
 * msg[1]==7--> enota bi �la iz msg[2],msg[3] v msg[4],msg[5]. Za prevoz potrebuje drona. 
 */

/*
 * Faze:
 * Faza 0: Nakup minerjev ki cimprej poiscejo kaksno surovino
 * 
 * Faza 1: Razvoj ekonomije. ce imajo minerji oznacena kaksna dobra surovinska polja, naj razmislijo
 * o rafineriji ce je to dalec, ali pa naj se naberejo ce je blizu. Nekje 6 minerjev lahko precej v redu zapolni rafinerijo. 
 * Rafinerije se gradi stran od baze, da ne skodimo zidu z pollutionom. 
 * Premik v fazo 3 naredimo ko imamo dovolj surovin za to. 
 * 
 * Faza 3: Sedaj bi morali imeti ogromno surovin, in se lahko lotimo napada/obrambe. Baza pokli�e enega minerja nazaj.
 * 
 * Faza 4: Baza ima zid, ki pa ga je treba izbolj�ati. Dronov se ne gradi prevec. 
 * 
 * 
 * Celoten plan temelji bolj na ekonomiji, tako da se verjetno splaca biti previden glede invazij.  
 * 
 */

/*Plan baze
 *             Faza 0:
 *              ...
 *              .B.
 *              ...
 *              
 *              V fazi 1 naredimo tovarne za drone
 *              D..
 *              .B.
 *              ..D
 *              V fazi 2 naredimo tovarne za diggerje
 *              D..
 *              .H.
 *              ..L
 * 				V fazi 3 okoli baze okrepimo obrambo
 * 				NNNNNNN...
 *              NWWWWWN
 *              NWD..WN
 *              NW.H.WN
 *              NW..LWN
 *              NWWWWWN
 *              NNNNNNN
 * 	
 */

/*
 * TODO list
 * -> Branje blockchaina se mora dovolj hitro ustaviti, in se nadaljuje med igro bota. Ne moremo vsega prebrati na za�etku! Oziroma
 * lahko ampak le do morda poteze 500
 */
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
		}
		if (tangent_shortcut != null) {
			// Naj bi obstajala fuzzy pot do tam ...?
			Direction dir = fuzzy_step(cur, tangent_shortcut);
			if (dir != null && can_move(cur, dir))
				return dir;
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
		if (cur.equals(dest)) return true;
		boolean adj = cur.isAdjacentTo(dest);
		if (adj && can_move(cur, cur.directionTo(dest))) {
			return false;
		}
		return adj;
	}

	protected Object[] save_state() {
		return new Object[] { closest, bug_wall, bug_wall_tangent, tangent_shortcut };
	}

	protected void set_state(Object[] state) {
		closest = (MapLocation) state[0];
		bug_wall = (MapLocation) state[1];
		bug_wall_tangent = (int) state[2];
		tangent_shortcut = (MapLocation) state[3];
	}

	protected void reset_tangent() {
		tangent_shortcut = null;
		bug_wall_tangent = NO_WALL;
		bug_wall = null;
		closest = rc.getLocation();
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
		if (is_unit_obstruction(cur.add(dir))) {
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
class blockchain {
	ArrayList<paket> msg;
	RobotController rc;

	blockchain(RobotController rc) {
		this.rc = rc;
		msg = new ArrayList<paket>();
	}

	public void sendMsg(paket p) throws GameActionException {
		if (rc.canSubmitTransaction(p.data, p.cost)) {
			rc.submitTransaction(p.data, p.cost);
			System.out.println(rc.getLocation()+" je poslal posto");
			msg.add(new paket(p.data, p.cost));
		} else {
			msg.add(new paket(p.data, 0));
		}
	}

	public void checkQueue() throws GameActionException {
		int minCost = 1000000;
		Transaction[] t = rc.getBlock(rc.getRoundNum() - 1);
		for (int i = 0; i < msg.size(); i++) {
			paket p = msg.get(i);
			for (Transaction tt : t) {
				if (Arrays.equals(tt.getMessage(), p.data)) {
					msg.remove(p);
					i--;
					break;
				}
				minCost = Math.min(minCost, tt.getCost());
			}
		}
		minCost = (minCost == 1000000 ? konst.min_cena_transakcije : minCost);
		for (int i = 0; i < msg.size(); i++) {
			paket p = msg.get(i);
			if (p.cost >= minCost) {// Placali smo ze dovolj
				continue;
			}
			int[] msgg = p.data;
			if (rc.canSubmitTransaction(msgg, minCost)) {
				rc.submitTransaction(msgg, minCost);
				msg.remove(p);
				System.out.println("Poslano");
				i--;
			}
		}
//		System.out.println(msg.size() + " paketov caka");
	}

}

class Util {
	public static Direction[] dir = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };

	// Garantirano terminira
	public static Direction tryMoveLite(RobotController rc, Direction dir) throws GameActionException {
		if (rc.canMove(dir) && rc.canSenseLocation(rc.getLocation().add(dir)) && !rc.senseFlooding(rc.getLocation().add(dir))) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateLeft())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateLeft()))) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateRight())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateRight()))) {
			return dir.rotateRight();
		} else {
			return null;
		}
	}

	public static Direction tryMove(RobotController rc, Direction dir) throws GameActionException {
		if (rc.canMove(dir) && rc.canSenseLocation(rc.getLocation().add(dir)) && !rc.senseFlooding(rc.getLocation().add(dir))) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateLeft())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateLeft()))) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateRight())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateRight()))) {
			return dir.rotateRight();
		} else if (rc.canMove(dir.rotateLeft().rotateLeft()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateLeft().rotateLeft())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateLeft().rotateLeft()))) {
			return dir.rotateLeft().rotateLeft();
		} else if (rc.canMove(dir.rotateRight().rotateRight()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateRight().rotateRight())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateRight().rotateRight()))) {
			return dir.rotateRight().rotateRight();
		} else {
			return null;
		}
	}

	public static Direction getRandomDirection() {
		return dir[(int) Math.floor(dir.length * Math.random())];
	}
	
	public static int d_inf(MapLocation l1,MapLocation l2) {//d neskon�no metrika. 
		return Math.max(Math.abs(l2.x-l1.x),Math.abs(l2.y-l1.y));
	}

	public static Direction tryMoveLiteDrone(RobotController rc,Direction dir){
		if (rc.canMove(dir)) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		} else if (rc.canMove(dir.rotateLeft().rotateLeft())) {
			return dir.rotateLeft().rotateLeft();
		} else if (rc.canMove(dir.rotateRight().rotateRight())) {
			return dir.rotateRight().rotateRight();
		} else {
			return null;
		}
	}
	public static Direction tryMoveDrone(RobotController rc,Direction dir){
		if (rc.canMove(dir)) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		} else {
			return null;
		}
	}
}

abstract class robot {
	public abstract void init() throws Exception;

	public abstract void precompute() throws Exception;

	public abstract void runTurn() throws Exception;

	public abstract void postcompute() throws Exception;

}

public strictfp class RobotPlayer {
	static RobotController rc;

	public static void run(RobotController rci) {
		rc = rci;
		robot r = null;
		switch (rc.getType()) {
		case HQ:
			r = new HQ(rc);
			break;
		case MINER:
			r = new miner(rc);
			break;
		case REFINERY:
			r = new refinery(rc);
			break;
		case VAPORATOR:
			r = new vaporator(rc);
			break;
		case DESIGN_SCHOOL:
			r = new design_school(rc);
			break;
		case FULFILLMENT_CENTER:
			r = new fulfillment_center(rc);
			break;
		case LANDSCAPER:
			r = new landscaper(rc);
			break;
		case DELIVERY_DRONE:
			r = new delivery_drone(rc);
			break;
		case NET_GUN:
			r = new net_gun(rc);
			break;
		case COW:
			break;
		default:
			break;
		}
		try {
			r.init();
		}catch(ArithmeticException e) {
			System.out.println("DIE!");
			int t=1/0;
			System.out.println(t);
		}catch (Exception e) {
		
			System.out.println(Arrays.toString(e.getStackTrace()));
			System.out.println(e.getMessage());
		}
		while (true) {
			try {
				int init=rc.getRoundNum();
				r.precompute();
				r.runTurn();
				r.postcompute();
				if(rc.getRoundNum()!=init) {
					System.out.println("Prevec racunanja!");
				}
				Clock.yield();
			} catch (Exception e) {
				e.printStackTrace();
				Clock.yield();
			}
		}
		
	}

}
