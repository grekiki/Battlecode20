package grekiki3;

import battlecode.common.*;

abstract class robot {
	RobotController rc;
	blockchain b;

	public robot(RobotController rc) {
		this.rc = rc;
		this.b = new blockchain(rc) {
			@Override
			public void handle_location(int type, MapLocation pos) {
				System.out.println("" + type + " " + pos);
			    switch (type) {
					case LOC_SUROVINA: bc_surovina(pos); break;
					case LOC_POLJE: bc_polje(pos); break;
					case LOC_RAFINERIJA: bc_rafinerija(pos); break;
					case LOC_TOVARNA_DRONOV: bc_tovarna_dronov(pos);
					case LOC_HOME_HQ: bc_home_hq(pos);
				}
			}
		};
	}
	/**
	 * Ta metoda se poklice ko se robot spawna. 
	 */
	public abstract void init() throws GameActionException;

	public abstract void precompute() throws GameActionException;

	public abstract void runTurn() throws GameActionException;

	public abstract void postcompute() throws GameActionException;

	// bc_* metode se sprozijo, ko iz blockchaina preberemo ustrezen tip podatka.
	// Roboti implementirajo samo tiste metode, ki jih zelijo sprejeti.
	// Za branje blockchaina se uporabi: b.read_next_round();
	public void bc_surovina(MapLocation pos) {}
	public void bc_polje(MapLocation pos) {}
	public void bc_rafinerija(MapLocation pos) {}
	public void bc_tovarna_dronov(MapLocation pos) {}
	public void bc_home_hq(MapLocation pos) {}
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
