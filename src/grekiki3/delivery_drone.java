package grekiki3;

import battlecode.common.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract class dronePathFinder extends BasePathFinder {
	boolean ignore_danger = false;

	dronePathFinder(RobotController rc) {
		super(rc);
		LOOKAHEAD_STEPS = 3;
		UNIT_MAX_WAIT = 1;
	}

	// Metoda se bo poklicala, ko naletimo na vodo.
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
		// TODO kaj ce je ujet
		if (!ignore_danger && is_dangerous(to)) {
			return false;
		}
		RobotInfo robot = rc.senseRobotAtLocation(to);
		if (robot != null && robot.getID() != rc.getID()) {
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
	int time_running = 0;

	String reason = "BREZ DELA";  // Za debug

	DroneTask(delivery_drone drone) {
	    this.drone = drone;
	}

	public abstract boolean run() throws GameActionException;

	public void on_start() throws GameActionException {
		is_running = true;
		is_complete = false;
	}

	public void on_complete(boolean success) throws GameActionException {
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
	    time_running++;
		boolean move = drone.path_finder.moveTowards(destination);
	    if (drone.path_finder.is_at_goal(drone.rc.getLocation(), destination)) {
	        on_complete(true);
		}
	    return move;
	}

	@Override
	public String toString() {
		return String.format("%s: --> %s", reason, destination);
	}
}

class DeliverDroneTask extends DroneTask {
    DroneTask current_task;
    DroneDeliveryRequest delivery;

	DeliverDroneTask(delivery_drone drone, DroneDeliveryRequest delivery, int priority) {
		super(drone);

		this.reason = "DeliverDroneTask";
		this.delivery = delivery;
		this.priority = priority;
	}

	@Override
	public boolean run() throws GameActionException {
		if (!is_running) {
			on_start();
		}

		time_running++;
		boolean move = false;
		if (!current_task.is_complete()) {
			move = current_task.run();
		}
		return move;
	}

	@Override
	public void on_start() throws GameActionException {
		super.on_start();
		int sense = handle_sense_target();
		if (sense > 0) {
			on_arrive_source(delivery);
		} else {
			current_task = new MoveDroneTask(drone, delivery.from, this.priority) {
				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					on_arrive_source(delivery);
				}

				@Override
				public boolean run() throws GameActionException {
					int sense = handle_sense_target();
					if (sense > 0) {
						on_arrive_source(delivery);
					} else if (sense == 0) {
						return false;
					}
					return super.run();
				}
			};
		}
	}

	public void on_arrive_source(DroneDeliveryRequest delivery) throws GameActionException {
		if (!on_pick_up()) {
			current_task = new ChaseDroneTask(drone, delivery.id, this.priority) {
				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					if (success) {
						if (!on_pick_up()) {
							on_arrive_source(delivery);
						}
					} else {
						DeliverDroneTask.this.on_complete(false);
					}
				}

				@Override
				public boolean run() throws GameActionException {
					int sense = handle_sense_target();
					if (sense == 0) {
						return false;
					}
					return super.run();
				}
			};
		}
	}

	int handle_sense_target() throws GameActionException {
		if (drone.rc.canSenseRobot(delivery.id)) {
			RobotInfo unit = drone.rc.senseRobot(delivery.id);
			if (drone.rc.canSenseLocation(unit.getLocation())) {
				RobotInfo carry_drone = drone.rc.senseRobotAtLocation(unit.getLocation());
				if (carry_drone.getHeldUnitID() == delivery.id) {
					on_complete(false);
					return 0;
				}
			}
			return 1;
		}
		return -1;
	}

	boolean on_pick_up() throws GameActionException {
		int sense = handle_sense_target();
		if (sense > 0) {
			if (drone.pick_up_unit(drone.rc.senseRobot(delivery.id))) {
			    priority = 71;
				drone.b.send_location2(drone.b.LOC2_DRONE_COMPLETE, delivery.from, delivery.to, delivery.id);
				current_task = new MoveDroneTask(drone, delivery.to, this.priority) {
					@Override
					public void on_complete(boolean success) throws GameActionException {
						super.on_complete(success);
						DeliverDroneTask.this.on_complete(success);
					}
				};
				return true;
			}
			return false;
		} else {
	        on_complete(false);
	        return true;
		}
	}

	@Override
	public String toString() {
		return String.format("%s: --> %s", reason, delivery);
	}

	@Override
	public void debug() {
		super.debug();
		System.out.println(this);
		if (current_task != null)
			current_task.debug();
		drone.rc.setIndicatorDot(delivery.from, 0,0,255);
		drone.rc.setIndicatorDot(delivery.to, 0,255,0);
	}
}


