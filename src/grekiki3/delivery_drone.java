package grekiki3;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class delivery_drone extends robot{

	public delivery_drone(RobotController rc){
		super(rc);
		
	}

	@Override public void init() throws GameActionException {
		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	@Override public void precompute() throws GameActionException {
		b.checkQueue();
	}

	@Override public void runTurn(){
		

	}

	@Override public void postcompute() throws GameActionException {
		while (Clock.getBytecodesLeft() > 500) {
			if (!b.read_next_round()) {
				break;
			}
		}
	}

}
