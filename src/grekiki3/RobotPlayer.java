package grekiki3;

import battlecode.common.*;

abstract class robot {
	RobotController rc;

	public robot(RobotController rc) {
		this.rc = rc;
	}
	/**
	 * Ta metoda se poklice ko se robot spawna. 
	 */
	public abstract void init();

	public abstract void precompute();

	public abstract void runTurn();

	public abstract void postcompute();
}

public strictfp class RobotPlayer {
	static RobotController rc;

	public static void run(RobotController rci) throws GameActionException {
		rc = rci;
		robot r = null;
		try {
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
		} catch (Exception e) {
			System.out.println(rc.getType() + " Exception");
			e.printStackTrace();
		}
		try {
			r.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (true) {
			try {
				r.precompute();
				r.runTurn();
				r.postcompute();
				Clock.yield();
			} catch (Exception e) {
				e.printStackTrace();
				Clock.yield();
			}

		}
	}

}
