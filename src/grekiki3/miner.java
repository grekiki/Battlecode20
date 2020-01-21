package grekiki3;

import java.util.Arrays;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

class vector_set_gl {
	private boolean[][] grid;
	MapLocation[] q;
	public int size = 0;
	private int cappacity = 20;
	private int load = 0;

	vector_set_gl() {
		q = new MapLocation[20];
		grid = new boolean[64][];
		grid[0] = new boolean[64];
		for (int i = 1; i < 64; i++) {
			grid[i] = grid[0].clone();
		}
	}

	void add(MapLocation ml) {
		if (!grid[ml.x][ml.y]) {
			load++;
			grid[ml.x][ml.y] = true;
			q[size++] = ml;
			if (size == cappacity) {// Redek pojav

				// Ce je load factor precej velik potem kloniramo. Drugace cistimo
				double limit = 0.7;
				if (load / (double) cappacity > limit) {
					q = Arrays.copyOf(q, 2 * cappacity);
					cappacity *= 2;
				} else {
					MapLocation[] q2 = new MapLocation[cappacity];
					int p = 0;
					for (int i = 0; i < size; i++) {
						if (grid[q[i].x][q[i].y]) {
							q2[p++] = q[i];
						}
					}
					q = q2;
					size = load;
				}
			}
		}

	}

	MapLocation get(int a) {
		return q[a];
	}

	void remove(MapLocation ml) {
		if (grid[ml.x][ml.y]) {
			load--;
			grid[ml.x][ml.y] = false;
		}
	}

	boolean contains(MapLocation m) {
		return grid[m.x][m.y];
	}
}

class MapCell {
	int soupCount = 0;
	int pollutionLevel = 0;
	int elevation = 0;
	boolean flooded = false;
	int lastTurnSeen = -1;
	RobotInfo robot = null;

	MapCell(int soup, int pollution, int elevation, boolean flooded, int lastSeen, RobotInfo robot) {
		soupCount = soup;
		pollutionLevel = pollution;
		this.elevation = elevation;
		this.flooded = flooded;
		lastTurnSeen = lastSeen;
		this.robot = robot;
	}
}

class minerPathFinder {
	private static final int LOOKAHEAD_STEPS = 5;
	private static final int UNIT_MAX_WAIT = 2;

	private static final int NO_WALL = 0; // Ne sledi zidu.
	private static final int LEFT_WALL = 1; // Zid je na levi.
	private static final int RIGHT_WALL = 2;

	private RobotController rc;

	private MapLocation goal;
	private MapLocation closest; // Uporablja se pri bug navigation.
	private MapLocation bug_wall; // Kje je zid, ki mu sledimo?
	private int bug_wall_tangent = NO_WALL; // Na kateri strani je zid, ki mu sledimo?
	private MapLocation tangent_shortcut; // Pomozna bliznjica.
	private boolean ignore_units = true;
	private int unit_wait_time = 0;

	minerPathFinder(RobotController rc) {
		this.rc = rc;
	}

