package grekikiRush;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import battlecode.common.*;

class dronePathFinder extends BasePathFinder {
	dronePathFinder(RobotController rc) {
		super(rc);
		LOOKAHEAD_STEPS = 3;
		UNIT_MAX_WAIT = 1;
	}

	// Metoda se bo poklicala, ko naletimo na vodo.
	void found_water(MapLocation pos) {
	}

	void found_unit(RobotInfo robot) {
	}

	@Override
	protected boolean is_unit_obstruction(MapLocation at) {
		return super.is_unit_obstruction(at);
	}

	@Override
	boolean can_move(MapLocation from, Direction dir) throws GameActionException {
		// Ta metoda ignorira cooldown ...

		MapLocation to = from.add(dir);
		if (!rc.canSenseLocation(to))
			return false;
		if (rc.senseFlooding(to))
			found_water(to);
		RobotInfo robot = rc.senseRobotAtLocation(to);
		if (robot != null && robot.getID() != rc.getID()) {
			found_unit(robot);
			if (!ignore_units || robot.getType().isBuilding())
				return false;
		}
		return true;
	}
}

class minerPathFinder extends BasePathFinder {
	minerPathFinder(RobotController rc) {
		super(rc);
	}

	@Override
	boolean can_move(MapLocation from, Direction dir) throws GameActionException {
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

	public boolean moveTowards(MapLocation dest) throws GameActionException {
		Direction dir = get_move_direction(dest);

		if (dir != null && rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		return false;
	}
}

class miner extends robot {
	RobotController rc;
	int phase = 0;
	MapLocation hq;
	minerPathFinder path;
	ArrayList<MapLocation> polja;
	HashSet<MapLocation> surovine;
	ArrayList<MapLocation> refinery;
	ArrayList<MapLocation> turret;
	ArrayList<MapLocation> dronespawn;
	blockchain b;

	// phase 0
	boolean done;// ali smo konec z premikanjem v tej smeri
	Direction init;
	// phase 1

	// phase 2
	boolean rusher = false;

	miner(RobotController rc) {
		this.rc = rc;
	}

	public void readBlockchain() throws Exception {
		for (int i = 1; i < rc.getRoundNum(); i++) {
			readBlockchain(i);
		}
	}

	public void readBlockchain(int round) throws GameActionException {
		Transaction[] t = rc.getBlock(round);
		for (Transaction tt : t) {
			int[] msg = tt.getMessage();
			if (msg.length == 7) {
				if (msg[0] == konst.private_key) {
					if (msg[1] == 1) {// Sprememba faze
						int currentPhase = msg[2];
						phase = currentPhase;
						int id = msg[3];
						if (id == rc.getID()) {
							rusher = true;
						}
						System.out.println(phase + ". FAZA");
						if (phase == 3) {
							refinery.remove(hq);
						}
					} else if (msg[1] == 2) {
						MapLocation m = new MapLocation(msg[2], msg[3]);
						if (!polja.contains(m)) {
							polja.add(m);
						}
					} else if (msg[1] == 3) {
						MapLocation m = new MapLocation(msg[2], msg[3]);
						if (polja.contains(m)) {
							polja.remove(m);
						}
					} else if (msg[1] == 4) {
						MapLocation m = new MapLocation(msg[2], msg[3]);
						if (!refinery.contains(m)) {
							refinery.add(m);
						}
					} else if (msg[1] == 5) {
						MapLocation m = new MapLocation(msg[2], msg[3]);
						if (!turret.contains(m)) {
							turret.add(m);
						}
					}
				}
			}
		}
	}

	@Override
	public void init() throws Exception {
		path = new minerPathFinder(this.rc);
		surovine = new HashSet<MapLocation>();
		for (RobotInfo r : rc.senseNearbyRobots(2, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq = r.location;
			}
		}
		init = rc.getLocation().directionTo(hq).opposite();// Miner gre stran od HQ
		done = false;
		polja = new ArrayList<MapLocation>();
		b = new blockchain(rc);
		refinery = new ArrayList<MapLocation>();
		refinery.add(hq);
		turret = new ArrayList<MapLocation>();
		dronespawn = new ArrayList<MapLocation>();
		readBlockchain();
	}

	@Override
	public void precompute() throws Exception {
		if (rc.getRoundNum() > 1) {
			readBlockchain(rc.getRoundNum() - 1);
		}
	}

	@Override
	public void runTurn() throws Exception {
		if (!rc.isReady()) {
			return;
		}
		if (rusher) {
			if (runRush()) {
				return;
			}
		}
		if (!rusher && Util.d_inf(rc.getLocation(), hq) <= konst.min_base_dist_miner_suicide && phase == 3) {
			rc.disintegrate();
		}
		if (phase == 0 || phase == 1 || phase == 2 || phase == 3) {// TO-DO optimiziraj fazo 3.
			// Obdelamo potrebo po refineriji
//			if (surovine.size() > 0 && (rc.getTeamSoup() >= RobotType.REFINERY.cost + konst.refinery_build_buffer
//					|| (refinery.size() < konst.refinery_low_count
//							&& rc.getTeamSoup() >= RobotType.REFINERY.cost + konst.refinery_build_buffer2))) {
//				MapLocation closest = findClosest(surovine);
//				if (closest != null && rc.getLocation().distanceSquaredTo(closest) < konst.max_refinery_dist) {
//					// Preverimo èe je refinerija potrebna
//					MapLocation ref = closest;
//					if (Util.d_inf(hq, ref) >= konst.refinery_dist_hq) {// Ne na zidu... Da ne motimo baze
//						int dist = konst.min_ref_ref_dist;
//						for (MapLocation re : refinery) {
//							dist = Math.min(dist, re.distanceSquaredTo(ref));
//						}
//						if (dist == konst.min_ref_ref_dist) {// ni refinerije bližje kot 20
//							// Potrebujemo refinerijo.
//							// Poskusimo èe smo dovolj blizu
//							int sum = 0;
//							for (int i = 0; i <= konst.ref_soup_scan_range; i++) {
//								for (MapLocation mm : pc.range[i]) {
//									MapLocation m = new MapLocation(ref.x + mm.x, ref.y + mm.y);
//									if (rc.canSenseLocation(m)) {
//										sum += rc.senseSoup(m);
//									}
//								}
//							}
////							System.out.println(sum);
//							if (sum > konst.min_soup_nearby || refinery.size() < konst.refinery_low_count) {
//								if (ref.distanceSquaredTo(rc.getLocation()) <= 2) {
//									Direction d = rc.getLocation().directionTo(ref);
//									if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {
//										if (rc.canBuildRobot(RobotType.REFINERY, d)) {
//											rc.buildRobot(RobotType.REFINERY, d);
//											int[] msg = { konst.private_key, 4, rc.getLocation().x + d.dx,
//													rc.getLocation().y + d.dy, 0, 0, 0 };
//											b.sendMsg(new paket(msg, 1));
//											return;
//										}
//									}
//								} else {
//									Direction d = path.get_move_direction(ref);
//									if (d != null && rc.canMove(d)) {
//										rc.move(d);
//										return;
//									}
//								}
//							}
//						}
//					}
//				}
//			}

			// Najprej poskusimo èe smo polni
			if (rc.getSoupCarrying() == 100) {
				for (Direction d : Util.dir) {
					if (rc.canDepositSoup(d)) {
						rc.depositSoup(d, rc.getSoupCarrying());
						return;
					}
				}
				if (moveClosest(refinery)) {
					return;
				} else {
//					System.out.println("Ne moremo priti do najblizje refinerije!");
				}
			}
//			a: if (phase == 2 && rc.getTeamSoup() >= RobotType.NET_GUN.cost + konst.net_gun_build_buffer) {// Rafinerije
//																											// potrebujejo
//																											// drone za
//																											// obrambo.
//																											// Tovarna
//																											// vsaj 3
//																											// stran po
//																											// d_inf
//																											// metriki
//				MapLocation ref = findClosest(refinery);
//				if (ref.equals(hq)) {
//					break a;
//				}
//				int optRange = konst.net_gun_radius;
//				int dist = 1000000000;
//				for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam())) {
//					if (r.team == rc.getTeam() && r.type == RobotType.NET_GUN) {
//						if (rc.getLocation().distanceSquaredTo(r.location) < dist) {
//							dist = rc.getLocation().distanceSquaredTo(r.location);
//						}
//					}
//				}
//				if (dist < konst.dist_optrange_factor * optRange) {
//					break a;
//				}
//				if (optRange - 1 <= Util.d_inf(ref, rc.getLocation())
//						&& Util.d_inf(ref, rc.getLocation()) <= optRange + 1) {// Morda se da razdaljo spraviti na 3
//					for (Direction d : Util.dir) {
//						MapLocation op = rc.getLocation().add(d);
//						if (Util.d_inf(op, ref) == optRange) {
//							if (rc.canBuildRobot(RobotType.NET_GUN, d)) {
//								rc.buildRobot(RobotType.NET_GUN, d);
//								int[] msg = { konst.private_key, 5, rc.getLocation().x + d.dx,
//										rc.getLocation().y + d.dy, 0, 0, 0 };
//								b.sendMsg(new paket(msg, 1));
//								return;
//							}
//						}
//					}
//				}
//			}
			// Poskusimo kopati
			if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
				for (Direction d : Direction.allDirections()) {
					if (rc.canMineSoup(d)) {
						rc.mineSoup(d);
						return;
					}
				}
			}
			// Gremo do najbližje surovine
			if (surovine.size() > 0) {
				if (moveClosest(surovine)) {
					return;
				}
			}
			// Gremo do najbližjega polja
			if (polja.size() > 0) {
				if (moveClosest(polja)) {
					return;
				}
			}
			// Ce ni niè dela pametno raziskujemo
			Direction d = Util.tryMoveLite(rc, init);
			if (d == null) {
				init = Util.getRandomDirection();
			} else {
				rc.move(d);
				return;
			}
		} // phase>1
	}

	int fazaBaze = -1;
	MapLocation enemyHq = null;
	boolean lrsim = false;
	boolean udsim = false;

	boolean madeDS = false;
	boolean builtng = false;
	public boolean runRush() throws GameActionException {
		System.out.println("rush");
		if (enemyHq == null) {
			// najprej lr
			if (lrsim) {
				enemyHq = new MapLocation(rc.getMapWidth() - 1 - hq.x, hq.y);
			} else if (udsim) {
				enemyHq = new MapLocation(hq.x, rc.getMapHeight() - 1 - hq.y);
			} else {
				enemyHq = new MapLocation(rc.getMapWidth() - 1 - hq.x, rc.getMapHeight() - 1 - hq.y);
			}
		}
		boolean seeHq = false;
		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
			if (r.type == RobotType.HQ) {
				seeHq = true;
				enemyHq = r.location;
				int[] msg = { konst.private_key, 8, r.location.x, r.location.y, 0, 0, 0 };
				b.sendMsg(new paket(msg, 1));
			}
		}
		if (!seeHq) {
			path.moveTowards(enemyHq);
			return true;
		} else {
			if (!madeDS) {
				Direction d = rc.getLocation().directionTo(enemyHq);
				if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
					rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
					madeDS = true;
					return true;
				}
				if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d.rotateLeft())) {
					rc.buildRobot(RobotType.DESIGN_SCHOOL, d.rotateLeft());
					madeDS = true;
					return true;
				}
				if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d.rotateRight())) {
					rc.buildRobot(RobotType.DESIGN_SCHOOL, d.rotateRight());
					madeDS = true;
					return true;
				}
				if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d.rotateLeft().rotateLeft())) {
					rc.buildRobot(RobotType.DESIGN_SCHOOL, d.rotateLeft().rotateLeft());
					madeDS = true;
					return true;
				}
				if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d.rotateRight().rotateRight())) {
					rc.buildRobot(RobotType.DESIGN_SCHOOL, d.rotateRight().rotateRight());
					madeDS = true;
					return true;
				}
			} else {
				boolean seeEnemyDrone = false;
				for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
					if (r.type == RobotType.DELIVERY_DRONE) {
						seeEnemyDrone = true;
					}
				}
				if (seeEnemyDrone && !builtng) {
					Direction d = rc.getLocation().directionTo(enemyHq);
					if (rc.canBuildRobot(RobotType.NET_GUN, d)) {
						rc.buildRobot(RobotType.NET_GUN, d);
						builtng = true;
						return true;
					}
					if (rc.canBuildRobot(RobotType.NET_GUN, d.rotateLeft())) {
						rc.buildRobot(RobotType.NET_GUN, d.rotateLeft());
						builtng = true;
						return true;
					}
					if (rc.canBuildRobot(RobotType.NET_GUN, d.rotateRight())) {
						rc.buildRobot(RobotType.NET_GUN, d.rotateRight());
						builtng = true;
						return true;
					}
					if (rc.canBuildRobot(RobotType.NET_GUN, d.rotateLeft().rotateLeft())) {
						rc.buildRobot(RobotType.NET_GUN, d.rotateLeft().rotateLeft());
						builtng = true;
						return true;
					}
					if (rc.canBuildRobot(RobotType.NET_GUN, d.rotateRight().rotateRight())) {
						rc.buildRobot(RobotType.NET_GUN, d.rotateRight().rotateRight());
						builtng = true;
						return true;
					}
				}else {
					path.moveTowards(enemyHq);
				}
				return false;
			}
			return true;
		}
	}

	public MapLocation findClosest(Iterable<? extends MapLocation> somethings) throws GameActionException {
		MapLocation best = null;
		int dist = 64 * 64;
		for (MapLocation m : somethings) {
			int op = rc.getLocation().distanceSquaredTo(m);
			if (op < dist) {
				dist = op;
				best = m;
			}
		}
		return best;
	}

	public boolean moveClosest(Iterable<? extends MapLocation> somethings) throws GameActionException {
		MapLocation best = null;
		int dist = 64 * 64;
		for (MapLocation m : somethings) {
			int op = rc.getLocation().distanceSquaredTo(m);
			if (op < dist) {
				dist = op;
				best = m;
			}
		}
		if (best == null) {
			return false;
		}
		Direction d = path.get_move_direction(best);
		if (d != null && rc.canMove(d)) {
			rc.move(d);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void postcompute() throws Exception {
		if (rusher) {
			return;
		}
		int range = rc.getCurrentSensorRadiusSquared();
		if (Clock.getBytecodesLeft() < konst.clock_buffer) {
			return;
		}
		discardMissing();
		checkForEmptyField();
		findResources(range);
	}

	public void checkForEmptyField() throws GameActionException {
		int range = rc.getCurrentSensorRadiusSquared();
		for (MapLocation polje : polja) {
			if (range >= rc.getLocation().distanceSquaredTo(polje) + konst.resource_radius) {
				boolean ok = false;
				for (int i = 0; i <= konst.resource_radius; i++) {
					if (Clock.getBytecodesLeft() < konst.clock_buffer2) {
						System.out.println("Ni casa");
						return;
					}
					MapLocation[] scan = pc.range[i];
					for (MapLocation mm : scan) {
						MapLocation m = new MapLocation(polje.x + mm.x, polje.y + mm.y);
						if (rc.canSenseLocation(m) && !rc.senseFlooding(m) && rc.senseSoup(m) > 0
								&& rc.senseElevation(m) <= 3 + rc.senseElevation(rc.getLocation())) {
							ok = true;
							i = 1000;
							break;
						}
					}
				}
				if (!ok) {
					int[] msg = { konst.private_key, 3, polje.x, polje.y, 0, 0, 0 };
					b.sendMsg(new paket(msg, 1));
				}
			}
		}
	}

	public void findResources(int range) throws GameActionException {
		for (int i = 0; i < range; i++) {
			MapLocation[] scan = pc.range[i];
			if (Clock.getBytecodesLeft() < 1000) {
				System.out.println("Ni casa");
				return;
			}
			for (MapLocation mm : scan) {
				MapLocation m = new MapLocation(rc.getLocation().x + mm.x, rc.getLocation().y + mm.y);
				if (rc.canSenseLocation(m) && !rc.senseFlooding(m) && rc.senseSoup(m) > 0) {
					if (Clock.getBytecodesLeft() < konst.clock_buffer3) {
						System.out.println("Ni casa");
						return;
					}
					surovine.add(m);
					boolean used = false;
					for (MapLocation center : polja) {
						if (center.distanceSquaredTo(m) <= konst.resource_radius) {
							used = true;
							break;
						}
					}
					if (!used && rc.canSenseLocation(m.add(rc.getLocation().directionTo(m)))
							&& Math.abs(rc.senseElevation(m.add(rc.getLocation().directionTo(m)))
									- rc.senseElevation(rc.getLocation())) <= 3) {
						// Dodamo polje v blockchain
						MapLocation interpolacija_centra = m.add(rc.getLocation().directionTo(m));
						polja.add(interpolacija_centra);
						int[] msg = { konst.private_key, 2, interpolacija_centra.x, interpolacija_centra.y, 0, 0, 0 };
						b.sendMsg(new paket(msg, 1));
					}
				}
			}
		}
	}

	public void discardMissing() throws GameActionException {
		// Tako se brise iz seta. Vir
		// :https://stackoverflow.com/questions/1110404/remove-elements-from-a-hashset-while-iterating
		Iterator<MapLocation> iterator = surovine.iterator();
		while (iterator.hasNext()) {
			if (Clock.getBytecodesLeft() < konst.clock_buffer4) {
				System.out.println("Ni casa");
				return;
			}
			MapLocation m = iterator.next();
			if (rc.canSenseLocation(m) && rc.senseSoup(m) == 0) {
				iterator.remove();
			}
		}
	}
}

class landscaper extends robot {
	RobotController rc;
	MapLocation hq;
	int phase = 0;
	blockchain b;
	public static MapLocation[] place = { new MapLocation(0, -2), new MapLocation(0, 2), new MapLocation(-2, 0),
			new MapLocation(2, 0), new MapLocation(2, -2), new MapLocation(2, 2), new MapLocation(-2, 2),
			new MapLocation(-2, -2) };
	public static MapLocation[] place_inside = { new MapLocation(0, 1), new MapLocation(-1, 0), new MapLocation(1, 1),
			new MapLocation(-1, -1), new MapLocation(0, -1) };
	boolean internal_goal = false;
	MapLocation ig = null;
	MapLocation enemyHq=null;
	landscaper(RobotController rc) {
		this.rc = rc;
	}

	public void readBlockchain() throws Exception {
		for (int i = 1; i < rc.getRoundNum(); i++) {
			readBlockchain(i);
		}
	}

	public void readBlockchain(int round) throws GameActionException {
		Transaction[] t = rc.getBlock(round);
		for (Transaction tt : t) {
			int[] msg = tt.getMessage();
			if (msg.length == 7) {
				if (msg[0] == konst.private_key) {
					if (msg[1] == 8) {
						enemyHq=new MapLocation(msg[2],msg[3]);
					}
				}
			}
		}
	}

	@Override
	public void init() {
		for (RobotInfo r : rc.senseNearbyRobots(10, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq = r.location;
			}
		}
		b = new blockchain(rc);
		if(hq==null) {
			attacking=true;
		}
	}

	public Direction direction(int p) {
		if (place[p].y == 2) {
			return Direction.NORTH;
		} else if (place[p].y == -2) {
			return Direction.SOUTH;
		} else if (place[p].x == -2) {
			return Direction.WEST;
		} else if (place[p].x == 2) {
			return Direction.EAST;
		} else {
			System.out.println("NAPAKA!!!");
			return null;
		}
	}

	@Override
	public void precompute() throws GameActionException {
		readBlockchain(rc.getRoundNum()-1);
		b.checkQueue();
//		boolean ok = true;
//		for (MapLocation m : landscaper.place) {
//			MapLocation check = new MapLocation(hq.x + m.x, hq.y + m.y);
//			if (!rc.canSenseLocation(check) || rc.senseRobotAtLocation(check) == null
//					|| (rc.senseRobotAtLocation(check) != null
//							&& rc.senseRobotAtLocation(check).type != RobotType.LANDSCAPER)) {
//				ok = false;
//				break;
//			}
//		}
//		if (ok) {
//			phase = 3;
//		}
//		internal_goal = false;
//		for (MapLocation m : place_inside) {
//			MapLocation g = new MapLocation(hq.x + m.x, hq.y + m.y);
//			if (rc.canSenseLocation(g) && rc.senseRobotAtLocation(g) == null) {
//				internal_goal = true;
//				ig = g;
//				return;
//			}
//		}
	}

	int p = 0;
	boolean positioned = false;
	boolean attacking=false;
	@Override
	public void runTurn() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}
		if(attacking) {
			//poi��emo najbli�jo nasprotnikovo stavbo, jo zasipamo ali gremo do nje
			MapLocation eb=null;
			int dist=100000;
			for(RobotInfo r:rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
				if(r.getType()==RobotType.DESIGN_SCHOOL||r.getType()==RobotType.HQ||r.getType()==RobotType.FULFILLMENT_CENTER||r.getType()==RobotType.NET_GUN) {
					if(rc.getLocation().distanceSquaredTo(r.location)<dist) {
						dist=rc.getLocation().distanceSquaredTo(r.location);;
						eb=r.location;
					}
				}
			}
			for(Direction d:Util.dir) {
				RobotInfo r=rc.senseRobotAtLocation(rc.getLocation().add(d));
				if(r==null) {
					continue;
				}
				if(r.getType()==RobotType.DESIGN_SCHOOL||r.getType()==RobotType.HQ||r.getType()==RobotType.FULFILLMENT_CENTER||r.getType()==RobotType.NET_GUN) {
					if(r.getTeam()==rc.getTeam()) {
						if(rc.canDigDirt(d)) {
							rc.digDirt(d);
							return;
						}
					}else {
						if(rc.canDepositDirt(d)) {
							rc.depositDirt(d);
							return;
						}else {
							if(rc.canDigDirt(d.opposite())) {
								rc.digDirt(d.opposite());
								return;
							}
						}
					}
				}
			}
			Direction d=Util.tryMove(rc, rc.getLocation().directionTo(eb));
			if(d!=null) {
				rc.move(d);
			}
		}
		

	}

	@Override
	public void postcompute() throws GameActionException {

	}

}

