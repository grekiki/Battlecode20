package grekiki2;

import battlecode.common.*;

class HQ extends robot {
	RobotController rc;
	Direction[] dir = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };
	MapLocation curr;
	int h, w;
	int prevPhase = -1;
	int phase = 0;
	// phase 0
	Direction[] goodMiners;
	int miner = 0;

	HQ(RobotController rc) {
		this.rc = rc;
		curr = rc.getLocation();
		h = rc.getMapHeight();
		w = rc.getMapWidth();
	}

	public void computeData(int phase) throws GameActionException {
		if (phase == 0) {
			int count = 0;
			int lenLimit = 20;
			// A se splača delavca poslati v to smer?
			boolean left = curr.x > lenLimit;
			boolean right = (w - curr.x) > lenLimit;
			boolean top = curr.y > lenLimit;
			boolean bot = (h - curr.y) > lenLimit;
			if (top && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.NORTH))) <= 3) {
				count++;
			}
			if (top && right && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.NORTHEAST))) <= 3) {
				count++;
			}
			if (right && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.EAST))) <= 3) {
				count++;
			}
			if (right && bot && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.SOUTHEAST))) <= 3) {
				count++;
			}
			if (bot && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.SOUTH))) <= 3) {
				count++;
			}
			if (bot & left && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.SOUTHWEST))) <= 3) {
				count++;
			}
			if (left && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.WEST))) <= 3) {
				count++;
			}
			if (left && top && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.NORTHWEST))) <= 3) {
				count++;
			}
			goodMiners = new Direction[count];
			count = 0;
			if (top) {
				goodMiners[count++] = Direction.NORTH;
			}
			if (top && right) {
				goodMiners[count++] = Direction.NORTHEAST;
			}
			if (right) {
				goodMiners[count++] = Direction.EAST;
			}
			if (right && bot) {
				goodMiners[count++] = Direction.SOUTHEAST;
			}
			if (bot) {
				goodMiners[count++] = Direction.SOUTH;
			}
			if (bot & left) {
				goodMiners[count++] = Direction.SOUTHWEST;
			}
			if (left) {
				goodMiners[count++] = Direction.WEST;
			}
			if (left && top) {
				goodMiners[count++] = Direction.NORTHWEST;
			}
		}
	}

	@Override
	public void init() {
		if (phase == 0) {
			if (prevPhase != phase) {
				try {
					computeData(phase);
				} catch (Exception e) {
					e.printStackTrace();
				}
				prevPhase = phase;
			}
			System.out.println(rc.getTeamSoup());
		}
	}

	@Override
	public void precompute() {

	}

	@Override
	public void runTurn() {
		try {
			if (!rc.isReady()) {
				return;
			}
			if (phase == 0) {
				if (rc.canBuildRobot(RobotType.MINER, goodMiners[miner])) {
					rc.buildRobot(RobotType.MINER, goodMiners[miner]);
					miner++;
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postcompute() {
		if(phase==0) {
			if(miner==goodMiners.length) {
				phase++;
			}
		}
	}

}

class refinery extends robot {
	RobotController rc;

	public refinery(RobotController r) {
		rc = r;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void runTurn() {
		// TODO Auto-generated method stub

	}

	@Override
	public void postcompute() {
		// TODO Auto-generated method stub

	}

}

class vaporator extends robot {
	RobotController rc;

	public vaporator(RobotController r) {
		rc = r;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void runTurn() {
		// TODO Auto-generated method stub

	}

	@Override
	public void postcompute() {
		// TODO Auto-generated method stub

	}

}

class design_school extends robot {
	RobotController rc;

	public design_school(RobotController r) {
		rc = r;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void runTurn() {
		// TODO Auto-generated method stub

	}

	@Override
	public void postcompute() {
		// TODO Auto-generated method stub

	}

}

class fulfillment_center extends robot {
	RobotController rc;

	public fulfillment_center(RobotController r) {
		rc = r;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void runTurn() {
		// TODO Auto-generated method stub

	}

	@Override
	public void postcompute() {
		// TODO Auto-generated method stub

	}

}

class net_gun extends robot {
	RobotController rc;

	public net_gun(RobotController r) {
		rc = r;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void runTurn() {
		// TODO Auto-generated method stub

	}

	@Override
	public void postcompute() {
		// TODO Auto-generated method stub

	}

}