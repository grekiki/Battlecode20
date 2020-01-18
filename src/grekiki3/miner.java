package grekiki3;

import java.util.ArrayList;

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
	// 0 : naravnost
	// > 0 : desno
	// < 0 : levo
	private static int[] directions = { 0, -1, 1, -2, 2 };
	private static final int NO_WALL = 0;  // Ne sledi zidu.
	private static final int LEFT_WALL = 1;  // Zid je na levi.
	private static final int RIGHT_WALL = 2;
	private static final int LOOKAHEAD_STEPS = 5;

	private RobotController rc;

	private MapLocation goal;
	private MapLocation closest;  // Uporablja se pri bug navigation.
	private Direction bug_wall_dir;
	private int bug_wall_tangent = NO_WALL;
	private MapLocation tangent_shortcut;

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

	private MapLocation bug_step_simulate(MapLocation cur, MapLocation dest, int wall, int steps) {
		MapLocation prev_closest = this.closest;
		Direction prev_bug_wall_dir = this.bug_wall_dir;

		MapLocation end = cur;
		for (int i = 0; i < steps; ++i) {
			Direction dir = bug_step(end, dest, wall);
			end = end.add(dir);
			if (end.distanceSquaredTo(dest) < closest.distanceSquaredTo(dest)) {
				closest = end;
			}
		}

		this.bug_wall_dir = prev_bug_wall_dir;
		this.closest = prev_closest;

		return end;
	}

	private boolean exists_straight_path(MapLocation cur, MapLocation dest) {
		Direction dir = cur.directionTo(dest);
		while (!cur.equals(dest)) {
			if (!can_move(cur, dir)) return false;
			cur = cur.add(dir);
		}
		return true;
	}

	// TODO NE DELA
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
			// Naj bi obstajala ravna pot do tam ...?
    		Direction dir = cur.directionTo(tangent_shortcut);
    		if (can_move(cur, dir)) return dir;
			// Zgubili smo se
			tangent_shortcut = null;
			bug_wall_tangent = NO_WALL;
		}

    	// Stran zidu je ze izbrana
		// Simularmo pot z izbranim zidom
		if (bug_wall_tangent != NO_WALL) {
			// bug_step(cur, dest, bug_wall_tangent);
			MapLocation shortcut = bug_step_simulate(cur, dest, bug_wall_tangent, LOOKAHEAD_STEPS);
			if (exists_straight_path(cur, shortcut)) {
				tangent_shortcut = shortcut;
				return cur.directionTo(tangent_shortcut);
			}
			return bug_step(cur, dest, bug_wall_tangent);
		}

		// Odlocimo se med levo in desno stranjo
		MapLocation left_pos = bug_step_simulate(cur, dest, LEFT_WALL, LOOKAHEAD_STEPS);
		MapLocation right_pos = bug_step_simulate(cur, dest, RIGHT_WALL, LOOKAHEAD_STEPS);

		int d1 = right_pos.distanceSquaredTo(dest);
		int d2 = left_pos.distanceSquaredTo(dest);
		if (d1 <= d2) {
			bug_wall_tangent = RIGHT_WALL;
			if (exists_straight_path(cur, right_pos)) {
				tangent_shortcut = right_pos;
				return cur.directionTo(tangent_shortcut);
			}
		} else {
			bug_wall_tangent = LEFT_WALL;
			if (exists_straight_path(cur, left_pos)) {
				tangent_shortcut = left_pos;
				return cur.directionTo(tangent_shortcut);
			}
		}
		return bug_step(cur, dest, bug_wall_tangent);
	}
	
	private Direction smart_step(MapLocation dest) {
		//Predpostavimo da je source==rc.getLocation();
		int range=rc.getCurrentSensorRadiusSquared();
		int poteze=0;
		MapLocation curr=rc.getLocation();
		while(true) {
			int requiredRange=rc.getLocation().distanceSquaredTo(curr.add(rc.getLocation().directionTo(curr)));
			if(requiredRange>range) {
				break;
			}
			Direction d=curr.directionTo(bug_step_simulate(curr,dest,2,1));
			if(d==null) {
				break;
			}
			MapLocation next=curr.add(d);
			rc.setIndicatorDot(next, 255, 0, 0);
			curr=next;
			poteze++;
		}
		MapLocation curr2=rc.getLocation();
		int count2=0;
		Direction init=null;
		while(curr2!=curr&&count2<poteze) {
			Direction d=fuzzy_step(curr2,curr);
			if(init==null) {
				init=d;
			}
			count2++;
			curr2=curr2.add(d);
		}
		if(init!=null&&count2<poteze) {
			System.out.println("Optimizacija");
			return init;
		}else {
			return null;
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
//		Direction dirdemo=smart_step(dest);
		Direction dir = bug_step(cur, dest, RIGHT_WALL);
//		for(int i=0;i<2;i++) {
//			rc.setIndicatorDot(bug_step_simulate(rc.getLocation(),dest,RIGHT_WALL,i+1), 0,255,0);
//		}
//		System.out.println(dirdemo+" "+dir);
		// Direction dir = tangent_bug(dest);
		return dir;
	}

	public boolean moveTowards(MapLocation dest) throws GameActionException {
		Direction dir = get_move_direction(dest);
		
		if (dir != null) {
			rc.move(dir);
			return true;
		}
		return false;
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
		System.out.println("Reset pathing to "+goal);
		history[0] = new int[rc.getMapHeight()];
		for (int i = 1; i < rc.getMapWidth(); i++) {
			history[i] = history[0].clone();
		}
	}

	/**
	 * Po meritvah stane nekje od 400-800 bytecoda.
	 * 
	 * @param mm
	 * @throws GameActionException
	 */
	boolean moveTowards(MapLocation mm) throws GameActionException {
		if (goal == null || !goal.equals(mm)) {
			goal = mm;
			refresh();
			path.add(rc.getLocation());
		}
		if (Util.d_inf(mm,rc.getLocation())<=1) {
			goal = null;
			m.goal=null;//Naj miner najde nov cilj, to smo nasli
		}
		if(path.size()>10) {
			history[path.get(0).x][path.get(0).y]=0;
			path.remove(0);
		}
		rc.setIndicatorLine(rc.getLocation(), goal, 255, 0, 0);
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
		history[curr.x][curr.y] = 1;
		Direction aim = curr.directionTo(goal);
		for (int i : shift) {
			Direction aim2 = Util.rotateLeft(aim, i);
			MapLocation next = curr.add(aim2);
			// zaradi performanca predpostavimo da to polje vidimo in je že skenirano
			if (rc.canMove(aim2) && !rc.senseFlooding(next) && history[next.x][next.y] == 0) {
				rc.move(aim2);
//				System.out.println(aim2.dx+" "+aim2.dy);
				path.add(next);
				return true;
			}
		}
		// backtracking
//		System.out.println("Gremo nazaj do "+path.get(path.size() - 2));
		Direction d = curr.directionTo(path.get(path.size() - 2));
		if (rc.canMove(d)) {
			rc.move(d);
			path.remove(path.size() - 1);
			return true;
		}else {
			return false;
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

	public miner(RobotController rc) {
		super(rc);
		path_finder = new minerPathFinder(rc);
	}

	@Override
	public void init() throws GameActionException {
//		goal = new MapLocation(0, 28);
		w = rc.getMapWidth();
		h = rc.getMapHeight();

		for (RobotInfo r : rc.senseNearbyRobots(2, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq_location = r.location;
			}
		}

		// Test blockchaina
		for (int i = 1; i < rc.getRoundNum(); ++i) {
			if (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
				b.read_next_round();
			} else {
				System.out.println("Zmanjkalo casa za blockchain pri potezi " + b.rounds_read);
			}
		}
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
		System.out.println(goal);
		// Ce smo polni gremo do baze
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			goal = hq_location;
			if (tryDepositSoup()) {
				goal=null;
				return;
			} else {
				if (path_finder.moveTowards(goal)) {
					return;
				}
			}
		}
		if (tryMine()) {
			goal=null;//nasli smo surovino, zato jo izbrisemo da se ne bi kaj zaciklalo ce se sprazne
			return;
		}
		//Ce ni dela gremo po najblizjo surovino
		if (goal == null) {
			MapLocation[]soupLocations=rc.senseNearbySoup();
			goal=Util.closest(soupLocations, rc.getLocation());
		}
		
//		int t=Clock.getBytecodesLeft();
		if (goal != null) {
//			rc.setIndicatorDot(goal, 0, 255, 255);
			path_finder.moveTowards(goal);
		}else {
			//Nakljucno raziskovanje ker ni cilja
			MapLocation explore=Util.randomPoint(h,w);
			Direction d=path_finder.get_move_direction(explore);
			if(d!=null&&rc.canMove(d)) {
				rc.move(d);
				return;
			}
		}
//		System.out.println(t-Clock.getBytecodesLeft()+" cena.");

		// test blockchaina
//		for(int i=0;i<3;++i){
//			b.send_location(blockchain.LOC_SUROVINA,new MapLocation(rc.getRoundNum(),0));
//			b.send_location2(blockchain.LOC2_DRONE,new MapLocation(rc.getRoundNum(),0),new MapLocation(0,rc.getRoundNum()));
//		}
	}

	public void postcompute() throws GameActionException {
		while (Clock.getBytecodesLeft() > 800) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	//Util
	public boolean tryDepositSoup() throws GameActionException {
		for (Direction d : Util.dir) {
			if (rc.canDepositSoup(d)&&rc.senseRobotAtLocation(rc.getLocation().add(d)).team==rc.getTeam()) {
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
	public void bc_surovina(MapLocation pos) {
		System.out.println("BC SUROVINA: " + pos);
	}

	@Override
	public void bc_drone(MapLocation from, MapLocation to) {
		System.out.println("BC DRONE: " + from + " " + to);
	}

}
