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
	 * 3000-lanscape<br>
	 * 4000-mapa je zelo cudna in potrebuje posebne ideje
	 */
	int strategy = -1;

	int miners_spawned = 0;
	int miners_alive = 0;

	int wallRadius;
	ArrayList<Integer> minerIds;
	boolean haveBaseBuilder = false;
	int builder = -1;

	public HQ(RobotController rc) {
		super(rc);
	}

	/**
	 * HQ se v prvi potezi najprej odloci kaj bi naredil, glede na stanje mape.
	 */
	@Override
	public void init() {
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		loc = rc.getLocation();
		strategy = choose_strategy();
		minerIds = new ArrayList<Integer>();
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
		if (!haveBaseBuilder && minerIds.size() > 0) {
			haveBaseBuilder=true;
			b.send_packet(b.MINER_HELP_HQ, new int[] { b.PRIVATE_KEY, b.MINER_HELP_HQ, minerIds.get(0), 0, 0, 0, 0 });
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

		// testiranje dronov
		if (rc.getRoundNum() == 20) {
			b.send_location(b.BUILD_TOVARNA_DRONOV, loc.translate(2, -1));
		}

		if (strategy == 1000) {

		}
		if (strategy == 2000) {
			// Na zacetku potrebujemo vsaj dva minerja. Vedno.
			if (this.miners_spawned <= 5)
				if (try_spawn_miner(pick_miner_direction()))
					return;

		}
		if (strategy == 3000) {

		}

	}

	@Override
	public void postcompute() {

	}

	public int choose_strategy() {
		wallRadius = 2;
		if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) {
			return 1000;
		} else {
			return 2000;
		}
	}

	// Pomozne metode
	Direction pick_miner_direction() {
		// TODO doloci najboljso smer (proti surovinam)
		return Direction.SOUTH;
	}

	boolean try_spawn_miner(Direction dir) {
		if (rc.getTeamSoup() >= miner.MINER_COST && rc.canBuildRobot(RobotType.MINER, dir)) {
			try {
				rc.buildRobot(RobotType.MINER, dir);
				miners_spawned++;
				miners_alive++;
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
}
