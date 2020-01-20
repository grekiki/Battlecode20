package grekiki3;

import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;

class dronePathFinder extends BasePathFinder {
	dronePathFinder(RobotController rc) {
		super(rc);
	}

	// Metoda se bo poklicala, ko naletimo na vodo.
	void found_water(MapLocation pos) { }

	@Override
	boolean can_move(MapLocation from, Direction dir) throws GameActionException {
		// Ta metoda ignorira cooldown ...

		MapLocation to = from.add(dir);
		if (!rc.canSenseLocation(to))
			return false;
		if (rc.senseFlooding(to))
			found_water(to);
		RobotInfo robot = rc.senseRobotAtLocation(to);
		if (robot != null && robot.getID() != rc.getID() && (!ignore_units || robot.getType().isBuilding()))
			return false;
		return true;
	}
}

class DroneTask {
	MapLocation destination;
	String reason = "BREZ DELA";  // Za debug
}

public class delivery_drone extends robot{
	MapLocation hq_location;

	Set<MapLocation> water_locations = new HashSet<>();
	Set<MapLocation> enemy_netguns = new HashSet<>();

	DroneTask task;
	dronePathFinder path_finder;

	public delivery_drone(RobotController rc){
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

	@Override public void runTurn(){
		

	}

	@Override public void postcompute() throws GameActionException {
		while (Clock.getBytecodesLeft() > 500) {
			if (!b.read_next_round()) {
				break;
			}
		}
	}

	@Override
	public void bc_drone(MapLocation from, MapLocation to) {

	}

	@Override
	public void bc_water(MapLocation pos) {
	    water_locations.add(pos);
	}

	@Override
	public void bc_enemy_netgun(MapLocation pos) {
	    enemy_netguns.add(pos);
	}
}
