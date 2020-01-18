package grekiki3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

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
	private static final int NO_WALL = 0;  // Ne sledi zidu.
	private static final int LEFT_WALL = 1;  // Zid je na levi.
	private static final int RIGHT_WALL = 2;
	private static final int LOOKAHEAD_STEPS = 5;

	private RobotController rc;

	private MapLocation goal;
	private MapLocation closest;  // Uporablja se pri bug navigation.
	private Direction bug_wall_dir;  // V kateri smeri je zid, ki mu sledimo?
	private int bug_wall_tangent = NO_WALL;  // Na kateri strani je zid, ki mu sledimo?
	private MapLocation tangent_shortcut;  // Pomozna bliznjica.

	minerPathFinder(RobotController rc) {
		this.rc = rc;
	}

	private boolean can_move(MapLocation from, Direction to) {
		// Ta metoda ignorira cooldown ...

		MapLocation p = from.add(to);
		try {
			if (!rc.onTheMap(p) || rc.senseFlooding(p))
				return false;
			if (Math.abs(rc.senseElevation(from) - rc.senseElevation(p)) > 3)
				return false;
			if (rc.senseRobotAtLocation(p) != null)
				return false;
		} catch (GameActionException e) {
			return false;
		}
		return true;
	}

	private Direction fuzzy(MapLocation dest) {
	    MapLocation cur = rc.getLocation();
	    Direction straight = cur.directionTo(dest);
	    if (rc.canMove(straight)) return straight;
	   	Direction left = straight.rotateLeft();
	   	if (rc.canMove(left)) return left;
	   	Direction right = straight.rotateRight();
	   	if (rc.canMove(right)) return right;
	   	left = left.rotateLeft();
	   	if (rc.canMove(left)) return left;
	   	right = right.rotateRight();
	   	if (rc.canMove(right)) return right;
		return null;
	}

	private Direction fuzzy_step(MapLocation cur, MapLocation dest) {
		Direction straight = cur.directionTo(dest);
		if (can_move(cur, straight)) return straight;
		Direction left = straight.rotateLeft();
		if (can_move(cur, left)) return left;
		Direction right = straight.rotateRight();
		if (can_move(cur, right)) return right;
		left = left.rotateLeft();
		if (can_move(cur, left)) return left;
		right = right.rotateRight();
		if (can_move(cur, right)) return right;
		return null;
	}

	private Direction bug_step(MapLocation cur, MapLocation dest, int wall) {
		Direction dir = fuzzy_step(cur, dest);
		if (dir != null && cur.add(dir).distanceSquaredTo(dest) < closest.distanceSquaredTo(dest)) {
		    bug_wall_dir = null;
			return dir;
		}

		// Ne moremo blizje, zato se drzimo zidu.
		// Drzimo se lahko leve ali desne strani: parameter 'wall'.
		if (bug_wall_dir == null)
			bug_wall_dir = cur.directionTo(dest);

        if (wall == LEFT_WALL) {
        	// V smeri urinega kazalca
            Direction right = bug_wall_dir;
            for (int i = 0; i < 8; ++i) {
            	if (can_move(cur, right)) {
					MapLocation wall_loc = cur.add(right.rotateLeft());
					bug_wall_dir = cur.add(right).directionTo(wall_loc);
            		return right;
				}
            	right = right.rotateRight();
			}
		} else {
			// Nasprotna smer urinega kazalca
			Direction left = bug_wall_dir;
			for (int i = 0; i < 8; ++i) {
				if (can_move(cur, left)) {
					MapLocation wall_loc = cur.add(left.rotateRight());
					bug_wall_dir = cur.add(left).directionTo(wall_loc);
					return left;
				}
				left = left.rotateLeft();
			}
		}

		// To se lahko zgodi samo, ce je obkoljen ...
		return null;
	}

	private Object[] bug_step_simulate(MapLocation cur, MapLocation dest, int wall, int steps) {
		// Vrne [0]: direction po prvem koraku
		// 	    [1]: wall dir po prvem koraku
		//      [2]: wall dir po zadnjem koraku
		//      [3]: koncna lokacija
		Object[] result = new Object[4];

		MapLocation prev_closest = closest;
		Direction prev_bug_wall_dir = bug_wall_dir;

		MapLocation end = cur;
		for (int i = 0; i < steps; ++i) {
			Direction dir = bug_step(end, dest, wall);
			if (i == 0) {
				result[0] = dir;
				result[1] = bug_wall_dir;
			}
			if (dir == null) break;

			end = end.add(dir);
			if (end.distanceSquaredTo(dest) < closest.distanceSquaredTo(dest)) {
				closest = end;
			}
			rc.setIndicatorDot(end, (255 / steps) * i, wall * 100, 255);
		}

		result[2] = bug_wall_dir;
		result[3] = end;

		bug_wall_dir = prev_bug_wall_dir;
		closest = prev_closest;

		return result;
	}

	private boolean exists_straight_path(MapLocation cur, MapLocation dest) {
		Direction dir = cur.directionTo(dest);
		while (!cur.equals(dest)) {
			if (!can_move(cur, dir)) return false;
			cur = cur.add(dir);
		}
		return true;
	}

	private boolean exists_fuzzy_path(MapLocation cur, MapLocation dest, int max_steps) {
		Direction dir = fuzzy_step(cur, dest);
		for (int steps = 0; dir != null && !cur.equals(dest) && steps < max_steps; ++steps) {
			if (!can_move(cur, dir)) return false;
			cur = cur.add(dir);
			dir = fuzzy_step(cur, dest);
		}
		return cur.equals(dest);
	}

	private Direction tangent_bug(MapLocation dest) {
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
    		Direction dir = fuzzy(tangent_shortcut);
    		if (can_move(cur, dir)) return dir;
			// Zgubili smo se ali pa je ovira ...
			tangent_shortcut = null;
			bug_wall_tangent = NO_WALL;
			bug_wall_dir = null;
		}

    	// Stran zidu je ze izbrana
		// Simularmo pot z izbranim zidom
		if (bug_wall_tangent != NO_WALL) {
			// bug_step(cur, dest, bug_wall_tangent);
			Object[] simulation = bug_step_simulate(cur, dest, bug_wall_tangent, LOOKAHEAD_STEPS);
			MapLocation shortcut = (MapLocation) simulation[3];
			if (exists_fuzzy_path(cur, shortcut, LOOKAHEAD_STEPS - 1)) {
				tangent_shortcut = shortcut;
				bug_wall_dir = (Direction) simulation[2];
				return fuzzy(tangent_shortcut);
			}
			bug_wall_dir = (Direction) simulation[1];
			return (Direction) simulation[0];
		}

		// Odlocimo se med levo in desno stranjo
        Object[] left_simulation = bug_step_simulate(cur, dest, LEFT_WALL, LOOKAHEAD_STEPS);
		Object[] right_simulation = bug_step_simulate(cur, dest, RIGHT_WALL, LOOKAHEAD_STEPS);
		MapLocation left_pos = (MapLocation) left_simulation[3];
		MapLocation right_pos = (MapLocation) right_simulation[3];

		int d1 = right_pos.distanceSquaredTo(dest);
		int d2 = left_pos.distanceSquaredTo(dest);
		if (d1 <= d2) {
			bug_wall_tangent = RIGHT_WALL;
			if (exists_fuzzy_path(cur, right_pos, LOOKAHEAD_STEPS - 1)) {
				tangent_shortcut = right_pos;
				bug_wall_dir = (Direction) right_simulation[2];
				return fuzzy(tangent_shortcut);
			}
			bug_wall_dir = (Direction) right_simulation[1];
			return (Direction) right_simulation[0];
		} else {
			bug_wall_tangent = LEFT_WALL;
			if (exists_fuzzy_path(cur, left_pos, LOOKAHEAD_STEPS - 1)) {
				tangent_shortcut = left_pos;
				bug_wall_dir = (Direction) left_simulation[2];
				return fuzzy(tangent_shortcut);
			}
			bug_wall_dir = (Direction) left_simulation[1];
			return (Direction) left_simulation[0];
		}
	}

	public Direction get_move_direction(MapLocation dest) {
		MapLocation cur = rc.getLocation();
		if (cur.isAdjacentTo(dest)) return null;

		if (!dest.equals(goal)) {
		    goal = dest;
		    closest = cur;
		    bug_wall_dir = null;
		} else {
			if (cur.distanceSquaredTo(dest) < closest.distanceSquaredTo(dest)) {
				closest = cur;
			}
		}
		// fuzzy(goal);
		// tangent_bug(dest);
		// Direction dir = bug_step(cur, dest, RIGHT_WALL);

		if (tangent_shortcut != null)
			rc.setIndicatorDot(tangent_shortcut, 255, 0, 0);

		/*
		MapLocation tmp = bug_step_simulate(cur, dest, LEFT_WALL, LOOKAHEAD_STEPS);
		if (tmp != null)
			rc.setIndicatorDot(tmp, 255, 0, 255);
		 */

		Direction dir = tangent_bug(dest);
		return dir;
	}
}

