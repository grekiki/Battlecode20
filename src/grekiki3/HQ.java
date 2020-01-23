package grekiki3;

import java.util.ArrayList;

import battlecode.common.*;

public class HQ extends robot {
	int w, h;// dimenzije mape
	MapLocation loc;// nasa lokacija
	/**
	 * Strategije: <br>
	 * 1000-rush<br>
	 * 2000-build a wall<br>
	 * 3000-napadeni smo
	 */
	int strategy = -1;

	int miners_spawned = 0;
	vector_set_gl polja;
	vector_set_gl slaba_polja;

	int wallRadius;
	ArrayList<Integer> minerIds;
	boolean haveBaseBuilder = false;
	int builder = -1;
	boolean haveRusher = false;
	int rusher = -1;

	MapLocation landscaping;
	MapLocation drones;
	public HQ(RobotController rc) {
		super(rc);
	}

	/**
	 * HQ se v prvi potezi najprej odloci kaj bi naredil, glede na stanje mape.
	 *
	 * @throws GameActionException
	 */
	@Override
	public void init() throws GameActionException {
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		loc = rc.getLocation();
		polja = new vector_set_gl();
		slaba_polja = new vector_set_gl();
		strategy = choose_strategy();
		minerIds = new ArrayList<Integer>();
		b.send_packet(b.BASE_STRATEGY, new int[] { b.PRIVATE_KEY, b.BASE_STRATEGY, strategy, 0, 0, 0, 0 });
		b.send_location(b.LOC_HOME_HQ, rc.getLocation());
		boolean left=rc.getLocation().x<rc.getMapWidth()/2;
		boolean bottom=rc.getLocation().y<rc.getMapHeight()/2;
		for(int x=0;x<5;x++) {
			for(int y=0;y<5;y++) {
				if((x%2==0&&y%2==0)||(x+y<5)) {
					
				}else {
					int px=left?x:-x;
					int py=bottom?y:-y;
					int h=rc.senseElevation(new MapLocation(rc.getLocation().x+px,rc.getLocation().y+py));
					if(!rc.senseFlooding(new MapLocation(rc.getLocation().x+px,rc.getLocation().y+py))&&Math.abs(h-rc.senseElevation(rc.getLocation()))<=3) {
						if(landscaping==null) {
							landscaping=new MapLocation(rc.getLocation().x+px,rc.getLocation().y+py);
						}else if(drones==null){
							drones=new MapLocation(rc.getLocation().x+px,rc.getLocation().y+py);
						}
					}
				}
			}
		}
		System.out.println(drones+" "+landscaping);
	}

