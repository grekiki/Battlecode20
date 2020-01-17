package grekiki3;

import battlecode.common.*;
import grekiki20.pc;

public class HQ extends robot{
	int w,h;//dimenzije mape
	MapLocation loc;//nasa lokacija

	int miners_spawned = 0;
	int miners_alive = 0;

	public HQ(RobotController rc){
		super(rc);
	}
	/**
	 * HQ se v prvi potezi najprej odloci kaj bi naredil, glede na stanje mape.
	 */
	@Override public void init(){
		w=rc.getMapWidth();
		h=rc.getMapHeight();
		loc=rc.getLocation();
		//Ce vidimo nasprotnikovo bazo (!)
		if(rc.senseNearbyRobots(-1,rc.getTeam().opponent()).length>0) {
			
		}
	}

	@Override public void precompute(){

	}

	@Override public void runTurn(){
		if(!rc.isReady()){
			return;
		}

		// Na zacetku potrebujemo vsaj dva minerja. Vedno.
		if (miners_spawned < 2) {
			if (try_spawn_miner()) return;
		}

        if (try_shoot()) return;
	}

	@Override public void postcompute(){

	}

	Direction pick_miner_direction() {
		// TODO doloci najboljso smer (proti surovinam)

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
		RobotInfo closest=null;
		int dis=1000000;
		for(RobotInfo r:rc.senseNearbyRobots(net_gun.SHOOT_RADIUS, rc.getTeam().opponent())){
			if(r.getType()== RobotType.DELIVERY_DRONE){
				int t=rc.getLocation().distanceSquaredTo(r.location);
				if(t<dis){
					dis=t;
					closest=r;
				}
			}
		}
		if(closest!=null&&rc.canShootUnit(closest.ID)){
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
