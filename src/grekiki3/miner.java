package grekiki3;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import grekiki21.pc;

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
	MapLocation goal;// kam hoce miner priti
	minerPathfind pth;// iskalnik poti
	RobotController rc;
	MapCell[][] mapData;
	int w, h;// dimenzije mape
	public static final int MINER_COST = RobotType.MINER.cost;

	public miner(RobotController rc) {
		super(rc);
		this.rc = rc;
	}

	@Override
	public void init() throws Exception {
		pth = new minerPathfind(this);
		goal = new MapLocation(0, 28);
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		mapData = new MapCell[w][];
		mapData[0] = new MapCell[h];
		for (int i = 1; i < w; i++) {
			mapData[i] = mapData[0].clone();
		}
		scan(1000);//kar lahko
	}

	@Override
	public void precompute() {

	}

	@Override
	public void runTurn() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}
//		int t=Clock.getBytecodesLeft();
		pth.moveTowards(goal);
//		System.out.println(t-Clock.getBytecodesLeft()+" cena.");

	}

	@Override
	public void postcompute() throws Exception {
		scan(2);//vsa sosednja polja
	}

	public void scan(int a) throws Exception {
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
