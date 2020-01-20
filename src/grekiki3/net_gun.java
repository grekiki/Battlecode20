package grekiki3;

import battlecode.common.*;

public class net_gun extends robot{
	public static int SHOOT_RADIUS = GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED;

	public net_gun(RobotController rc){
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

	@Override public void runTurn() throws GameActionException {
		if(!rc.isReady()){
			return;
		}
		RobotInfo closest=null;
		int dist=1000000;
		for(RobotInfo r:rc.senseNearbyRobots()){
			if(r.team==rc.getTeam().opponent()&&r.getType()== RobotType.DELIVERY_DRONE){
				int t=rc.getLocation().distanceSquaredTo(r.location);
				if(t<dist){
					dist=t;
					closest=r;
				}
			}
		}
		if(closest==null){
			return;
		}
		if(rc.canShootUnit(closest.ID)){
			rc.shootUnit(closest.ID);
		}
	}

	@Override public void postcompute() throws GameActionException {
		while (Clock.getBytecodesLeft() > 500) {
			if (!b.read_next_round()) {
				break;
			}
		}
	}

}