class minerPathfind {
	miner m;
	RobotController rc;
	MapLocation goal = null;
	int[][] history;// lokacije kjer smo ze bili 0- nismo 1-smo enkrat 2- morali smo iti nazaj
	int[] shift = { 0, 1, -1, 2, -2, 3, -3, 4 };// odmiki smeri
	ArrayList<MapLocation> path;

	minerPathfind(miner m) {
		this.m = m;
		this.rc = m.rc;
		int h = rc.getMapHeight();
		int w = rc.getMapWidth();
		history = new int[w][];
		history[0] = new int[h];
		for (int i = 1; i < w; i++) {
			history[i] = history[0].clone();
		}
		path = new ArrayList<MapLocation>();
	}

	void refresh() {
		System.out.println("reset");
		history[0] = new int[rc.getMapHeight()];
		for (int i = 1; i < rc.getMapWidth(); i++) {
			history[i] = history[0].clone();
		}
	}
	/**
	 * Po meritvah stane nekje od 400-800 bytecoda. 
	 * @param m
	 * @throws GameActionException
	 */
	void moveTowards(MapLocation m) throws GameActionException {
		if (goal == null || !goal.equals(m)) {
			goal = m;
			refresh();
			path.add(rc.getLocation());
		}
		if(m==rc.getLocation()) {
			return;
		}
//		for (int i = 0; i < rc.getMapWidth(); i++) {
//			for (int j = 0; j < rc.getMapHeight(); j++) {
//				if (history[i][j] != 0) {
//					rc.setIndicatorDot(new MapLocation(i, j), 255, 0, 0);
//				}
//			}
//		}
//		for (MapLocation mm : path) {
//			rc.setIndicatorDot(mm, 255, 255, 255);
//		}
		MapLocation curr = rc.getLocation();
		history[curr.x][curr.y]=1;
		Direction aim = curr.directionTo(goal);
		for (int i : shift) {
			Direction aim2 = Util.rotate(aim, i);
			MapLocation next = curr.add(aim2);
			// zaradi performanca predpostavimo da to polje vidimo in je že skenirano
			if (rc.canMove(aim2) && !rc.senseFlooding(next) && history[next.x][next.y] == 0) {
				rc.move(aim2);
//				System.out.println(aim2.dx+" "+aim2.dy);
				path.add(next);
				return;
			}
		}
		//backtracking
//		System.out.println("Gremo nazaj do "+path.get(path.size() - 2));
		Direction d = curr.directionTo(path.get(path.size() - 2));
		if (rc.canMove(d)) {
			rc.move(d);
			path.remove(path.size() - 1);
		}
//		System.out.println("Nic nismo nasli");

	}
}

