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

abstract class DroneTask {
	delivery_drone drone;
	MapLocation destination;
	boolean is_running = false;

	String reason = "BREZ DELA";  // Za debug

	DroneTask(delivery_drone drone) {
	    this.drone = drone;
	}

	public abstract boolean run() throws GameActionException;

	public boolean is_running() {
		return is_running;
	}
}

class MoveDroneTask extends DroneTask {

	MoveDroneTask(delivery_drone drone, MapLocation dest) {
		super(drone);
		this.destination = dest;
	}

	@Override
	public boolean run() throws GameActionException {
		is_running = true;
	    return drone.path_finder.moveTowards(destination);
	}

	@Override
	public String toString() {
		return String.format("%s: --> %s", reason, destination);
	}
}

public class delivery_drone extends robot{
	MapLocation hq_location;

	Set<MapLocation> water_locations = new HashSet<>();
	Set<MapLocation> enemy_netguns = new HashSet<>();
	Set<MapLocation> delivery_locations = new HashSet<>();

	DroneTask task;
	dronePathFinder path_finder;

	public delivery_drone(RobotController rc){
		super(rc);
		
	}

	@Override public void init() throws GameActionException {
		path_finder = new dronePathFinder(rc) {
			@Override
			void found_water(MapLocation pos) {
				on_find_water(pos);
			}
		};

		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	@Override public void precompute() throws GameActionException {
		b.checkQueue();
	}

	@Override public void runTurn() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}
		task = find_best_task();
		if (task != null) {
		    task.run();
		}
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

	private void on_find_water(MapLocation pos) {
	    rc.setIndicatorDot(pos,255,255,255);
		water_locations.add(pos);
	}

	private DroneTask find_best_task() {
		if (!this.task.is_running()) {
			return new MoveDroneTask(this, Util.randomPoint(rc.getMapHeight(), rc.getMapWidth()));
		}
		return null;
	}
}