class delivery_drone extends robot {
	RobotController rc;
	Direction init;
	MapLocation hq;
	MapLocation goal;
	int timeToReach;
	boolean full = false;
	ArrayList<MapLocation> aqua = new ArrayList<MapLocation>();
	MapLocation s, drain;
	int time = 1;
	int dist;
	dronePathFinder path;
	boolean working = true;

//	boolean[][]flooding;
	delivery_drone(RobotController rc) {
		this.rc = rc;
	}

	@Override
	public void init() {
		path = new dronePathFinder(this.rc);
		for (RobotInfo r : rc.senseNearbyRobots(10, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq = r.location;
			}
		}
		goal = hq;
		timeToReach = -1;
		dist = rc.getMapWidth() - 2 * (Math.min(hq.x, rc.getMapWidth() - 1 - hq.x));
		dist += rc.getMapHeight() - 2 * (Math.min(hq.y, rc.getMapHeight() - 1 - hq.y));

		if (dist < konst.dist_drone_limit) {
			if (hq.x < rc.getMapWidth()) {
				goal = new MapLocation(goal.x - konst.shift, goal.y);
			} else {
				goal = new MapLocation(goal.x + konst.shift, goal.y);

			}
			if (hq.y < rc.getMapHeight()) {
				goal = new MapLocation(goal.x, goal.y - konst.shift);
			} else {
				goal = new MapLocation(goal.x, goal.y + konst.shift);
			}
		}
		s = null;
		drain = null;
		init = rc.getLocation().directionTo(hq).opposite();
//		flooding=new boolean[rc.getMapWidth()][rc.getMapHeight()];
	}

