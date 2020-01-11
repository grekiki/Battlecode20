package grekiki1;

import battlecode.common.Direction;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

class HQ extends robot{
	int round;
	RobotController rc;
	int miners=0;
	int diggers=0;
	HQ(RobotController rc){
		this.rc=rc;
		round=rc.getRoundNum();
	}
	@Override public void precompute(){

	}
	@Override public void runTurn(){
		try{
			round++;
			int soup=rc.getTeamSoup();
			if(rc.isReady()) {
				for(RobotInfo u:rc.senseNearbyRobots()) {
					if(u.team==rc.getTeam().opponent()&&u.type==RobotType.DELIVERY_DRONE&&rc.canShootUnit(u.ID)) {
						rc.shootUnit(u.ID);
						return;
					}
				}
			}
			if(miners<10&&soup>=70&&rc.isReady()){
				for(Direction d:Direction.allDirections()){
					if(rc.canBuildRobot(RobotType.MINER,d)){
						rc.buildRobot(RobotType.MINER,d);
						miners++;
						return;
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	@Override public void postcompute(){

	}
}
