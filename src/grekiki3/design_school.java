package grekiki3;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class design_school extends robot {
	int strategy = -1;

	boolean attacking = false;
	MapLocation hq = null;

	public design_school(RobotController rc) {
		super(rc);
		for (RobotInfo r : rc.senseNearbyRobots(10, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq = r.location;
			}
		}
		if (hq == null) {
			attacking = true;
		}
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
	public void precompute() {

	}

	@Override
	public void runTurn() throws GameActionException {
		if (attacking) {
			if (rc.getTeamSoup() > RobotType.NET_GUN.cost) {
				for (Direction d : Util.dir) {
					if (rc.canBuildRobot(RobotType.LANDSCAPER, d)) {
						rc.buildRobot(RobotType.LANDSCAPER, d);
					}
				}
			}
		}
	}

	@Override
	public void postcompute() {

	}

	public void bc_base_strategy(int[] message) {
		strategy = message[2];
	}
}
