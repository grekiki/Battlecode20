package grekiki3;

import java.util.ArrayList;

import battlecode.common.*;

public class HQ extends robot {
	int w, h;// dimenzije mape
	MapLocation loc;// nasa lokacija
	/**
	 * Strategije: <br>
	 * 1000-rush<br>
	 * 2000-build a wall<br>
	 * 3000-napadeni smo
	 */
	int strategy = -1;

	int miners_spawned = 0;
	vector_set_gl polja;
	vector_set_gl slaba_polja;
	minerPathFinder path_finder;

	int wallRadius;
	ArrayList<Integer> minerIds;
	boolean haveBaseBuilder = false;
	int builder = -1;
	boolean haveRusher = false;
	int rusher = -1;
	ArrayList<MapLocation> wall1;
	ArrayList<MapLocation> wall2;
	MapLocation landscaping;
	MapLocation drones;
	boolean wall = false;

	public HQ(RobotController rc) {
		super(rc);
	}

	/**
	 * HQ se v prvi potezi najprej odloci kaj bi naredil, glede na stanje mape.
	 *
	 * @throws GameActionException
	 */
	@Override
	public void init() throws GameActionException {
		path_finder = new minerPathFinder(rc);
		loc = rc.getLocation();
		wall1 = new ArrayList<MapLocation>();
		wall2 = new ArrayList<MapLocation>();
		for (int x = -2; x <= 2; x++) {
			for (int y = -2; y <= 2; y++) {
				if (x != 0 || y != 0) {
					int ax = Math.abs(x);
					int ay = Math.abs(y);
					if ((ax == 2 && ay == 0) || (ax == 0 && ay == 2)) {

					} else {
						if (Math.max(ax, ay) == 1) {
							if (rc.onTheMap(new MapLocation(loc.x + x, loc.y + y))) {
								wall1.add(new MapLocation(loc.x + x, loc.y + y));
							}
						} else {
							if (rc.onTheMap(new MapLocation(loc.x + x, loc.y + y))) {
								wall2.add(new MapLocation(loc.x + x, loc.y + y));
							}
						}
					}
				}
			}
		}
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		polja = new vector_set_gl();
		slaba_polja = new vector_set_gl();
		strategy = choose_strategy();
		minerIds = new ArrayList<Integer>();
		b.send_packet(b.BASE_STRATEGY, new int[] { b.PRIVATE_KEY, b.BASE_STRATEGY, strategy, 0, 0, 0, 0 });
		b.send_location(b.LOC_HOME_HQ, rc.getLocation());
		for (int x = -5; x < 5; x++) {
			for (int y = -5; y < 5; y++) {
				if ((x % 2 == 0 && y % 2 == 0) || (x + y < 5)) {

				} else {
					int px = x;
					int py = y;
					int h = rc.senseElevation(new MapLocation(rc.getLocation().x + px, rc.getLocation().y + py));
					if (!rc.senseFlooding(new MapLocation(rc.getLocation().x + px, rc.getLocation().y + py)) && Math.abs(h - rc.senseElevation(rc.getLocation())) <= 3) {
						if (landscaping == null && path_finder.exists_path(rc.getLocation(), new MapLocation(rc.getLocation().x + px, rc.getLocation().y + py))) {
							landscaping = new MapLocation(rc.getLocation().x + px, rc.getLocation().y + py);
						} else if (drones == null&&path_finder.exists_path(rc.getLocation(), new MapLocation(rc.getLocation().x + px, rc.getLocation().y + py))) {
							drones = new MapLocation(rc.getLocation().x + px, rc.getLocation().y + py);
						}
					}
				}
			}
		}
		System.out.println(drones + " " + landscaping);
	}