	private boolean is_unit_obstruction(MapLocation at) {
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

	private boolean can_move(MapLocation from, Direction dir) throws GameActionException {
		// Ta metoda ignorira cooldown ...

		MapLocation to = from.add(dir);
		if (!rc.canSenseLocation(to) || rc.senseFlooding(to))
			return false;
		if (!rc.canSenseLocation(from) || Math.abs(rc.senseElevation(from) - rc.senseElevation(to)) > 3)
			return false;
		RobotInfo robot = rc.senseRobotAtLocation(to);
		if (robot != null && robot.getID() != rc.getID() && (!ignore_units || robot.getType().isBuilding()))
			return false;
		return true;
	}

	@SuppressWarnings("unused")
	private Direction fuzzy(MapLocation dest) {
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

	private Direction fuzzy_step(MapLocation cur, MapLocation dest) throws GameActionException {
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

	private Direction fuzzy_step_short(MapLocation cur, MapLocation dest) throws GameActionException {
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

	private Direction bug_step(MapLocation cur, MapLocation dest, int wall) throws GameActionException {
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

	private Object[] bug_step_simulate(MapLocation cur, MapLocation dest, int wall, int steps)
			throws GameActionException {
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

	private boolean exists_fuzzy_path(MapLocation cur, MapLocation dest, int max_steps) throws GameActionException {
		Direction dir = fuzzy_step(cur, dest);
		for (int steps = 0; dir != null && !cur.equals(dest) && steps < max_steps; ++steps) {
			if (!can_move(cur, dir))
				return false;
			cur = cur.add(dir);
			dir = fuzzy_step(cur, dest);
		}
		return cur.equals(dest);
	}

	private Direction run_simulation(MapLocation cur, Object[] simulation, int wall) throws GameActionException {
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

	private Direction tangent_bug(MapLocation dest) throws GameActionException {
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

	private boolean is_at_goal(MapLocation cur, MapLocation dest) throws GameActionException {
		boolean adj = cur.isAdjacentTo(dest);
		if (adj && can_move(cur, cur.directionTo(dest))) {
			return false;
		}
		return adj || cur.equals(dest);
	}

	private Object[] save_state() {
		return new Object[] { closest, bug_wall, bug_wall_tangent, tangent_shortcut };
	}

	private void set_state(Object[] state) {
		closest = (MapLocation) state[0];
		bug_wall = (MapLocation) state[1];
		bug_wall_tangent = (int) state[2];
		tangent_shortcut = (MapLocation) state[3];
	}

	private void reset_tangent() {
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

class naloga {
	// ID-ji nalog ki jih lahko delamo
	final static int GRADNJA_REFINERIJE = 10;
	final static int GRADNJA_TOVARNE_ZA_DRONE = 11;
	final static int NABIRANJE = 20;
	final static int PREMIK_DO_JUHE = 30;
	final static int PREMIK_DO_POLJA = 31;
	final static int PREMIKANJE_JUHE_V_BAZO = 40;
	final static int RAZISKOVANJE_JUHE = 50;
	final static int RAZISKOVANJE_MAPE = 60;
	miner m;
	MapLocation mesto;// ni nujno da naloga da mesto, je pa tako pogosto da pride prav
	int value;
	int type;

	naloga(miner mi, MapLocation mm, int c, int t) {
		m = mi;
		mesto = mm;// lahko je null!
		value = c;
		type = t;
	}

	public void run() throws GameActionException {
		System.out.println("Naloga " + type);
		switch (type) {
		case GRADNJA_REFINERIJE:
			gradnja_refinerije();
			break;
		case GRADNJA_TOVARNE_ZA_DRONE:
			gradnja_tovarne_za_drone();
			break;
		case NABIRANJE:
			nabiranje();
			break;
		case PREMIK_DO_JUHE:
			premik_do_juhe();
			break;
		case PREMIK_DO_POLJA:
			premik_do_polja();
			break;
		case PREMIKANJE_JUHE_V_BAZO:
			premikanje_juhe_v_bazo();
			break;
		case RAZISKOVANJE_JUHE:
			raziskovanje_juhe();
			break;
		case RAZISKOVANJE_MAPE:
			raziskovanje_mape();
			break;
		}
	}

	private void gradnja_tovarne_za_drone() throws GameActionException {
		if (m.rc.getLocation().distanceSquaredTo(mesto) <= 2) {
			Direction d = m.rc.getLocation().directionTo(mesto);
			if (m.rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, d)) {
				m.rc.buildRobot(RobotType.FULFILLMENT_CENTER, d);
				value = 0;
				return;
			}
		} else {
			m.path_finder.moveTowards(this.mesto);
		}
	}

	private void raziskovanje_mape() throws GameActionException {
		if (Math.random() < 0.05 || m.rc.getLocation().distanceSquaredTo(this.mesto) <= 2) {// Menjamo smer // casa
			value = 0;
			return;
		}
		m.path_finder.moveTowards(this.mesto);
	}

	private void raziskovanje_juhe() throws GameActionException {
		m.path_finder.moveTowards(this.mesto);
	}

	private void premikanje_juhe_v_bazo() throws GameActionException {
		if (m.tryDepositSoup()) {
			value = 0;
			return;
		}
		m.path_finder.moveTowards(m.hq_location);

	}

	private void premik_do_juhe() throws GameActionException {
		if (!m.juhe.contains(this.mesto)) {
			value = 0;
			return;
		}
		m.path_finder.moveTowards(this.mesto);
	}

	private void premik_do_polja() throws GameActionException {
		if (!m.polja.contains(this.mesto)) {
			value = 0;
			return;
		}
		m.path_finder.moveTowards(this.mesto);
	}

	private void nabiranje() throws GameActionException {
		if (m.rc.canSenseLocation(mesto) && m.rc.senseSoup(mesto) == 0) {
			value = 0;
			return;
		}
		if (m.tryMine()) {
			value = 0;
			return;
		}
		m.path_finder.moveTowards(this.mesto);
	}

	private void gradnja_refinerije() throws GameActionException {
		if (m.rc.getLocation().distanceSquaredTo(mesto) <= 2) {
			Direction d = m.rc.getLocation().directionTo(mesto);
			if (m.rc.canBuildRobot(RobotType.REFINERY, d)) {
				m.rc.buildRobot(RobotType.REFINERY, d);
				value = 0;
				return;
			}
		} else {
			m.path_finder.moveTowards(this.mesto);
		}
	}
}

/**
 * TO-DO ce se do neke surovine sprehajamo vec kot recimo 50 potez, potem recemo
 * da je tezka
 * 
 * @author Gregor
 *
 */
public class miner extends robot {
	public static final int MINER_COST = RobotType.MINER.cost;
	public static final int razmik_med_polji = 20;
	public static final int optimize = 10;

	naloga task;
	final static int GRADNJA_REFINERIJE = 1000;
	final static int PREMIKANJE_JUHE_V_BAZO = 300;
	final static int NABIRANJE = 250;// Vidimo juho do katere lahko dokazano pridemo
	final static int PREMIK_DO_JUHE = 200;// Baje da se do juhe da priti. Pathfinding
	final static int PREMIK_DO_POLJA = 150;// Baje da se do juhe da priti. Pathfinding
	final static int RAZISKOVANJE_JUHE = 100;// Poiscemo pot do slabe juhe
	final static int RAZISKOVANJE_MAPE = 50;

	minerPathFinder path_finder;
	vector_set_gl juhe;
	vector_set_gl slabe_juhe;// tiste za katere ne vemo kako priti do njih
	vector_set_gl polja;
	vector_set_gl slaba_polja;// ne vemo a se da priti do njih
	vector_set_gl refinerije;
	int w, h;// dimenzije mape

	MapLocation hq_location;
	/**
	 * 0- obicajno<br>
	 * 10- pomagamo hq 20- hq je napaden 30- nasprotnik uporablja drone
	 */
	int stanje;

	public miner(RobotController rc) {
		super(rc);
	}

	@Override
	public void init() throws GameActionException {
		path_finder = new minerPathFinder(rc);
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		for (RobotInfo r : rc.senseNearbyRobots(2, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq_location = r.location;
			}
		}
		juhe = new vector_set_gl();
		slabe_juhe = new vector_set_gl();
		polja = new vector_set_gl();
		slaba_polja = new vector_set_gl();
		refinerije = new vector_set_gl();
		refinerije.add(hq_location);
		stanje = 0;
		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	@Override
	public void precompute() throws GameActionException {
//		System.out.println("\n" + rc.getRoundNum());
		b.checkQueue();
	}

	@Override
	public void runTurn() throws GameActionException {
//		System.out.println("Pred potezo " + Clock.getBytecodesLeft());
		if (!rc.isReady()) {
			return;
		}
		findBestTask();
		if (task != null) {
			task.run();
		}

	}

	public void postcompute() throws GameActionException {
		update_soup();
		while (Clock.getBytecodesLeft() > 500) {
			if (!b.read_next_round()) {
				break;
			}
		}
	}

	private void update_soup() throws GameActionException {
//		System.out.println("Za "+juhe.size+" juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		if (Clock.getBytecodesLeft() < 1000) {
			System.out.println("tle");
			return;
		}
		// Dodamo juhe ki jih vidimo
		MapLocation[] q = rc.senseNearbySoup();
		int i = q.length;
		while (i-- > 0) {
			if (i % optimize != rc.getRoundNum() % optimize) {
				continue;
			}
			MapLocation m = q[i];
			if (Clock.getBytecodesLeft() < 1000) {
				System.out.println("tle dodajanje juh");
				return;
			}
			if (!juhe.contains(m) && !slabe_juhe.contains(m)) {
				if (Util.d_inf(rc.getLocation(), m) <= 5 && path_finder.exists_path(rc.getLocation(), m)) {
					juhe.add(m);
					check_for_field(m);
				} else {
					slabe_juhe.add(m);
					check_for_bad_field(m);
				}
			}
		}

//		System.out.println("Za odstranjevanje juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		for (i = 0; i < juhe.size; i++) {
			if (i % optimize != rc.getRoundNum() % optimize) {
				continue;
			}
			if (Clock.getBytecodesLeft() < 1000) {
				System.out.println("tle odstranjevanje praznih juh");
				return;
			}
			MapLocation m = juhe.get(i);

//			rc.setIndicatorDot(m, 0, 255, 0);
			if (rc.canSenseLocation(m) && rc.senseSoup(m) == 0) {
				juhe.remove(m);
				i--;
			}
		}

//		System.out.println("Za odstranjevanje slabih juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		for (i = 0; i < slabe_juhe.size; i++) {
			MapLocation m = slabe_juhe.get(i);
			if (Clock.getBytecodesLeft() < 1000) {
				System.out.println("tle odstanjevanje slabih praznih juh");
				return;
			}
//			rc.setIndicatorDot(m, 255, 0, 0);
			if (rc.canSenseLocation(m) && rc.senseSoup(m) == 0) {
				slabe_juhe.remove(m);
				i--;
			}
		}

//		System.out.println("Za premikanje juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		for (i = 0; i < slabe_juhe.size; i++) {
			if (i % optimize != rc.getRoundNum() % optimize) {
				continue;
			}
			// To delamo le vsakih 10 potez
			if (i % optimize != rc.getRoundNum() % optimize) {
				continue;
			}
			MapLocation m = slabe_juhe.get(i);
			if (Clock.getBytecodesLeft() < 1000) {
				System.out.println("tle premikanje slabih v dobre juhe");
				return;
			}
			if (Util.d_inf(rc.getLocation(), m) <= 5 && path_finder.exists_path(rc.getLocation(), m)) {
				juhe.add(m);// vedno ko dodamo juho, preverimo ce dodamo polje
				check_for_field(m);
				slabe_juhe.remove(m);// TO-DO odstrani polje povezano s to juho?
				i--;
			}
		}

		/**
		 * pogledamo ce imamo polje, da velja<br>
		 * 1. Vidimo vsaj razmik_med_polji stran od polje<br>
		 * 2. V tem obmocju ni nobene dosegljive juhe.
		 */
		for (i = 0; i < polja.size; i++) {
			MapLocation m = polja.get(i);
			if (rc.getLocation().distanceSquaredTo(m) <= 2) {
				// Kako pogledati ce je okoli polja juha? Simple: Scan
				if (rc.senseNearbySoup(m, 20).length == 0) {
					polja.remove(m);
					i--;
				}
			}
		}
//		System.out.println("Za barvanje juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		if (Clock.getBytecodesLeft() < 1000) {
			System.out.println("tle pred barvanjem");
			return;
		}
		for (i = 0; i < polja.size; i++) {
			MapLocation m = polja.get(i);
			rc.setIndicatorDot(m, 0, 255, 0);
		}
		if (Clock.getBytecodesLeft() < 1000) {
			System.out.println("tle med barvanjem");
			return;
		}
		for (i = 0; i < slaba_polja.size; i++) {
			MapLocation m = slaba_polja.get(i);
			rc.setIndicatorDot(m, 255, 0, 0);
		}

	}

	private void check_for_field(MapLocation m) throws GameActionException {
		System.out.println("Preverjamo ce je " + m + "polje ");
		System.out.println(juhe.size + " " + slabe_juhe.size);
		MapLocation closest = Util.closest(polja, m);
		if (closest != null && closest.distanceSquaredTo(m) < razmik_med_polji) {
			return;
		}
		if (rc.canSenseLocation(m)) {
			polja.add(m);
			b.send_location(b.LOC_SUROVINA, m);
		}
	}

	private void check_for_bad_field(MapLocation m) throws GameActionException {
		MapLocation closest = Util.closest(slaba_polja, m);
		if (closest != null && closest.distanceSquaredTo(m) < razmik_med_polji) {
			return;
		}
		if (rc.canSenseLocation(m)) {
			polja.add(m);
			b.send_location(b.LOC_SLABA_SUROVINA, m);
		}
	}

	// Util
	public void findBestTask() throws GameActionException {
		int currentValue = (task == null ? 0 : task.value);
		if (currentValue < PREMIKANJE_JUHE_V_BAZO) {
			if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
				task = new naloga(this, hq_location, PREMIKANJE_JUHE_V_BAZO, naloga.PREMIKANJE_JUHE_V_BAZO);
				currentValue = PREMIKANJE_JUHE_V_BAZO;
			}
		}
		if (currentValue < NABIRANJE && rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
			MapLocation ans = Util.closest(juhe, rc.getLocation());
			if (ans != null && path_finder.exists_path(rc.getLocation(), ans)) {
				task = new naloga(this, ans, NABIRANJE, naloga.NABIRANJE);
				currentValue = NABIRANJE;
			}
		}
		if (currentValue < PREMIK_DO_JUHE && rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
			MapLocation ans = Util.closest(juhe, rc.getLocation());
			if (ans != null) {
				task = new naloga(this, ans, PREMIK_DO_JUHE, naloga.PREMIK_DO_JUHE);
				currentValue = PREMIK_DO_JUHE;
			}
		}
		if (currentValue < PREMIK_DO_POLJA && rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
			MapLocation ans = Util.closest(juhe, rc.getLocation());
			if (ans != null) {
				task = new naloga(this, ans, PREMIK_DO_POLJA, naloga.PREMIK_DO_POLJA);
				currentValue = PREMIK_DO_POLJA;
			}
		}
		if (currentValue < RAZISKOVANJE_JUHE) {
			MapLocation ans = Util.closest(slabe_juhe, rc.getLocation());
			if (ans != null) {
				task = new naloga(this, ans, RAZISKOVANJE_JUHE, naloga.RAZISKOVANJE_JUHE);
				currentValue = RAZISKOVANJE_JUHE;
			}
		}
		if (currentValue < RAZISKOVANJE_MAPE) {
			MapLocation ans = Util.randomPoint(h, w);
			if (ans != null) {
				task = new naloga(this, ans, RAZISKOVANJE_MAPE, naloga.RAZISKOVANJE_MAPE);
				currentValue = RAZISKOVANJE_MAPE;
			}
		}
	}

	public boolean tryDepositSoup() throws GameActionException {
		for (Direction d : Util.dir) {
			if (rc.canDepositSoup(d) && rc.senseRobotAtLocation(rc.getLocation().add(d)).team == rc.getTeam()) {
				rc.depositSoup(d, rc.getSoupCarrying());
				return true;
			}
		}
		return false;
	}

	public boolean tryMine() throws GameActionException {
		for (Direction d : Util.dir) {
			if (rc.canMineSoup(d)) {
				rc.mineSoup(d);
				return true;
			}
		}
		return false;
	}

	@Override
	public void bc_polje_found(MapLocation pos) {
		if (!polja.contains(pos)) {
			polja.add(pos);
		}
	}

	@Override
	public void bc_polje_empty(MapLocation pos) {
		if (polja.contains(pos)) {
			polja.remove(pos);
		}
		if (slaba_polja.contains(pos)) {
			slaba_polja.remove(pos);
		}
	}

	@Override
	public void bc_polje_slabo(MapLocation pos) {
		if (!slaba_polja.contains(pos)) {
			slaba_polja.add(pos);
		}
	}

	@Override
	public void bc_polje_upgrade(MapLocation pos) {
		if (slaba_polja.contains(pos)) {
			slaba_polja.remove(pos);
		}
		if (!polja.contains(pos)) {
			polja.add(pos);
		}
	}

	@Override
	public void bc_rafinerija(MapLocation pos) {
		if (polja.contains(pos)) {
			polja.remove(pos);
		}
	}

	@Override
	public void bc_build_tovarna_dronov(MapLocation pos) {
		if (stanje == 10) {
			task = new naloga(this, pos, 10000, naloga.GRADNJA_TOVARNE_ZA_DRONE);
		}
	}

	@Override
	public void bc_miner_to_help(int[] message) {
		if (rc.getID() == message[2]) {
			stanje = 10;
		}
	}

}
