package grekiki2;

import java.util.Arrays;

import battlecode.common.*;
import battlecode.world.GameWorld;
/*
 * Sifrirni sistem za sporocila. 
 * Prvi int je privatna cifra ki jo uporabimo za sifriranje. 
 * Drugi je tip sporocila.
 * msg[2]...msg[6] je vsebina
 * 
 * Tukaj bo slovar ki pove kaj dolocena stevilka pomeni ce je navedena kot vsebina
 * 
 * msg[1]==1--> msg[2] pove fazo v kateri je trenutno baza. 
 */
class tocka{
	int x,y;
	tocka(int a,int b){
		x=a;
		y=b;
	}
}
class Util{
	//Garantirano terminira
	public static Direction tryMoveLite(RobotController rc,Direction dir) {
		if(rc.canMove(dir)) {
			return dir;
		}else if(rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		}else if(rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		}else {
			return null;
		}
	}
	public static Direction tryMove(RobotController rc,Direction dir) {
		if(rc.canMove(dir)) {
			return dir;
		}else if(rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		}else if(rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		}else if(rc.canMove(dir.rotateLeft().rotateLeft())) {
			return dir.rotateLeft().rotateLeft();
		}else if(rc.canMove(dir.rotateRight().rotateRight())) {
			return dir.rotateRight().rotateRight();
		}else {
			return null;
		}
	}
}
abstract class robot {
	
	public abstract void init() throws Exception;

	public abstract void precompute() throws Exception;

	public abstract void runTurn() throws Exception;

	public abstract void postcompute() throws Exception;
	
}

public strictfp class RobotPlayer {
	static RobotController rc;

	public static void run(RobotController rci) {
		rc = rci;
		robot r = null;
		switch (rc.getType()) {
		case HQ:
			r = new HQ(rc);
			break;
		case MINER:
			r = new miner(rc);
			break;
		case REFINERY:
			r = new refinery(rc);
			break;
		case VAPORATOR:
			r = new vaporator(rc);
			break;
		case DESIGN_SCHOOL:
			r = new design_school(rc);
			break;
		case FULFILLMENT_CENTER:
			r = new fulfillment_center(rc);
			break;
		case LANDSCAPER:
			r = new landscaper(rc);
			break;
		case DELIVERY_DRONE:
			r = new delivery_drone(rc);
			break;
		case NET_GUN:
			r = new net_gun(rc);
			break;
		case COW:
			break;
		default:
			break;
		}
		try {
			r.init();
			while (true) {
				r.precompute();
				r.runTurn();
				r.postcompute();
				Clock.yield();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
