package grekiki3;

import battlecode.common.*;

import java.util.*;

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
		if(drone.rc.getLocation().isAdjacentTo(destination)) {
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

class LocationPriority {
	MapLocation loc;
	int priority;

	LocationPriority(MapLocation loc, int priority) {
		this.loc = loc;
		this.priority = priority;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LocationPriority that = (LocationPriority) o;
		return Objects.equals(loc, that.loc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(loc);
	}
}

public class delivery_drone extends robot{
	private static final int ENEMY_DANGER_RADIUS = GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED + 18;
	private static final int COW_ENEMY_PRIORITY = 27;
	private static final int COW_ENEMY_RADIUS = ENEMY_DANGER_RADIUS * 3;
	private static final int TASK_TIME_LIMIT = 100;
	private static final int ASSIST_RADIUS = 25;
	private static int MAX_TASK_RADIUS;

	int strategy=-1;

	MapLocation hq_location;
	MapLocation home_location;

	vector_set_gl water_locations = new vector_set_gl();
	Set<MapLocation> enemy_netguns = new HashSet<>();
	Set<MapLocation> enemy_refineries = new HashSet<>();
	Set<LocationPriority> assist_locations = new HashSet<>();
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

		MAX_TASK_RADIUS = Math.max(rc.getMapWidth(), rc.getMapHeight()) / 2;
		MAX_TASK_RADIUS = MAX_TASK_RADIUS * MAX_TASK_RADIUS;

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
	}

	@Override
	public void bc_drone_assist(MapLocation pos, int priority) {
	    assist_locations.add(new LocationPriority(pos, priority));
	}

	@Override
	public void bc_drone_assist_clear(MapLocation pos, int priority) {
	    assist_locations.remove(new LocationPriority(pos, priority));
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
		return !rc.isCurrentlyHoldingUnit();
	}

	private boolean drop_unit_safe(MapLocation goal) throws GameActionException {
		while(rc.getCooldownTurns()>1) {
			Clock.yield();
		}
		MapLocation p = rc.getLocation();
		Direction d=p.directionTo(goal);
		if(rc.canDropUnit(d)) {
			rc.dropUnit(d);
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
		return !rc.isCurrentlyHoldingUnit();
	}

	private boolean drop_unit_unsafe() throws GameActionException {
		for (Direction d : Util.dir) {
			if (drop_unit(d))
				return true;
		}
		return !rc.isCurrentlyHoldingUnit();
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
		for (MapLocation m : enemy_netguns) {
			if (pos.isWithinDistanceSquared(m, ENEMY_DANGER_RADIUS)) {
				return true;
			}
		}

		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
			on_find_unit(r);
			if (r.getType().canShoot()) {
				if (pos.isWithinDistanceSquared(r.getLocation(), ENEMY_DANGER_RADIUS)) {
					return true;
				}
			}
		}
		return false;
	}

	void handle_enemy_cluster(MapLocation pos, int enemies) throws GameActionException {
		if (enemies >= 3 && pos != null) {
		    LocationPriority assist_location = find_closest_assist_location(pos);
		    if (assist_location == null || assist_location.loc.distanceSquaredTo(pos) > ASSIST_RADIUS) {
		    	assist_locations.add(assist_location);
		    	b.send_location_priority(b.LOCP_DRONE_ASSIST, pos, 7 + enemies);
			}
		}
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
		// Hkrati se prestejemo ...
		int enemies = 0;
		int closest = c.inf;
		RobotInfo robot = null;
		for (RobotInfo r : rc.senseNearbyRobots()) {
		    if (r.getTeam() == rc.getTeam()) continue;
		    if (r.getType() != RobotType.COW && r.getType() != RobotType.DELIVERY_DRONE) {
		    	enemies++;
			}
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
		if (robot != null)
			handle_enemy_cluster(robot.getLocation(), enemies);
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
	    		boolean dropped = false;

				@Override
				public boolean is_running() {
				    return !is_complete();
				}

				@Override
				public boolean is_complete() {
					return dropped && super.is_complete();
				}

				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					if (drop_unit_unsafe()) {
						dropped = true;
					}
				}

				@Override
				public boolean run() throws GameActionException {
				    if (drone.rc.getLocation().isWithinDistanceSquared(destination, COW_ENEMY_RADIUS)) {
				    	on_complete(true);
				    	return false;
					}
					return super.run();
				}
			};
		}
	    return drop_water_task();
	}

	private DroneTask escape_task(MapLocation cur) throws GameActionException {
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
		return null;
	}

	private LocationPriority find_closest_assist_location(MapLocation cur) {
		int min_dist = c.inf;
		LocationPriority best = null;
		for (LocationPriority loc : assist_locations) {
			if (loc == null) continue;
			int d = cur.distanceSquaredTo(loc.loc);
			if (d < min_dist) {
				min_dist = d;
				best = loc;
			}
		}
		return best;
	}

	private LocationPriority find_best_assist_location(MapLocation cur) {
		int max_priority = -1;
		int min_dist = c.inf;
		LocationPriority best = null;
		for (LocationPriority loc : assist_locations) {
			if (loc == null) continue;
			int d = cur.distanceSquaredTo(loc.loc);
			if (loc.priority > max_priority) {
				min_dist = d;
				max_priority = loc.priority;
				best = loc;
			} else if (loc.priority >= max_priority && d < min_dist) {
				min_dist = d;
				max_priority = loc.priority;
				best = loc;
			}
		}
		return best;
	}

	private DroneTask assistance_move_task(LocationPriority loc) {
		return new MoveDroneTask(this, loc.loc, loc.priority) {
			@Override
			public void on_complete(boolean success) throws GameActionException {
				super.on_complete(success);
				if (success) {
					assist_locations.remove(loc);
					b.send_location_priority(b.LOCP_DRONE_ASSIST_CLEAR, loc.loc, loc.priority);
				}
			}
		};
	}

	private DroneTask find_best_task() throws GameActionException {
		if (task != null && (task.is_complete() || task.time_running > TASK_TIME_LIMIT)) {
			task = null;
		}

		if (task != null && task.priority >= 100) return task;

		// UMIK
		MapLocation cur = rc.getLocation();
		if (task != null && task.priority < 100 && is_location_dangerous(cur)) {
		    DroneTask t = escape_task(cur);
		    if (t != null) return t;
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
		if (delivery != null && delivery.from.isWithinDistanceSquared(cur, MAX_TASK_RADIUS)) {
			return new DeliverDroneTask(this, delivery, 50) {
				@Override
				public void on_complete(boolean success) throws GameActionException {
					super.on_complete(success);
					if (success) {
						drop_unit_safe(delivery.to);
					}
					delivery_locations.remove(delivery.id);
					priority = -1;
				}
			};
		}

		// Najdemo nasprotnikovo enoto ...
		RobotInfo enemy_unit = find_closest_enemy_unit(cur);
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

		LocationPriority assist_location = find_best_assist_location(cur);
		if (assist_location != null) {
			return assistance_move_task(assist_location);
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