	@Override
	public void precompute() throws GameActionException {
		if (rc.getRoundNum() < 200) {
			for (RobotInfo r : rc.senseNearbyRobots(2)) {
				if (r.team == rc.getTeam() && r.type == RobotType.MINER) {
					if (!minerIds.contains(r.ID)) {
						minerIds.add(r.ID);
					}
				}
			}
		}
		if (rc.getRoundNum() < 200 && strategy != 1000) {
			for (RobotInfo r : rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent())) {
				if (r.type == RobotType.MINER) {
					strategy = 3000;
					b.send_packet(b.BASE_STRATEGY, new int[] { b.PRIVATE_KEY, b.BASE_STRATEGY, strategy, 0, 0, 0, 0 });
					if (!haveBaseBuilder && minerIds.size() > 0) {
						haveBaseBuilder = true;
						b.send_packet(b.MINER_HELP_HQ, new int[] { b.PRIVATE_KEY, b.MINER_HELP_HQ, minerIds.get(0), 0, 0, 0, 0 });
					}
				}
			}
		}
		if (strategy == 1000) {
			if (!haveRusher && minerIds.size() > 0) {
				haveRusher = true;
				rusher = minerIds.get(0);
				b.send_packet(b.MINER_RUSH, new int[] { b.PRIVATE_KEY, b.MINER_RUSH, minerIds.get(0), 0, 0, 0, 0 });
			}
		} else if (strategy == 2000) {
			if (!haveBaseBuilder && minerIds.size() > 0) {
				haveBaseBuilder = true;
				builder = minerIds.get(0);
				b.send_packet(b.MINER_HELP_HQ, new int[] { b.PRIVATE_KEY, b.MINER_HELP_HQ, minerIds.get(0), 0, 0, 0, 0 });
			}
		}
		boolean wall_full = true;
		for (MapLocation m : wall1) {
			if (rc.canSenseLocation(m)) {
				RobotInfo r = rc.senseRobotAtLocation(m);
				if (r != null && r.team == rc.getTeam() && r.type == RobotType.LANDSCAPER) {

				} else {
					wall_full = false;
				}
			}
		}
		if (wall_full) {
			wall = true;
		}
	}

	@Override
	public void runTurn() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}
		if (try_shoot()) {
			return;
		}

		if (strategy == 1000) {
			if (this.miners_spawned < 3)
				if (try_spawn_miner(pick_miner_direction()))
					return;
		}
		if (strategy == 2000) {
			if (((this.miners_spawned <= 4 * polja.load && rc.getTeamSoup() > 700) || this.miners_spawned <= 2 * polja.load) && this.miners_spawned < 10)
				if (try_spawn_miner(pick_miner_direction()))
					return;

		}
		if (strategy == 3000) {
			if (this.miners_spawned < 3)
				if (try_spawn_miner(pick_miner_direction()))
					return;
		}

	}

	static boolean built_defensive_ls = false;

	@Override
	public void postcompute() throws GameActionException {
		if (strategy == 2000) {
			if (rc.getRoundNum() == 10) {
				b.send_location(b.BUILD_TOVARNA_DRONOV, drones);
				System.out.println(drones + " droni?");
			}
			if (rc.getRoundNum() == 50) {
				b.send_location(b.BUILD_TOVARNA_LANDSCAPERJEV, landscaping);
				System.out.println(landscaping + " diggerji?");
			}
		}
		if (strategy == 3000) {
			if (!built_defensive_ls) {
				b.send_location(b.BUILD_TOVARNA_LANDSCAPERJEV, new MapLocation(rc.getLocation().x, rc.getLocation().y - 2));
//				b.send_location(b.BUILD_TOVARNA_DRONOV, new MapLocation(rc.getLocation().x,rc.getLocation().y-1));
				built_defensive_ls = true;
			}
			if(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length==0) {
				strategy=2000; 
				b.send_packet(b.BASE_STRATEGY, new int[] { b.PRIVATE_KEY, b.BASE_STRATEGY, strategy, 0, 0, 0, 0 });
			}
		}
		while (Clock.getBytecodesLeft() > 500) {
			if (!b.read_next_round()) {
				break;
			}
		}
	}

	public int choose_strategy() {
		wallRadius = 2;
		if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) {
			return 1000;// rush
		} else {
			return 2000;
		}
	}

	// Pomozne metode
	Direction pick_miner_direction() {
		return Util.getRandomDirection();
//		return Direction.SOUTH;
	}

	boolean try_spawn_miner(Direction dir) {
		if (rc.getTeamSoup() >= miner.MINER_COST && rc.canBuildRobot(RobotType.MINER, dir)) {
			try {
				rc.buildRobot(RobotType.MINER, dir);
				miners_spawned++;
				return true;
			} catch (GameActionException e) {
				// e.printStackTrace();
			}
		}
		return false;
	}

	boolean try_shoot() throws GameActionException {
		RobotInfo closest = null;
		int dis = 1000000;
		for (RobotInfo r : rc.senseNearbyRobots(net_gun.SHOOT_RADIUS, rc.getTeam().opponent())) {
			if (r.getType() == RobotType.DELIVERY_DRONE) {
				int t = rc.getLocation().distanceSquaredTo(r.location);
				if (t < dis) {
					dis = t;
					closest = r;
				}
			}
		}
		if (closest != null && rc.canShootUnit(closest.ID)) {
			rc.shootUnit(closest.ID);
			return true;
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
}