public class miner extends robot {
	public static final int MINER_COST = RobotType.MINER.cost;

	MapLocation goal;// kam hoce miner priti
	minerPathFinder path_finder;
	MapCell[][] mapData;
	int w, h;// dimenzije mape

	MapLocation hq_location;

	Set<MapLocation> surovine = new HashSet<>();

	Direction move_direction;

	public miner(RobotController rc) {
		super(rc);
		path_finder = new minerPathFinder(rc);
	}

	@Override
	public void init() throws GameActionException {
		goal = new MapLocation(0, 28);
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		mapData = new MapCell[w][];
		mapData[0] = new MapCell[h];
		for (int i = 1; i < w; i++) {
			mapData[i] = mapData[0].clone();
		}

		for(RobotInfo r:rc.senseNearbyRobots(2,rc.getTeam())){
			if(r.type==RobotType.HQ){
				hq_location=r.location;
			}
		}
		move_direction=rc.getLocation().directionTo(hq_location).opposite();// Miner gre stran od HQ

        // Test blockchaina
		/*
		for (int i = 1; i < rc.getRoundNum(); ++i) {
			b.read_next_round();
		}
		*/

		scan(1000);//kar lahko
	}

	@Override
	public void precompute() throws GameActionException {
		b.checkQueue();
	}

	@Override
	public void runTurn() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}
		int t=Clock.getBytecodesLeft();
		Direction dir = path_finder.get_move_direction(goal);
		if (dir != null)
			rc.move(dir);
		System.out.println(t-Clock.getBytecodesLeft()+" cena.");
	}

	public void postcompute() throws GameActionException {
		scan(2);//vsa sosednja polja

		while (Clock.getBytecodesLeft() > 800) {
			b.read_next_round();
		}
	}

	@Override
	public void bc_surovina(MapLocation pos) {
		System.out.println("BC SUROVINA: " + pos);
		surovine.add(pos);
	}

	@Override
	public void bc_drone(MapLocation from, MapLocation to) {
	    System.out.println("BC DRONE: " + from + " " + to);
	}

	public void initialScan() throws GameActionException {
		scan(10000);
	}

	public void scan(int a) throws GameActionException {
		int range = Math.min(a,rc.getCurrentSensorRadiusSquared());
		int x = rc.getLocation().x;
		int y = rc.getLocation().y;
		for (int qq = 0; qq <= range; qq++) {
			MapLocation[] q = pc.range[qq];
			int i = q.length;
			while (i-- > 0) {
				MapLocation mm = new MapLocation(q[i].x + x, q[i].y + y);
				if (rc.onTheMap(mm)) {
					if (rc.canSenseLocation(mm)) {
						mapData[mm.x][mm.y] = new MapCell(rc.senseSoup(mm), rc.sensePollution(mm), rc.senseElevation(mm), rc.senseFlooding(mm), rc.getID(), rc.senseRobotAtLocation(mm));
					} else {
						System.out.println("Robot ne more skenirati lokacije, ki pa je v dometu " + rc.getCurrentSensorRadiusSquared() + " " + rc.getLocation().distanceSquaredTo(mm));
					}
				}
			}
		}
	}
}
