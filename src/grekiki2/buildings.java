package grekiki2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import battlecode.common.*;

class paket {
	int[] data;
	int cost;

	paket(int[] a, int b) {
		data = a;
		cost = b;
	}
}

class HQ extends robot {
	RobotController rc;
	Direction[] dir = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };
	MapLocation curr;
	int h, w;
	int prevPhase = -1;
	int phase = 0;
	ArrayList<paket> send = new ArrayList<paket>();
	// phase 0
	Direction[] goodMiners;
	int miner = 0;

	HQ(RobotController rc) {
		this.rc = rc;
		curr = rc.getLocation();
		h = rc.getMapHeight();
		w = rc.getMapWidth();
	}

	public void computeData(int phase) throws GameActionException {
		if (phase == 0) {
			int count = 0;
			int lenLimit = 20;
			// A se splaca delavca poslati v to smer?
			boolean left = curr.x > lenLimit;
			boolean right = (w - curr.x) > lenLimit;
			boolean top = (h - curr.y) > lenLimit;
			boolean bot = curr.y > lenLimit;
			if (top && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.NORTH))) <= 3) {
				count++;
			}
			if (top && right && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.NORTHEAST))) <= 3) {
				count++;
			}
			if (right && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.EAST))) <= 3) {
				count++;
			}
			if (right && bot && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.SOUTHEAST))) <= 3) {
				count++;
			}
			if (bot && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.SOUTH))) <= 3) {
				count++;
			}
			if (bot & left && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.SOUTHWEST))) <= 3) {
				count++;
			}
			if (left && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.WEST))) <= 3) {
				count++;
			}
			if (left && top && Math.abs(rc.senseElevation(curr) - rc.senseElevation(curr.add(Direction.NORTHWEST))) <= 3) {
				count++;
			}
			goodMiners = new Direction[count];
//			System.out.println(top+" "+right+" "+bot+" "+left);
//			System.out.println("Nasteli smo "+count+" pozicij za minerje");
			count = 0;
			if (top) {
				goodMiners[count++] = Direction.NORTH;
			}
			if (top && right) {
				goodMiners[count++] = Direction.NORTHEAST;
			}
			if (right) {
				goodMiners[count++] = Direction.EAST;
			}
			if (right && bot) {
				goodMiners[count++] = Direction.SOUTHEAST;
			}
			if (bot) {
				goodMiners[count++] = Direction.SOUTH;
			}
			if (bot & left) {
				goodMiners[count++] = Direction.SOUTHWEST;
			}
			if (left) {
				goodMiners[count++] = Direction.WEST;
			}
			if (left && top) {
				goodMiners[count++] = Direction.NORTHWEST;
			}
		}
	}

	public void readBlockchain(int round) throws GameActionException {
		Transaction[] t = rc.getBlock(round);
		int minCost=1000000;
		for (int i=0;i<send.size();i++) {//Na zacetku igre je 0 zato ni neke skode
			paket p=send.get(i);
			for (Transaction tt : t) {
				if(Arrays.equals(tt.getMessage(),p.data)) {
					send.remove(p);
					i--;
				}
			}
		}
		
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
			minCost=Math.min(minCost, tt.getCost());
		}
		minCost=(minCost==1000000?1:minCost);
		for (int i=0;i<send.size();i++) {
			paket p=send.get(i);
			int[] msg = p.data;
			if(rc.canSubmitTransaction(msg, minCost)) {
				rc.submitTransaction(msg, minCost);
				send.remove(p);
				System.out.println("Poslano");
				i--;
			}
		}
		System.out.println(send.size()+" paketov caka");
	}

	@Override
	public void init() {

	}

	@Override
	public void precompute() throws GameActionException {
		if (prevPhase != phase) {
			try {
				computeData(phase);
			} catch (Exception e) {
				e.printStackTrace();
			}
			prevPhase = phase;
		}
		if(rc.getRoundNum()>1) {
			readBlockchain(rc.getRoundNum()-1);
		}
//		System.out.println(rc.getRoundNum() + " " + rc.getTeamSoup());
	}

	@Override
	public void runTurn() {
		try {
			if (!rc.isReady()) {
				return;
			}
			if (phase == 0) {
				if (rc.canBuildRobot(RobotType.MINER, goodMiners[miner])) {
					rc.buildRobot(RobotType.MINER, goodMiners[miner]);
					miner++;
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postcompute() throws GameActionException {
		if (phase == 0) {
			if (miner == goodMiners.length) {
				phase++;
				System.out.println("Faza 1!");
				int[] msg = new int[7];
				msg[0] = 123456789;
				msg[1] = 1;
				msg[2] = 1;
				if (rc.canSubmitTransaction(msg, 1)) {
					rc.submitTransaction(msg, 1);
					send.add(new paket(msg, 1));
				} else {
					send.add(new paket(msg, 0));
				}

			}
		}
		for(int x=-1;x<=1;x++) {
			for(int y=-1;y<=1;y++) {
				if(rc.canSenseLocation(new MapLocation(curr.x+x,curr.y+y))){
					if(rc.senseFlooding(new MapLocation(curr.x+x,curr.y+y))) {
						nukeServer();
					}
				}
			}
		}
	}

	/**
	 * To je metoda ki se uporablja le v skrajnem primeru da bi slucajno izgubljali
	 * in da je res pomembno da ne izgubimo.
	 * 
	 */
	public void nukeServer() {
		System.out.println(Clock.getBytecodeNum());
		String s = "aaaaaaaaaa";
		s = s + s + s + s + s + s + s + s + s + s;
		s = s + s + s + s + s + s + s + s + s + s;
		s = s + s + s + s + s + s + s + s + s + s;
		s = s + s + s + s + s + s + s + s + s + s;
		StringBuffer st2 = new StringBuffer("");
		for (int i = 0; i < 10; i++) {
			st2.append(s);
		}
		s = s + s + s + s + s + "b";
		while (true) {
			st2.append("a");
			System.out.println(st2.indexOf(s));
			System.out.println(Clock.getBytecodeNum());
		}
	}
}

class refinery extends robot {
	RobotController rc;

	public refinery(RobotController r) {
		rc = r;
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

class vaporator extends robot {
	RobotController rc;

	public vaporator(RobotController r) {
		rc = r;
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

class design_school extends robot {
	RobotController rc;

	public design_school(RobotController r) {
		rc = r;
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

class fulfillment_center extends robot {
	RobotController rc;

	public fulfillment_center(RobotController r) {
		rc = r;
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

class net_gun extends robot {
	RobotController rc;

	public net_gun(RobotController r) {
		rc = r;
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