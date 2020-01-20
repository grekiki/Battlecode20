package grekiki3;

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
	}

	@Override
	public void precompute() {

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
        if (rc.getRoundNum() < 20) {
        	b.send_location(b.LOC_TOVARNA_DRONOV, loc.translate(2, -1));
		}
		if (rc.getRoundNum() % 30 == 0) {
			b.send_location2(b.LOC2_DRONE, Util.randomPoint(h, w), Util.randomPoint(h, w));
		}

		if (strategy == 1000) {

		}
		if (strategy == 2000) {
			// Na zacetku potrebujemo vsaj dva minerja. Vedno.
			if (this.miners_spawned <= 5||Math.random()<2)
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
