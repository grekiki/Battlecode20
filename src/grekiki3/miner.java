package grekiki3;

import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;

public class miner extends robot{
	public static final int MINER_COST = RobotType.MINER.cost;

	MapLocation hq_location;

	Set<MapLocation> surovine = new HashSet<>();

	Direction move_direction;

	public miner(RobotController rc){
		super(rc);
	}

	@Override public void init() throws GameActionException {
		for(RobotInfo r:rc.senseNearbyRobots(2,rc.getTeam())){
			if(r.type==RobotType.HQ){
				hq_location=r.location;
			}
		}
		move_direction=rc.getLocation().directionTo(hq_location).opposite();// Miner gre stran od HQ

		System.out.println(hq_location);

		for (int i = 1; i < rc.getRoundNum(); ++i) {
			b.read_next_round();
		}
	}

	@Override public void precompute() throws GameActionException {
		b.checkQueue();
	}

	@Override public void runTurn() throws GameActionException {
	    // test blockchaina
	    for (int i = 0; i < 10; ++i) {
			b.send_location(blockchain.LOC_SUROVINA, new MapLocation(rc.getRoundNum(), 0));
		}
	}

	@Override public void postcompute() throws GameActionException {
	    while (Clock.getBytecodesLeft() > 800) {
			b.read_next_round();
		}
	}

	@Override
	public void bc_surovina(MapLocation pos) {
		System.out.println("BC SUROVINA: " + pos);
	    surovine.add(pos);
	}
}