	@Override
	public void precompute() throws GameActionException {
		for (RobotInfo r : rc.senseNearbyRobots(2)) {
			if (r.team == rc.getTeam() && r.type == RobotType.MINER) {
				if (!minerIds.contains(r.ID)) {
					minerIds.add(r.ID);
				}
			}
		}
		if(rc.getRoundNum()<200&&strategy!=1000) {
			for(RobotInfo r:rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent())) {
				if(r.type==RobotType.MINER) {
					strategy=3000;
					b.send_packet(b.BASE_STRATEGY, new int[] { b.PRIVATE_KEY, b.BASE_STRATEGY, strategy, 0, 0, 0, 0 });
					if (!haveBaseBuilder && minerIds.size() > 0) {
						haveBaseBuilder = true;
						b.send_packet(b.MINER_HELP_HQ, new int[] { b.PRIVATE_KEY, b.MINER_HELP_HQ, minerIds.get(0), 0, 0, 0, 0 });
					}
				}
			}
		}
		if (strategy == 1000) {
			if (!haveRusher && minerIds.size() > 0) {
				haveRusher = true;
				b.send_packet(b.MINER_RUSH, new int[] { b.PRIVATE_KEY, b.MINER_RUSH, minerIds.get(0), 0, 0, 0, 0 });
			}
		} else if (strategy == 2000) {
			if (!haveBaseBuilder && minerIds.size() > 0) {
				haveBaseBuilder = true;
				b.send_packet(b.MINER_HELP_HQ, new int[] { b.PRIVATE_KEY, b.MINER_HELP_HQ, minerIds.get(0), 0, 0, 0, 0 });
			}
		}
	}

	@Override
	public void runTurn() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}
		if (try_shoot()) {
			return;
		}

		if (strategy == 1000) {
			if (this.miners_spawned < 3)
				if (try_spawn_miner(pick_miner_direction()))
					return;
		}
		if (strategy == 2000) {
			if (((this.miners_spawned <= 4 * polja.load &&rc.getTeamSoup()>700)||this.miners_spawned <= 2 * polja.load )&& this.miners_spawned < 10)
				if (try_spawn_miner(pick_miner_direction()))
					return;

		}
		if (strategy == 3000) {
			if (this.miners_spawned < 3)
				if (try_spawn_miner(pick_miner_direction()))
					return;
		}

	}
	static boolean built_defensive_ls=false;
	@Override
	public void postcompute() throws GameActionException {
		if (strategy == 2000) {
			if (rc.getRoundNum() == 50) {
				b.send_location(b.BUILD_TOVARNA_DRONOV,drones);
			}
			if (rc.getRoundNum() == 200) {
				b.send_location(b.BUILD_TOVARNA_LANDSCAPERJEV, landscaping);
			}
		}
		if(strategy==3000) {
			if(!built_defensive_ls) {
				b.send_location(b.BUILD_TOVARNA_LANDSCAPERJEV, new MapLocation(rc.getLocation().x+1,rc.getLocation().y));
				built_defensive_ls=true;
			}
		}
		while (Clock.getBytecodesLeft() > 500) {
			if (!b.read_next_round()) {
				break;
			}
		}
	}

	public int choose_strategy() {
		wallRadius = 2;
		if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) {
			return 1000;// rush
		} else {
			return 2000;
		}
	}

	// Pomozne metode
	Direction pick_miner_direction() {
		return Util.getRandomDirection();
//		return Direction.SOUTH;
	}

	boolean try_spawn_miner(Direction dir) {
		if (rc.getTeamSoup() >= miner.MINER_COST && rc.canBuildRobot(RobotType.MINER, dir)) {
			try {
				rc.buildRobot(RobotType.MINER, dir);
				miners_spawned++;
				return true;
			} catch (GameActionException e) {
				// e.printStackTrace();
			}
		}
		return false;
	}

	boolean try_shoot() throws GameActionException {
		RobotInfo closest = null;
		int dis = 1000000;
		for (RobotInfo r : rc.senseNearbyRobots(net_gun.SHOOT_RADIUS, rc.getTeam().opponent())) {
			if (r.getType() == RobotType.DELIVERY_DRONE) {
				int t = rc.getLocation().distanceSquaredTo(r.location);
				if (t < dis) {
					dis = t;
					closest = r;
				}
			}
		}
		if (closest != null && rc.canShootUnit(closest.ID)) {
			rc.shootUnit(closest.ID);
			return true;
		}
		return false;
	}

	@Override
	public void bc_polje_found(MapLocation pos) {
		if (!polja.contains(pos)) {
			polja.add(pos);
		}
	}

	@Override
	public void bc_polje_empty(MapLocation pos) {
		if (polja.contains(pos)) {
			polja.remove(pos);
		}
		if (slaba_polja.contains(pos)) {
			slaba_polja.remove(pos);
		}
	}

	@Override
	public void bc_polje_slabo(MapLocation pos) {
		if (!slaba_polja.contains(pos)) {
			slaba_polja.add(pos);
		}
	}

	@Override
	public void bc_polje_upgrade(MapLocation pos) {
		if (slaba_polja.contains(pos)) {
			slaba_polja.remove(pos);
		}
		if (!polja.contains(pos)) {
			polja.add(pos);
		}
	}
}
