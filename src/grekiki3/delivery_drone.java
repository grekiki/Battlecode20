package grekiki3;

import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;

abstract class dronePathFinder extends BasePathFinder {
	dronePathFinder(RobotController rc) {
		super(rc);
		LOOKAHEAD_STEPS = 3;
		UNIT_MAX_WAIT = 1;
	}

	// Metoda se bo poklicala, ko naletimo na vodo.
	void found_water(MapLocation pos) throws GameActionException { }
	void found_unit(RobotInfo robot) throws GameActionException { }
	abstract boolean is_dangerous(MapLocation pos) throws GameActionException;

	@Override
	protected boolean is_unit_obstruction(MapLocation at) {
	    // TODO
		return super.is_unit_obstruction(at);
	}

	@Override
	boolean can_move(MapLocation from, Direction dir) throws GameActionException {
		// Ta metoda ignorira cooldown ...

		MapLocation to = from.add(dir);
		if (!rc.canSenseLocation(to))
			return false;
		if (rc.senseFlooding(to))
			found_water(to);
		// TODO kaj ce je ujet
		if (is_dangerous(to)) {
			return false;
		}
		RobotInfo robot = rc.senseRobotAtLocation(to);
		if (robot != null && robot.getID() != rc.getID()) {
		    found_unit(robot);
			if (!ignore_units || robot.getType().isBuilding())
				return false;
		}
		return true;
	}
}

abstract class DroneTask {
	delivery_drone drone;
	MapLocation destination;
	boolean is_running = false;
	boolean is_complete = false;

	String reason = "BREZ DELA";  // Za debug

	DroneTask(delivery_drone drone) {
	    this.drone = drone;
	}

	public abstract boolean run() throws GameActionException;

	public void on_start() {
		is_running = true;
		is_complete = false;
	}

	public void on_complete() {
		is_running = false;
		is_complete = true;
	}

	public boolean is_running() {
		return is_running;
	}

	public boolean is_complete() {
		return is_complete;
	}

	public void debug() {
		if (destination != null) {
			drone.rc.setIndicatorLine(drone.rc.getLocation(), destination, 255, 255, 0);
			System.out.println(this);
		}
	}
}

class MoveDroneTask extends DroneTask {

	MoveDroneTask(delivery_drone drone, MapLocation dest) {
		super(drone);
		this.destination = dest;
		this.reason = "MoveDroneTask";
	}

	@Override
	public boolean run() throws GameActionException {
	    if (!is_running) {
			on_start();
		}
		boolean move = drone.path_finder.moveTowards(destination);
	    if (drone.path_finder.is_at_goal(drone.rc.getLocation(), destination)) {
	        on_complete();
		}
	    return move;
	}

	@Override
	public String toString() {
		return String.format("%s: --> %s", reason, destination);
	}
}

class DeliverDroneTask extends DroneTask {
	MapLocation source;
	MoveDroneTask to_source_task;
	MoveDroneTask to_destination_task;

	DeliverDroneTask(delivery_drone drone, MapLocation from, MapLocation to) {
		super(drone);

		this.reason = "DeliverDroneTask";
		this.source = from;
		this.destination = to;
	}

	@Override
	public boolean run() throws GameActionException {
		if (!is_running) {
			on_start();
		}

		boolean move = false;
		if (!to_source_task.is_complete()) {
			move = to_source_task.run();
		} else if (!to_destination_task.is_complete()) {
			move = to_destination_task.run();
		}
		return move;
	}

	@Override
	public void on_start() {
		super.on_start();
		to_source_task = new MoveDroneTask(drone, source) {
			@Override
			public void on_complete() {
				super.on_complete();
				on_arrive_source(source);
			}
		};
	}

	public void on_arrive_source(MapLocation source) {
	    to_destination_task = new MoveDroneTask(drone, destination) {
			@Override
			public void on_start() {
				super.on_start();
			}

			@Override
			public void on_complete() {
				super.on_complete();
				DeliverDroneTask.this.on_complete();
			}
		};
	}

	@Override
	public String toString() {
		return String.format("%s: %s --> %s", reason, source, destination);
	}
}

public class delivery_drone extends robot{
	MapLocation hq_location;

	Set<MapLocation> water_locations = new HashSet<>();
	Set<MapLocation> enemy_netguns = new HashSet<>();
	Set<MapLocationPair> delivery_locations = new HashSet<>();

	DroneTask task;
	dronePathFinder path_finder;

	public delivery_drone(RobotController rc){
		super(rc);
	}

	@Override public void init() throws GameActionException {
		path_finder = new dronePathFinder(rc) {
			@Override
			void found_water(MapLocation pos) throws GameActionException {
				on_find_water(pos);
			}

			@Override
			void found_unit(RobotInfo robot) throws GameActionException {
			    on_find_unit(robot);
			}

			@Override
			boolean is_dangerous(MapLocation pos) throws GameActionException {
			    return is_location_dangerous(pos);
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
		    task.debug();
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

	@Override
	public void bc_home_hq(MapLocation pos) {
	    hq_location = pos;
	}

	private void on_find_water(MapLocation pos) throws GameActionException {
	    if (!water_locations.contains(pos)) {
			MapLocation closest_water = Util.closest(water_locations, pos);
			if (closest_water == null || closest_water.distanceSquaredTo(pos) > 200)
				b.send_location(b.LOC_WATER, pos);
			water_locations.add(pos);
		}
		rc.setIndicatorDot(pos,255,255,255);
	}

	private void on_find_unit(RobotInfo robot) throws GameActionException {
		if (robot.getTeam() == rc.getTeam().opponent()) {
			if (robot.getType().canShoot()) {
			    MapLocation netgun = robot.getLocation();
			    if (!enemy_netguns.contains(netgun)) {
					b.send_location(b.LOC_ENEMY_NETGUN, netgun);
					enemy_netguns.add(netgun);
				}
				rc.setIndicatorDot(netgun, 255, 0, 0);
			}
		}
	}

	boolean is_location_dangerous(MapLocation pos) throws GameActionException {
		for (MapLocation m : enemy_netguns) {
			if (pos.isWithinDistanceSquared(m, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
				return true;
			}
		}

		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
			on_find_unit(r);
			if (r.getType().canShoot()) {
				if (pos.isWithinDistanceSquared(r.getLocation(), GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
					return true;
				}
			}
		}

		return false;
	}

	private DroneTask find_best_task() {
		if (task == null || !task.is_running()) {
			return new MoveDroneTask(this, Util.randomPoint(rc.getMapHeight(), rc.getMapWidth()));
		}
		return task;
	}
}
