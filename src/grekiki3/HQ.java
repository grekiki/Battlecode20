package grekiki3;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import grekiki20.pc;

public class HQ extends robot{
	int w,h;//dimenzije mape
	MapLocation loc;//nasa lokacija
	
	
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
		
	}

	@Override public void postcompute(){

	}

}
