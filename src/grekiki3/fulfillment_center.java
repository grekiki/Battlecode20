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

	private boolean should_build() {
		if (strategy == 1000) {
			if (rc.getTeamSoup() < 1000) {// Ko imamo 1000 juhe lahko gradimo
				return false;
			}
		}
		boolean enough_soup = rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost;
		return enough_soup && (drones_built < 5+drone_requests);
	}

	private boolean try_build() throws GameActionException {
		for (Direction d : Util.dir) {
			if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, d)) {
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
