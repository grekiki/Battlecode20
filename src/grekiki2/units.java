package grekiki2;

import java.lang.reflect.Field;
import java.util.ArrayList;

import battlecode.common.*;

class miner extends robot {
	RobotController rc;
	ArrayList<MapLocation> surovine;
	int phase = 0;
	MapLocation hq;
	int[]hack;
	// phase 1
	boolean done;// ali smo konec z premikanjem v tej smeri
	Direction init;

	miner(RobotController rc) {
		this.rc = rc;
	}

	public void readBlockchain() throws Exception {
		int curr = rc.getRoundNum();
		for (int i = 1; i < curr; i++) {
			readBlockchain(i);
		}
	}
	public void readBlockchain(int round) throws GameActionException {
		Transaction[] t = rc.getBlock(round);
		for (Transaction tt : t) {
			int[] msg = tt.getMessage();
			if (msg.length == 7) {
				if (msg[0] == 123456789) {
					if (msg[1] == 1) {// Sprememba faze
						int currentPhase = msg[2];
						phase = currentPhase;
					}
				}
			}
		}
	}
	
	@Override
	public void init() throws Exception {
		surovine = new ArrayList<MapLocation>();
		readBlockchain();
		for (RobotInfo r : rc.senseNearbyRobots(2, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq = r.location;
			}
		}
		init = rc.getLocation().directionTo(hq).opposite();// Miner gre stran od HQ
		System.out.println(init);
		done = false;
	}

	@Override
	public void precompute() throws Exception {
		if(rc.getRoundNum()>1) {
			readBlockchain(rc.getRoundNum()-1);
		}
		System.out.println(hack);
	}

	@Override
	public void runTurn() throws Exception {
		if (!rc.isReady()) {
			return;
		}
		if (phase == 0) {
			if(!done) {
				Direction d=Util.tryMoveLite(rc,init);
				if(d==null) {
					done=true;
				}else {
					rc.move(d);
					return;
				}
			}else {
				System.out.println("Konec?!");
			}
			if(done) {
				
			}
		}
		
	}

	@Override
	public void postcompute() throws Exception {

	}

}

class landscaper extends robot {
	RobotController rc;

	landscaper(RobotController rc) {
		this.rc = rc;
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

class delivery_drone extends robot {
	RobotController rc;

	delivery_drone(RobotController rc) {
		this.rc = rc;
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