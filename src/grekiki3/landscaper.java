package grekiki3;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class landscaper extends robot {
	int strategy = -1;
	boolean attacking=false;
	MapLocation hq=null;
	public landscaper(RobotController rc) {
		super(rc);

	}

	@Override
	public void init() throws GameActionException {
		for (RobotInfo r : rc.senseNearbyRobots(10, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq = r.location;
			}
		}
		if(hq==null) {
			attacking=true;
		}
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
		if(attacking) {
			//poišèemo najbližjo nasprotnikovo stavbo, jo zasipamo ali gremo do nje
			MapLocation eb=null;
			int dist=100000;
			for(RobotInfo r:rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
				if(r.getType()==RobotType.DESIGN_SCHOOL||r.getType()==RobotType.HQ||r.getType()==RobotType.FULFILLMENT_CENTER||r.getType()==RobotType.NET_GUN) {
					if(rc.getLocation().distanceSquaredTo(r.location)<dist) {
						dist=rc.getLocation().distanceSquaredTo(r.location);;
						eb=r.location;
					}
				}
			}
			for(Direction d:Util.dir) {
				RobotInfo r=rc.senseRobotAtLocation(rc.getLocation().add(d));
				if(r==null) {
					continue;
				}
				if(r.getType()==RobotType.DESIGN_SCHOOL||r.getType()==RobotType.HQ||r.getType()==RobotType.FULFILLMENT_CENTER||r.getType()==RobotType.NET_GUN) {
					if(r.getTeam()==rc.getTeam()) {
						if(rc.canDigDirt(d)) {
							rc.digDirt(d);
							return;
						}
					}else {
						if(rc.canDepositDirt(d)) {
							rc.depositDirt(d);
							return;
						}else {
							if(rc.canDigDirt(d.opposite())) {
								rc.digDirt(d.opposite());
								return;
							}
						}
					}
				}
			}
			Direction d=Util.tryMove(rc, rc.getLocation().directionTo(eb));
			if(d!=null) {
				rc.move(d);
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
