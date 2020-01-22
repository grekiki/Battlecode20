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

	int wallRadius;
	ArrayList<Integer> minerIds;
	boolean haveBaseBuilder = false;
	int builder = -1;
	boolean haveRusher = false;
	int rusher = -1;

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
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		loc = rc.getLocation();
		polja = new vector_set_gl();
		slaba_polja = new vector_set_gl();
		strategy = choose_strategy();
		minerIds = new ArrayList<Integer>();
		b.send_packet(b.BASE_STRATEGY, new int[] { b.PRIVATE_KEY, b.BASE_STRATEGY, strategy, 0, 0, 0, 0 });
		b.send_location(b.LOC_HOME_HQ, rc.getLocation());
	}

	@Override
	public void precompute() throws GameActionException {
		for (RobotInfo r : rc.senseNearbyRobots(2)) {
			if (r.team == rc.getTeam() && r.type == RobotType.MINER) {
				if (!minerIds.contains(r.ID)) {
					minerIds.add(r.ID);
				}
			}
		}

		if (strategy == 1000) {
			if (!haveRusher && minerIds.size() > 0) {
				haveRusher = true;
				b.send_packet(b.MINER_RUSH, new int[] { b.PRIVATE_KEY, b.MINER_RUSH, minerIds.get(0), 0, 0, 0, 0 });
			}
		} else if (strategy == 2000) {
			if (!haveBaseBuilder && minerIds.size() > 0) {
				haveBaseBuilder = true;
				b.send_packet(b.MINER_HELP_HQ, new int[] { b.PRIVATE_KEY, b.MINER_HELP_HQ, minerIds.get(0), 0, 0, 0, 0 });
			}
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
			if (this.miners_spawned <= 4 * polja.load && this.miners_spawned < 10)
				if (try_spawn_miner(pick_miner_direction()))
					return;

		}

	}

	@Override
	public void postcompute() throws GameActionException {
		if (strategy == 2000) {
			if (rc.getRoundNum() == 20) {
				b.send_location(b.BUILD_TOVARNA_DRONOV, loc.translate(1, 1));
			}
			if (rc.getRoundNum() == 50) {
				System.out.println("Poslano?!");
				b.send_location(b.BUILD_TOVARNA_LANDSCAPERJEV, loc.translate(2, -1));
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
