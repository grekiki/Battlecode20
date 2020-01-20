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
	int priority = 0;

	String reason = "BREZ DELA";  // Za debug

	DroneTask(delivery_drone drone) {
	    this.drone = drone;
	}

	public abstract boolean run() throws GameActionException;

	public void on_start() {
		is_running = true;
		is_complete = false;
	}

	public void on_complete() throws GameActionException {
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

	MoveDroneTask(delivery_drone drone, MapLocation dest, int priority) {
		super(drone);
		this.destination = dest;
		this.reason = "MoveDroneTask";
		this.priority = priority;
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
		to_source_task = new MoveDroneTask(drone, source, 50) {
			@Override
			public void on_complete() throws GameActionException {
				super.on_complete();
				on_arrive_source(source);
			}
		};
	}

	public void on_arrive_source(MapLocation source) {
	    to_destination_task = new MoveDroneTask(drone, destination, 50) {
			@Override
			public void on_start() {
				super.on_start();
			}

			@Override
			public void on_complete() throws GameActionException {
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


class ChaseDroneTask extends DroneTask {
	RobotInfo target_unit;

	ChaseDroneTask(delivery_drone drone, RobotInfo target_unit, int priority) {
		super(drone);
		this.target_unit = target_unit;
		this.reason = "ChaseDroneTask";
		this.priority = priority;
	}

	@Override
	public boolean run() throws GameActionException {
		if (!is_running) {
			on_start();
		}

		if (drone.rc.canSenseRobot(target_unit.getID())) {
			target_unit = drone.rc.senseRobot(target_unit.getID());
			destination = target_unit.getLocation();
		} else {
			on_complete(false);
			return false;
		}

		boolean move = drone.path_finder.moveTowards(destination);
		if (drone.path_finder.is_at_goal(drone.rc.getLocation(), destination)) {
			on_complete(true);
		}
		return move;
	}

	public void on_complete(boolean success) throws GameActionException {
		super.on_complete();
	}

	@Override
	public String toString() {
		return String.format("%s: --> %s [%s]", reason, destination, target_unit);
	}
}

public class delivery_drone extends robot{
	MapLocation hq_location;

	Set<MapLocation> water_locations = new HashSet<>();
	Set<MapLocation> enemy_netguns = new HashSet<>();
	Set<MapLocationPair> delivery_locations = new HashSet<>();

	DroneTask task;
	dronePathFinder path_finder;
	RobotInfo held_unit;

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
		    if (task != null)
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
		delivery_locations.add(new MapLocationPair(from, to));
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

	private boolean is_water(MapLocation pos) throws GameActionException {
		return rc.canSenseLocation(pos) && rc.senseFlooding(pos);
	}

	private boolean pick_up_unit(RobotInfo unit) throws GameActionException {
		if (rc.canPickUpUnit(unit.getID())) {
			rc.pickUpUnit(unit.getID());
			held_unit = unit;
			return true;
		}
		return false;
	}

	private boolean drop_unit(Direction dir) throws GameActionException {
		if (rc.canDropUnit(dir)) {
			rc.dropUnit(dir);
			held_unit = null;
			return true;
		}
		return false;
	}

	private boolean drop_unit_water() throws GameActionException {
		MapLocation p = rc.getLocation();
		for (Direction d : Util.dir) {
			if (is_water(p.add(d))) {
				if (drop_unit(d))
					return true;
			}
		}
		return false;
	}

	private boolean drop_unit_safe() throws GameActionException {
		MapLocation p = rc.getLocation();
		for (Direction d : Util.dir) {
			if (!is_water(p.add(d))) {
				if (drop_unit(d))
					return true;
			}
		}
		return false;
	}

	private void on_find_water(MapLocation pos) throws GameActionException {
		if (held_unit != null && held_unit.getTeam() != rc.getTeam()) {
			drop_unit_water();
			if (task != null) {
				task.on_complete();
				task = null;
			}
		}

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

	RobotInfo find_closest_enemy_unit(MapLocation pos) {
		int closest = c.inf;
		RobotInfo robot = null;
		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
			int d = r.getLocation().distanceSquaredTo(pos);
			if (d < closest) {
				closest = d;
				robot = r;
			}
		}
		return robot;
	}

	private DroneTask drop_water_task() throws GameActionException {
		MapLocation cur = rc.getLocation();
		MapLocation closest_water = Util.closest(water_locations, cur);
		if (closest_water != null) {
			return new MoveDroneTask(this, closest_water, 25) {
				@Override
				public void on_complete() throws GameActionException {
					super.on_complete();
					drop_unit_water();
				}
			};
		}
		return new MoveDroneTask(this, Util.randomPoint(rc.getMapHeight(), rc.getMapWidth()), 20);
	}

	private DroneTask find_best_task() throws GameActionException {
		if (rc.isCurrentlyHoldingUnit()) {
		    if (task != null && task.is_running())
		    	return task;

		    // Zakaj smo tukaj?
		    if (held_unit.getTeam() == rc.getTeam()) {
		    	drop_unit_safe();
		    	return task;
			} else {
		    	return drop_water_task();
			}
		}

		if (task != null && task.is_running() && task.priority > 25) return task;

		// Najdemo nasprotnikovo enoto ...
		RobotInfo enemy_unit = find_closest_enemy_unit(rc.getLocation());
		if (enemy_unit != null) {
			return new ChaseDroneTask(this, enemy_unit, 40) {
				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					if (success) {
					    pick_up_unit(enemy_unit);
					}
				}
			};
		}

		if (task == null || !task.is_running()) {
			return new MoveDroneTask(this, Util.randomPoint(rc.getMapHeight(), rc.getMapWidth()), 1);
		}
		return task;
	}
}
