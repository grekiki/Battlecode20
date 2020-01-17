package grekiki3;

import battlecode.common.*;

public class HQ extends robot {
	int w, h;// dimenzije mape
	MapLocation loc;// nasa lokacija
	/**
	 * Strategije: 1000-rush 2000-build a wall 3000-lanscape 4000-mapa je zelo cudna
	 * in potrebuje posebne ideje
	 */
	int strategy = -1;

	int miners_spawned = 0;
	int miners_alive = 0;

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
	public void runTurn() {
		if (!rc.isReady()) {
			return;
		}
		if (strategy == 1000) {

		}
		if (strategy == 2000) {
			// Na zacetku potrebujemo vsaj dva minerja. Vedno.
			if (miners_spawned < 1 ) {
				if (try_spawn_miner(pick_miner_direction()))
					return;
			}
		}
		if (strategy == 3000) {

		}

		if (try_shoot())
			return;
	}

	@Override
	public void postcompute() {

	}

	public int choose_strategy() {
		return 2000;
	}

	// Pomožne metode
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

	boolean try_shoot() {
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
			try {
				rc.shootUnit(closest.ID);
				return true;
			} catch (GameActionException e) {
				// e.printStackTrace();
				System.out.println("NE MOREM STRELJATI");
			}
		}
		return false;
	}
}
