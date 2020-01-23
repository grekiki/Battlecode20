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
	MapLocation hq = null;
	boolean attacking=false;
	int spawned=0;
	public design_school(RobotController rc) {
		super(rc);
	}

	@Override
	public void init() throws GameActionException {
		for (RobotInfo r : rc.senseNearbyRobots(10, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq = r.location;
			}
		}
		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
		if (hq == null&&strategy==1000) {
			attacking = true;
		}
		

	}

	@Override
	public void precompute() {

	}

	@Override
	public void runTurn() throws GameActionException {
		if (strategy==2000) {
			if (rc.getTeamSoup() > 550||(rc.getTeamSoup()>RobotType.NET_GUN.cost&&spawned<6)) {
				for (Direction d : Util.dir) {
					if (rc.canBuildRobot(RobotType.LANDSCAPER, d)) {
						rc.buildRobot(RobotType.LANDSCAPER, d);
						spawned++;
					}
				}
			}
		}else if(strategy==1000) {
			
		}else if(strategy==3000) {
			if(rc.getTeamSoup()>RobotType.LANDSCAPER.cost&&spawned<5) {
				for (Direction d : Util.dir) {
					if (rc.canBuildRobot(RobotType.LANDSCAPER, d)) {
						rc.buildRobot(RobotType.LANDSCAPER, d);
						spawned++;
					}
				}
			}
		}
	}

	@Override
	public void postcompute() throws GameActionException {
		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	public void bc_base_strategy(int[] message) {
		strategy = message[2];
	}
	@Override
	public void bc_home_hq(MapLocation pos) {
	    hq = pos;
	}
}