class ChaseDroneTask extends DroneTask {
	int target_unit;

	ChaseDroneTask(delivery_drone drone, int target_unit, int priority) {
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

		time_running++;
		if (drone.rc.canSenseRobot(target_unit)) {
			RobotInfo unit = drone.rc.senseRobot(target_unit);
			destination = unit.getLocation();
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

	@Override
	public String toString() {
		return String.format("%s: --> %s [%s]", reason, destination, target_unit);
	}
}

class DroneDeliveryRequest {
	MapLocation from;
	MapLocation to;
	int id;

	DroneDeliveryRequest(MapLocation from, MapLocation to, int id) {
		this.from = from;
		this.to = to;
		this.id = id;
	}

	@Override
	public String toString() {
	    return String.format("%s --> %s [%d]", from, to, id);
	}
}

public class delivery_drone extends robot{
	private static final int COW_ENEMY_PRIORITY = 27;
	private static final int COW_ENEMY_RADIUS = 5 * GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED;
	int strategy=-1;
	
	MapLocation hq_location;
	MapLocation home_location;

	vector_set_gl water_locations = new vector_set_gl();
	Set<MapLocation> enemy_netguns = new HashSet<>();
	Set<MapLocation> enemy_refineries = new HashSet<>();
	Map<Integer, DroneDeliveryRequest> delivery_locations = new HashMap<>();

	DroneTask task;
	dronePathFinder path_finder;
	RobotInfo held_unit;
	MapLocation enemy_pickup_location;

	public delivery_drone(RobotController rc){
		super(rc);
	}

	@Override public void init() throws GameActionException {
		path_finder = new dronePathFinder(rc) {
			@Override
			boolean is_dangerous(MapLocation pos) throws GameActionException {
			    return is_location_dangerous(pos);
			}
		};

		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq_location = r.location;
			} else if (r.type == RobotType.FULFILLMENT_CENTER) {
				home_location = r.location;
			}
		}

		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	@Override public void precompute() throws GameActionException {
		
	}

	@Override public void runTurn() throws GameActionException {
		if (!rc.isReady())return;

		task = find_best_task();
		if (task != null) {
			task.run();
		    if (task != null)
				task.debug();
		}
	}

	@Override public void postcompute() throws GameActionException {
		water_scan(rc.getLocation());
		while (Clock.getBytecodesLeft() > 500) {
			if (!b.read_next_round()) {
				break;
			}
		}
	}

	@Override
	public void bc_enemy_hq(MapLocation pos) {
	    enemy_netguns.add(pos);
	}

	@Override
	public void bc_drone(MapLocation from, MapLocation to, int id) {
	    delivery_locations.put(id, new DroneDeliveryRequest(from, to, id));
	}

	@Override
	public void bc_drone_complete(MapLocation from, MapLocation to, int id) {
	    delivery_locations.remove(id);
	    /*
	    if (task != null && (task instanceof DeliverDroneTask) && ((DeliverDroneTask) task).delivery.id == id) {
	    	task = null;
		}
	     */
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

	private void water_scan(MapLocation pos) throws GameActionException {
		for (Direction d : Util.dir) {
			if (is_water(pos.add(d))) {
				on_find_water(pos.add(d));
			}
		}
	}

	boolean pick_up_unit(RobotInfo unit) throws GameActionException {
		if (rc.canPickUpUnit(unit.getID())) {
			rc.pickUpUnit(unit.getID());
			held_unit = unit;
			if (unit.getTeam() == rc.getTeam().opponent()) {
				enemy_pickup_location = unit.getLocation();
			}
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

	private boolean drop_unit_unsafe() throws GameActionException {
		for (Direction d : Util.dir) {
			if (drop_unit(d))
				return true;
		}
		return false;
	}

	private void on_find_water(MapLocation pos) throws GameActionException {
		if (held_unit != null && (held_unit.getTeam() == rc.getTeam().opponent()
				|| held_unit.getType() == RobotType.COW && task != null && task.priority < COW_ENEMY_PRIORITY)) {
			drop_unit_water();
			if (task != null) {
				task.on_complete(true);
				task = null;
			}
		}

	    if (!water_locations.contains(pos)) {
			MapLocation closest_water = Util.closest(water_locations, pos);
			if (closest_water == null || closest_water.distanceSquaredTo(pos) > 200)
				b.send_location(b.LOC_WATER, pos);
			if (closest_water == null || closest_water.distanceSquaredTo(pos) >= 64) {
				water_locations.add(pos);
			}
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

	MapLocation find_closest_netgun(MapLocation pos) throws GameActionException {
		MapLocation closest = Util.closest(enemy_netguns, pos);
		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
			on_find_unit(r);
			if (r.getType().canShoot()) {
			    if (r.getLocation().distanceSquaredTo(pos) < closest.distanceSquaredTo(pos)) {
			    	closest = r.getLocation();
				}
			}
		}
		return closest;
	}

	boolean is_location_dangerous(MapLocation pos) throws GameActionException {
	    final int safety_factor = 18;
		for (MapLocation m : enemy_netguns) {
			if (pos.isWithinDistanceSquared(m, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED + safety_factor)) {
				return true;
			}
		}

		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
			on_find_unit(r);
			if (r.getType().canShoot()) {
				if (pos.isWithinDistanceSquared(r.getLocation(), GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED + safety_factor)) {
					return true;
				}
			}
		}
		return false;
	}

	int get_unit_priority(RobotType type) {
		switch (type) {
			case LANDSCAPER: return 5;
			case COW: return 3;
			case MINER: return 2;
			default: return 0;
		}
	}

	RobotInfo find_closest_enemy_unit(MapLocation pos) throws GameActionException {
		int closest = c.inf;
		RobotInfo robot = null;
		for (RobotInfo r : rc.senseNearbyRobots()) {
		    if (r.getTeam() == rc.getTeam()) continue;
		   	int priority = get_unit_priority(r.getType());
		    if (priority > 0) {
		        if (r.getType() == RobotType.COW) {
		        	MapLocation loc = closest_enemy_building();
		        	if (loc != null && loc.isWithinDistanceSquared(r.getLocation(), COW_ENEMY_RADIUS))
		        		continue;
				}

				int d = r.getLocation().distanceSquaredTo(pos);
				if (robot == null || d < closest || priority > get_unit_priority(robot.getType())) {
					closest = d;
					robot = r;
				}
			}
		}
		return robot;
	}

	DroneDeliveryRequest find_closest_delivery_location(MapLocation pos) {
		int closest = c.inf;
		DroneDeliveryRequest request = null;
		for (DroneDeliveryRequest p : delivery_locations.values()) {
			int d = p.from.distanceSquaredTo(pos);
			if (d < closest) {
				closest = d;
				request = p;
			}
		}
		return request;
	}

	private DroneTask drop_water_task() throws GameActionException {
		MapLocation cur = rc.getLocation();
		MapLocation closest_water = Util.closest(water_locations, cur);
		if (closest_water != null) {
			return new MoveDroneTask(this, closest_water, 25) {
				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					drop_unit_water();
				}
			};
		}
		return explore_task(20);
	}

	private MapLocation closest_enemy_building() throws GameActionException {
		MapLocation cur = rc.getLocation();
		MapLocation dest = Util.closest(enemy_refineries, cur);
		if (dest == null) {
			dest = Util.closest(enemy_netguns, cur);
		}
		return dest;
	}

	private DroneTask explore_task(int priority) {
		return new MoveDroneTask(this, Util.randomPoint(rc.getMapHeight(), rc.getMapWidth()), priority);
	}

	private DroneTask go_home_task(int priority) {
		if (hq_location != null) {
			return new MoveDroneTask(this, hq_location, priority);
		}
		if (home_location != null) {
			return new MoveDroneTask(this, home_location, priority);
		}
		return explore_task(priority / 2);
	}

	private DroneTask enemy_cow_building_task() throws GameActionException {
	    MapLocation dest = closest_enemy_building();
	    if (dest != null) {
	    	return new MoveDroneTask(this, dest, COW_ENEMY_PRIORITY) {
				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					drop_unit_unsafe();
				}

				@Override
				public boolean run() throws GameActionException {
					boolean ret = super.run();
				    if (rc.getLocation().isWithinDistanceSquared(destination, COW_ENEMY_RADIUS)) {
				    	on_complete(true);
					}
					return ret;
				}
			};
		}
	    return new MoveDroneTask(this, Util.randomPoint(rc.getMapHeight(), rc.getMapWidth()), 20);
	}

	private DroneTask find_best_task() throws GameActionException {
		if (task != null && task.is_complete())	{
			task = null;
		}

		if (task != null && task.priority >= 100) return task;

		// UMIK
		MapLocation cur = rc.getLocation();
		if (task != null && task.priority < 100 && is_location_dangerous(cur)) {
			MapLocation netgun = find_closest_netgun(cur);
			if (netgun != null) {
			    Direction dir = cur.directionTo(netgun).opposite();
				MapLocation dest = cur.add(dir).add(dir).add(dir);
				return new MoveDroneTask(this, dest, 100) {
					@Override
					public void on_start() throws GameActionException {
						super.on_start();
						path_finder.ignore_danger = true;
					}

					@Override
					public void on_complete(boolean success) throws GameActionException {
						super.on_complete(success);
						path_finder.ignore_danger = false;
					}
				};
			}
		}

		if (rc.isCurrentlyHoldingUnit()) {
		    if (task != null && task.is_running())
		    	return task;

		    if (held_unit.getTeam() == rc.getTeam()) {
		    	if (!drop_unit_safe()) {
		    		return go_home_task(task == null ? 30 : task.priority);
				}
		    	return task;
			} else {
		        if (held_unit != null && held_unit.getType() == RobotType.COW) {
		            return enemy_cow_building_task();
				}
		    	return drop_water_task();
			}
		}

		if (task != null && task.is_running() && task.priority > 70) return task;

		DroneDeliveryRequest delivery = find_closest_delivery_location(rc.getLocation());
		if (delivery != null) {
			return new DeliverDroneTask(this, delivery, 50) {
				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					if (success) {
						drop_unit_safe();
					}
					delivery_locations.remove(delivery.id);
					priority = -1;
				}
			};
		}

		// Najdemo nasprotnikovo enoto ...
		RobotInfo enemy_unit = find_closest_enemy_unit(rc.getLocation());
		if (enemy_unit != null) {
			return new ChaseDroneTask(this, enemy_unit.getID(), 33 + get_unit_priority(enemy_unit.getType())) {
				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					if (success) {
					    pick_up_unit(enemy_unit);
					}
				}
			};
		}

		// Vrnemo se kamor smo pobrali nasprotnika ...
		if (enemy_pickup_location != null && task != null && task.priority < 7) {
			return new MoveDroneTask(this, enemy_pickup_location, 7) {
				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					if (enemy_pickup_location.isAdjacentTo(destination)) {
						enemy_pickup_location = null;
					}
				}
			};
		}

		if (task == null || task.time_running > 80) {
		    return explore_task(1);
		}
		return task;
	}
	
	public void bc_base_strategy(int[] message) {
		strategy=message[2];
	}

}