	@Override
	public void precompute() throws GameActionException {
		if (s != null || drain != null) {
			return;
		}
		for (int i = rc.getRoundNum() - 1; i < rc.getRoundNum(); i++) {
			Transaction[] q = rc.getBlock(rc.getRoundNum() - 1);
//			System.out.println(Clock.getBytecodesLeft());
			for (Transaction t : q) {
				if (t.getMessage()[0] == konst.private_key && t.getMessage()[1] == 7) {
					s = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
					drain = new MapLocation(t.getMessage()[4], t.getMessage()[5]);
					time = rc.getRoundNum();
					return;
				}
			}
		}
	}

	@Override
	public void runTurn() throws GameActionException {
//		if (rc.getRoundNum() > 300 && rc.getRoundNum() < 1000) {
//			working = false;
//		} else {
//			working = true;
//		}
		if (!rc.isReady()) {
			return;
		}

//		if (dist < 25) {
//			if (rc.getRoundNum() > 2000) {
//				MapLocation rightdown = new MapLocation(rc.getMapWidth() - hq.x - 1, rc.getMapHeight() - hq.y - 1);
//				goal = rightdown;
//				timeToReach = 2100;
//			}
//		} else {
//			if (rc.getRoundNum() > 1600) {
//				goal = hq;
//				timeToReach = 1600;
//			} else if (rc.getRoundNum() > 1500) {
//				MapLocation down = new MapLocation(hq.x, rc.getMapHeight() - hq.y - 1);
//				goal = down;
//				timeToReach = 1600;
//			} else if (rc.getRoundNum() > 1300) {
//				MapLocation rightdown = new MapLocation(rc.getMapWidth() - hq.x - 1, rc.getMapHeight() - hq.y - 1);
//				goal = rightdown;
//				timeToReach = 1400;
//			} else if (rc.getRoundNum() > 1100) {
//				MapLocation right = new MapLocation(rc.getMapWidth() - hq.x - 1, hq.y);
//				goal = right;
//				timeToReach = 1200;
//			}
//		}
		full = rc.isCurrentlyHoldingUnit();
		System.out.println(s + " " + drain);
		a: if (s != null && !full) {
			if (rc.getRoundNum() - time > konst.abort_time) {
				s = null;
				drain = null;
			}
			if (rc.canSenseLocation(s)) {
				if (rc.senseRobotAtLocation(s) == null || rc.senseRobotAtLocation(s).type == RobotType.DELIVERY_DRONE) {
					s = null;
					drain = null;
					break a;
				} else {
					if (rc.getLocation().distanceSquaredTo(s) <= 2) {
						if (rc.canPickUpUnit(rc.senseRobotAtLocation(s).ID)) {
							rc.pickUpUnit(rc.senseRobotAtLocation(s).ID);
							full = true;
							s = null;
							return;
						}
					}
				}
			}
			Direction d = path.get_move_direction(s);
			if (d != null && safeMove(d)) {
				rc.move(d);
				return;
			} else {
				return;
			}
		}
		if (s == null && drain != null && full) {
			if (rc.getRoundNum() - time > konst.abort_time) {
				s = null;
				drain = null;
			}
//			System.out.println(drain.x+" "+drain.y);
			if (rc.getLocation().distanceSquaredTo(drain) <= 2) {
				if (rc.getLocation().distanceSquaredTo(drain) == 0) {
					for (Direction d : Util.dir) {
						if (rc.canMove(d) && safeMove(d)) {
							rc.move(d);
							return;
						}
					}
				} else {
					Direction d = path.get_move_direction(drain);
					if (rc.canDropUnit(d)) {
						rc.dropUnit(d);
						drain = null;
						return;
					}
				}
			}
			Direction d = path.get_move_direction(drain);
			if (d != null && rc.canMove(d) && safeMove(d)) {
				rc.move(d);
				return;
			}
			if (d != null && rc.canMove(d) && safeMove(d)) {
				rc.move(d);
				return;
			}
		}
		if (!full) {
//			System.out.println(Clock.getBytecodesLeft());
			int closest = 64 * 64;
			MapLocation best = null;
			RobotInfo[] rr = rc.senseNearbyRobots();
			for (RobotInfo r : rr) {
				if (r.team != rc.getTeam() && Util.d_inf(rc.getLocation(), r.location) < closest
						&& (r.type == RobotType.LANDSCAPER || r.type == RobotType.MINER || r.type == RobotType.COW)) {
					best = r.location;
					closest = Util.d_inf(rc.getLocation(), r.location);
				}
			}
			if (best != null) {
				if (closest <= 1) {
					if (rc.canPickUpUnit(rc.senseRobotAtLocation(best).ID)) {
						rc.pickUpUnit(rc.senseRobotAtLocation(best).ID);
						full = true;
						return;
					}
				} else {
					Direction d = path.get_move_direction(best);
					if (d != null && safeMove(d)) {
						rc.move(d);
						return;
					}
				}
			} else {
				if (working) {
					// ni dobrih opcij
					int dist = Util.d_inf(goal, rc.getLocation());
					int neBlizje = f(timeToReach);
//			System.out.println(dist+" "+neBlizje);
					if (dist < neBlizje || dist < (goal.equals(hq) ? konst.hq_min : konst.nhq_min)) {
						Direction d = Util.tryMoveLiteDrone(rc, rc.getLocation().directionTo(goal).opposite());
						if (d != null && safeMove(d)) {
							rc.move(d);
							return;
						}
					}
					if (dist > (goal.equals(hq) ? konst.hq_max : konst.nhq_max)) {
						Direction d = Util.tryMoveLiteDrone(rc, rc.getLocation().directionTo(goal));
						if (d != null) {
							rc.move(d);
							return;
						}
					}
					if ((goal.equals(hq) ? konst.hq_min : konst.nhq_min) <= dist
							&& dist <= (goal.equals(hq) ? konst.hq_max : konst.nhq_max)) {
						for (int i = 0; i < 10; i++) {
							Direction d = Util.getRandomDirection();
							if (rc.canMove(d) && safeMove(d)) {
								rc.move(d);
								return;
							}
						}
					}
				} else {
					for (int i = 0; i < 10; i++) {
						Direction d = Util.tryMoveLiteDrone(rc, init);
						if (d == null) {
							init = Util.getRandomDirection();
						} else if (rc.canMove(d) && safeMove(d)) {
							rc.move(d);
							return;
						}
					}
				}
			}
		}
		if (full && (drain == null || s != null)) {
			for (Direction d : Util.dir) {
				if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseFlooding(rc.getLocation().add(d))) {
					if (rc.canDropUnit(d)) {
						rc.dropUnit(d);
						full = false;
						return;
					}
				}
			}
			// Gremo do najbližje vode
			int range = Math.min(konst.range_init, rc.getCurrentSensorRadiusSquared());
//			int l=Clock.getBytecodesLeft();
			for (int i = 0; i < range; i++) {
				for (MapLocation m : pc.range[i]) {
					if (rc.canSenseLocation(m)) {
						if (rc.senseFlooding(m)) {
							Direction d = path.get_move_direction(m);
							if (d != null) {
								if (safeMove(d)) {
									rc.move(d);
									return;
								} else if (safeMove(d.rotateLeft())) {
									rc.move(d.rotateLeft());
									return;
								} else if (safeMove(d.rotateRight())) {
									rc.move(d.rotateRight());
									return;
								} else if (safeMove(d.rotateLeft().rotateLeft())) {
									rc.move(d.rotateLeft().rotateLeft());
									return;
								} else if (safeMove(d.rotateRight().rotateRight())) {
									rc.move(d.rotateRight().rotateRight());
									return;
								}
							}
						}
					}
				}
			}
//			System.out.println("Cena "+(l-Clock.getBytecodesLeft()));
			MapLocation best = findClosest(aqua);
			Direction d;
			if (best != null) {
				d = path.get_move_direction(best);
			} else {
				d = init;
			}
			for (int i = 0; i < 10; i++) {
				if (safeMove(d)) {
					rc.move(d);
					return;
				} else {
					init = Util.getRandomDirection();
				}
			}
		}
	}

	private int f(int timeToReach2) {
		// Vsaj tako dalec moramo biti
		int turn = rc.getRoundNum();
		if (turn > timeToReach2) {
			return 0;
		} else {
			return Math.min(konst.f_max, (int) Math.floor((timeToReach2 - turn) * konst.dist_factor));
		}
	}

	public boolean safeMove(Direction d) {
		if (d == null) {
			return false;
		}
		if (!rc.canMove(d)) {
			return false;
		}
		MapLocation end = rc.getLocation().add(d);
		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
			if ((r.type == RobotType.NET_GUN || r.type == RobotType.HQ) && r.team == rc.getTeam().opponent()) {
				if (end.distanceSquaredTo(r.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
					return false;
				}
			}
		}
		return true;
	}

	public MapLocation findClosest(Iterable<? extends MapLocation> somethings) throws GameActionException {
		MapLocation best = null;
		int dist = 64 * 64;
		for (MapLocation m : somethings) {
			int op = rc.getLocation().distanceSquaredTo(m);
			if (op < dist) {
				dist = op;
				best = m;
			}
		}
		return best;
	}

	@Override
	public void postcompute() throws Exception {
		if (rc.getRoundNum() > 1000) {
			return;
		}
		int range = rc.getCurrentSensorRadiusSquared();
		for (int i = 0; i < range && i < 20; i++) {
			for (MapLocation mmm : pc.range[i]) {
				if (Clock.getBytecodesLeft() < konst.clock_buffer5) {
					return;
				}
//				System.out.println(Clock.getBytecodesLeft());
				MapLocation m = new MapLocation(rc.getLocation().x + mmm.x, rc.getLocation().y + mmm.y);
				if (rc.canSenseLocation(m)) {
					if (rc.senseFlooding(m)) {
						boolean ok = true;
						for (MapLocation mm : aqua) {
							if (m.distanceSquaredTo(mm) < konst.dist_water) {
								ok = false;
								break;
							}
						}
						if (ok) {
							aqua.add(m);
						}
					}
				}
			}
		}
	}

}
