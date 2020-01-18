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

	public MapLocation goal;// kam hoce miner priti
	minerPathfind pth;// iskalnik poti
	RobotController rc;
	int w, h;// dimenzije mape

	MapLocation hq_location;

	public miner(RobotController rc) {
		super(rc);
		this.rc = rc;
	}

	@Override
	public void init() throws GameActionException {
		pth = new minerPathfind(this);
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
		// Ce smo polni gremo do baze
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			goal = hq_location;
			if (tryDepositSoup()) {
				return;
			} else {
				if (pth.moveTowards(goal)) {
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
			goal=Util.closest(rc.senseNearbySoup(), rc.getLocation());
		}
		
//		int t=Clock.getBytecodesLeft();
		if (goal != null) {
			rc.setIndicatorDot(goal, 0, 255, 255);
			pth.moveTowards(goal);
		}else {
			//Nakljucno raziskovanje ker ni cilja
			Direction explore=Util.rotateLeft(rc.getLocation().directionTo(hq_location),3);
			if(rc.canMove(explore)) {
				rc.move(explore);
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

	public boolean tryDepositSoup() throws GameActionException {
		for (Direction d : Util.dir) {
			if (rc.canDepositSoup(d)) {
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
