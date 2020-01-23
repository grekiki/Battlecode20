package grekiki3;

import battlecode.common.*;

public class fulfillment_center extends robot {
	int drones_built = 0;
	int drone_requests = 0;
	int strategy = -1;

	public fulfillment_center(RobotController rc) {
		super(rc);
	}

	@Override
	public void init() throws GameActionException {
		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	@Override
	public void precompute() throws GameActionException {
	}

	@Override
	public void runTurn() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}

		if (should_build()) {
			try_build();
		}
	}

	@Override
	public void postcompute() throws GameActionException {
		while (Clock.getBytecodesLeft() > 500) {
			if (!b.read_next_round()) {
				break;
			}
		}
	}

	@Override
	public void bc_drone(MapLocation from, MapLocation to, int id) {
		drone_requests++;
	}

	@Override
	public void bc_drone_complete(MapLocation from, MapLocation to, int id) {
		drone_requests--;
	}

	private boolean should_build() {
		if (strategy == 1000) {
			if (rc.getTeamSoup() < 1000) {// Ko imamo 1000 juhe lahko gradimo
				return false;
			}
		}
		if (strategy == 3000) {
			if (rc.getTeamSoup() < 300) {// Ko imamo 1000 juhe lahko gradimo
				return false;
			}
		}
		int team_soup = rc.getTeamSoup();
		boolean enough_soup = team_soup >= RobotType.DELIVERY_DRONE.cost+300;
		return enough_soup && ((drones_built < Math.min(2 + drone_requests/2,6)) || team_soup > 1000);
	}

	private boolean try_build() throws GameActionException {
		MapLocation netgun = null;
		for (RobotInfo r : rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent())) {
			if (r.type == RobotType.NET_GUN) {
				netgun = r.location;
			}
		}
		for (Direction d : Util.dir) {
			MapLocation ans = rc.getLocation().add(d);
			boolean ok = true;
			if (netgun != null) {
				if (netgun.distanceSquaredTo(ans) <= 15) {
					ok = false;
				}
			}
			if (ok && rc.canBuildRobot(RobotType.DELIVERY_DRONE, d)) {
				rc.buildRobot(RobotType.DELIVERY_DRONE, d);
				drones_built++;
				return true;
			}
		}
		return false;
	}

	public void bc_base_strategy(int[] message) {
		strategy = message[2];
	}
}
