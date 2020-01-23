package grekiki3;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;


class landPathFinder extends BasePathFinder {
	landPathFinder(RobotController rc) {
		super(rc);
	}

	@Override
	boolean can_move(MapLocation from, Direction dir) throws GameActionException {
		// Ta metoda ignorira cooldown ...

		MapLocation to = from.add(dir);
		if (!rc.canSenseLocation(to) || rc.senseFlooding(to))
			return false;
		if (!rc.canSenseLocation(from) || Math.abs(rc.senseElevation(from) - rc.senseElevation(to)) > 3)
			return false;
		RobotInfo robot = rc.senseRobotAtLocation(to);
		if (robot != null && robot.getID() != rc.getID() && (!ignore_units || robot.getType().isBuilding()))
			return false;
		return true;
	}

	public boolean moveTowards(MapLocation dest) throws GameActionException {
		Direction dir = get_move_direction(dest);

		if (dir != null && rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		return false;
	}
}

public class landscaper extends robot {
	int strategy = -1;
	boolean attacking = false;
	MapLocation hq = null;
	final static int visina = 8;
	final static int omejitev_visine = 200;
	Direction explore = null;
	ArrayList<MapLocation> wall1;
	ArrayList<MapLocation> wall2;
	int px;
	int py;
	MapLocation prefered_location = null;
	int previousRound = -1;
	MapLocation[] holes;
	int roundHqHeight;
	int hqHeight = -1000;
	landPathFinder path;
	vector_set_gl net_guns;

	public landscaper(RobotController rc) {
		super(rc);
	}

	@Override
	public void init() throws GameActionException {
		net_guns=new vector_set_gl();
		while (Clock.getBytecodesLeft() > 2000 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				break;
			}
		}
		path = new landPathFinder(rc);
		previousRound = rc.getRoundNum();
		if (hq == null && strategy == 1000) {
			attacking = true;
		}
		if (hq != null && rc.canSenseLocation(hq)) {
			roundHqHeight = rc.senseElevation(hq) + 3;
		} else {
			roundHqHeight = 5;
		}
		wall1 = new ArrayList<MapLocation>();
		wall2 = new ArrayList<MapLocation>();
		for (int x = -2; x <= 2; x++) {
			for (int y = -2; y <= 2; y++) {
				if (x != 0 || y != 0) {
					int ax = Math.abs(x);
					int ay = Math.abs(y);
					if ((ax == 2 && ay == 0) || (ax == 0 && ay == 2)) {

					} else {
						if (Math.max(ax, ay) == 1) {
							wall1.add(new MapLocation(hq.x + x, hq.y + y));
						} else {
							wall2.add(new MapLocation(hq.x + x, hq.y + y));
						}
					}
				}
			}
		}
		for (Direction d : Util.dir) {
			if (rc.onTheMap(hq.add(d))) {
				wall1.add(hq.add(d));
			}
		}
		if (!attacking) {
			int x = hq.x;
			int y = hq.y;
			holes = new MapLocation[] { new MapLocation(x + 2, y), new MapLocation(x - 2, y), new MapLocation(x, y + 2), new MapLocation(x, y - 2) };
		}
	}

	@Override
	public void precompute() throws GameActionException {
		if (rc.getRoundNum() != previousRound + 1) {
			analyze_drone_delivery();
		}
		previousRound = rc.getRoundNum();
		px = hq.x % 2;
		py = hq.y % 2;
		if (!attacking) {
			if (rc.getRoundNum() >= 300 && prefered_location == null && (wall1.size() > 0 || wall2.size() > 0)) {
				if (wall1.size() > 0) {
					int t = (int) Math.floor(Math.random() * wall1.size());
					prefered_location = wall1.get(t);
					System.out.println(prefered_location);
					b.send_location2(b.LOC2_DRONE, rc.getLocation(), wall1.get(t), rc.getID());
				} else {
					int t = (int) Math.floor(Math.random() * wall2.size());
					prefered_location = wall2.get(t);
					System.out.println(prefered_location);
					b.send_location2(b.LOC2_DRONE, rc.getLocation(), wall2.get(t), rc.getID());
				}
			}
			if (prefered_location != null && rc.getRoundNum() > 300 && Math.random() < 1.0 / 30 && !rc.getLocation().equals(prefered_location)) {
				b.send_location2(b.LOC2_DRONE, rc.getLocation(), prefered_location, rc.getID());
			}
		}
	}

	private void analyze_drone_delivery() throws GameActionException {
		if (prefered_location == null) {
			return;
		}
		for (int i = 0; i < wall1.size(); i++) {
			MapLocation m = wall1.get(i);
			if (m.equals(rc.getLocation())) {
				prefered_location = rc.getLocation();
				return;
			}
		}
		for (int i = 0; i < wall2.size(); i++) {
			MapLocation m = wall2.get(i);
			if (m.equals(rc.getLocation())) {
				prefered_location = rc.getLocation();
				return;
			}
		}
		for (int i = 0; i < wall1.size(); i++) {
			MapLocation m = wall1.get(i);
			if (rc.canSenseLocation(m)) {
				RobotInfo r = rc.senseRobotAtLocation(m);
				if (r != null && r.team == rc.getTeam() && r.type == rc.getType()) {
					wall1.remove(m);
					i--;
				}
			}
		}
		for (int i = 0; i < wall2.size(); i++) {
			MapLocation m = wall2.get(i);
			if (rc.canSenseLocation(m)) {
				RobotInfo r = rc.senseRobotAtLocation(m);
				if (r != null && r.team == rc.getTeam() && r.type == rc.getType()) {
					wall2.remove(m);
					i--;
				}
			}
		}
		if (wall1.size() > 0) {
			int t = (int) Math.floor(Math.random() * wall1.size());
			prefered_location = wall1.get(t);
			System.out.println(prefered_location);
			b.send_location2(b.LOC2_DRONE, rc.getLocation(), wall1.get(t), rc.getID());
			return;
		}
		if (wall2.size() > 0) {
			int t = (int) Math.floor(Math.random() * wall2.size());
			prefered_location = wall2.get(t);
			System.out.println(prefered_location);
			b.send_location2(b.LOC2_DRONE, rc.getLocation(), wall2.get(t), rc.getID());
			return;
		}

	}

	@Override
	public void runTurn() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}
		if (strategy == 3000) {
			defendBase();
		}
//		System.out.println("init");
//		System.out.println(prefered_location);
		if (prefered_location != null && prefered_location == rc.getLocation()) {
			makeAWall();
		} else if (attacking) {
			doAttacking();
		} else {
			doLandscaping();
		}
	}

	private void defendBase() throws GameActionException {
		System.out.println("DEFENSE!");
		MapLocation enemyBuilding = findClosestEnemyBuilding();
		// poskusi odkopati našo stavbo
		for (Direction d : Util.dir) {
			RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(d));
			if (r != null && r.team == rc.getTeam() && rc.canDigDirt(d)&&(r.getType() == RobotType.DESIGN_SCHOOL ||r.getType() == RobotType.HQ || r.getType() == RobotType.FULFILLMENT_CENTER || r.getType() == RobotType.NET_GUN)) {
				rc.digDirt(d);
				System.out.println("Izkopavamo stavbo na "+rc.getLocation().add(d));
				return;
			}
		}
		if(rc.getDirtCarrying()==RobotType.LANDSCAPER.dirtLimit) {
			dumpDirt();
		}
		int countRoundHqAlly=0;
		for(RobotInfo r:rc.senseNearbyRobots(hq, 2, rc.getTeam())) {
			if(r.type==RobotType.LANDSCAPER) {
				countRoundHqAlly++;
			}
		}
		int countRoundHqEnemy=0;
		for(RobotInfo r:rc.senseNearbyRobots(hq, 2, rc.getTeam().opponent())) {
			if(r.type==RobotType.LANDSCAPER) {
				countRoundHqEnemy++;
			}
		}
		
		if (enemyBuilding == null||countRoundHqEnemy>=countRoundHqAlly) {
			System.out.println("Ni nasprotnih stavb, krožimo okoli hq");
			path.moveTowards(hq);
			return;
		} else {
			if (rc.getDirtCarrying() > 0) {
				//poskusimo nasipati zemljo na nasprotnikovo stavbo
				for (Direction d : Util.dir) {
					RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(d));
					if (r!=null&&(r.getType() == RobotType.DESIGN_SCHOOL || r.getType() == RobotType.FULFILLMENT_CENTER || r.getType() == RobotType.NET_GUN)) {
						if (r.getTeam() != rc.getTeam()) {
							if (rc.canDepositDirt(d)) {
								rc.depositDirt(d);
								System.out.println("Nasipavamo "+rc.getLocation().add(d));
								return;
							}
						}
					}
				}
				System.out.println("Gremo proti "+enemyBuilding);
				path.moveTowards(enemyBuilding);
				return;
			}else {
				//ni zemlje-poskusimo jo skopati
				for(Direction d:Util.dir) {
					if(rc.senseRobotAtLocation(rc.getLocation().add(d))==null) {
						if(rc.canDigDirt(d)) {
							System.out.println("Kopljemo zemljo iz "+rc.getLocation().add(d));
							rc.digDirt(d);
							return;
						}
					}
				}
				Direction rand=Util.getRandomDirection();
				if(rc.canMove(rand)) {
					rc.move(rand);
				}
				return;
			}
		}

	}

	private boolean dumpDirt() throws GameActionException {
		System.out.println("Dumping ");
		for (Direction d : Util.dir) {
			RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(d));
			if (r!=null&&(r.getType() == RobotType.DESIGN_SCHOOL || r.getType() == RobotType.FULFILLMENT_CENTER || r.getType() == RobotType.NET_GUN)) {
				if (r.getTeam() != rc.getTeam()) {
					if (rc.canDepositDirt(d)) {
						rc.depositDirt(d);
						System.out.println("Nasipavamo "+rc.getLocation().add(d));
						return true;
					}
				}
			}
		}
		for (Direction d : Util.dir) {
			RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(d));
			if (r!=null) {
				if (r.getTeam() != rc.getTeam()) {
					if (rc.canDepositDirt(d)) {
						rc.depositDirt(d);
						System.out.println("Nasipavamo "+rc.getLocation().add(d));
						return true;
					}
				}
			}
		}
		rc.depositDirt(Direction.CENTER);
		return true;
	}

	private MapLocation findClosestEnemyBuilding() {
		MapLocation eb = null;
		int dist = 100000;
		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
			if (rc.getLocation().distanceSquaredTo(r.location) < dist&&(r.getType() == RobotType.DESIGN_SCHOOL || r.getType() == RobotType.FULFILLMENT_CENTER || r.getType() == RobotType.NET_GUN)) {
				dist = rc.getLocation().distanceSquaredTo(r.location);
				eb = r.location;
			}
		}
		return eb;
	}

	private void makeAWall() throws GameActionException {
//		System.out.println("Walling");
		int dist = Util.d_inf(rc.getLocation(), hq);
		if (dist == 1) {
			if (rc.getDirtCarrying() > 0) {
				int minHeight = c.inf;
				Direction best = null;
				for (Direction d : Direction.allDirections()) {
					MapLocation goal = rc.getLocation().add(d);
					if (Util.d_inf(hq, goal) == 1) {
						int h = rc.senseElevation(goal);
						if (h < minHeight) {
							minHeight = h;
							best = d;
						}
					}
				}
				rc.depositDirt(best);
				return;
			} else {
				for (Direction d : Util.dir) {
					MapLocation goal = rc.getLocation().add(d);
					for (MapLocation m : holes) {
						if (m.equals(goal)) {
							rc.digDirt(d);
							return;
						}
					}
				}
			}
		} else {
			if (rc.getDirtCarrying() > 0) {
				if (Math.random() < 0.3) {
					rc.depositDirt(Direction.CENTER);
					return;
				} else {
					int minHeight = c.inf;
					Direction best = null;
					for (Direction d : Util.dir) {
						MapLocation goal = rc.getLocation().add(d);
						if (Util.d_inf(hq, goal) == 1) {
							int h = rc.senseElevation(goal);
							if (h < minHeight) {
								minHeight = h;
								best = d;
							}
						}
					}
					rc.depositDirt(best);
					return;
				}
			} else {
				for (Direction d : Util.dir) {
					int dist2 = Util.d_inf(rc.getLocation().add(d), hq);
					if (dist2 == 3 && rc.canDigDirt(d)) {
						rc.digDirt(d);
						return;
					}
				}
				return;
			}
		}

	}

	@Override
	public void postcompute() throws GameActionException {
		for(int i=0;i<net_guns.load;i++) {
			MapLocation m=net_guns.get(i);
			if(m==null) {
				continue;
			}
			if(rc.canSenseLocation(m)) {
				RobotInfo r=rc.senseRobotAtLocation(m);
				if(r!=null&&r.type==RobotType.NET_GUN) {
					
				}else {
					net_guns.remove(r.location);
					b.send_location(b.LOC_ENEMY_NETGUN_GONE, r.location);
				}
			}
		}
		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	private void doAttacking() throws GameActionException {
		System.out.println("Attacking");
		// poišèemo najblizjo nasprotnikovo stavbo, jo zasipamo ali gremo do nje
		MapLocation eb = null;
		int dist = 100000;
		for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
			if (r.getType() == RobotType.DESIGN_SCHOOL || r.getType() == RobotType.HQ || r.getType() == RobotType.FULFILLMENT_CENTER || r.getType() == RobotType.NET_GUN) {
				if (rc.getLocation().distanceSquaredTo(r.location) < dist) {
					dist = rc.getLocation().distanceSquaredTo(r.location);
					;
					eb = r.location;
				}
			}
		}
		for (Direction d : Util.dir) {
			RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(d));
			if (r == null) {
				continue;
			}
			if (r.getType() == RobotType.DESIGN_SCHOOL || r.getType() == RobotType.HQ || r.getType() == RobotType.FULFILLMENT_CENTER || r.getType() == RobotType.NET_GUN) {
				if (r.getTeam() == rc.getTeam()) {
					if (rc.canDigDirt(d)) {
						rc.digDirt(d);
						return;
					}
				} else {
					if (rc.canDepositDirt(d)) {
						rc.depositDirt(d);
						return;
					} else {
						if (rc.canDigDirt(d.opposite())) {
							rc.digDirt(d.opposite());
							return;
						}
					}
				}
			}
		}
		Direction d = Util.tryMove(rc, rc.getLocation().directionTo(eb));
		if (d != null) {
			rc.move(d);
		}
	}

	private void doLandscaping() throws GameActionException {
//		System.out.println("Landscaping");
//		if (rc.getRoundNum() < 300) {
//			// buildhq
//			if (hqHeight != -1000) {
//				int l1h = hqHeight + 3;
//				int l2h = l1h + 3;
//				int l3h = l1h;
//				int dist = Util.d_inf(hq, rc.getLocation());
//				if (dist > 5) {
//					Direction d = Util.tryMove(rc, rc.getLocation().directionTo(hq));
//					if (d != null && rc.canMove(d)) {
//						rc.move(d);
//					}
//				} else {
//					if (rc.getDirtCarrying() > 0) {
//						for (Direction d : Util.dir) {
//							int dist2 = Util.d_inf(hq, rc.getLocation().add(d));
//							if (dist2 < 3) {
//								int h;
//								switch (dist2) {
//								case 1:
//									h = l1h;
//									break;
//								case 2:
//									h = l2h;
//									break;
//								case 3:
//									h = l3h;
//									break;
//								default:
//									h = -1;
//									break;
//								}
//								if(rc.senseElevation(rc.getLocation().add(d))<h) {
//									rc.depositDirt(d);
//									return;
//								}
//							}
//						}
//					} else {
//						if (rc.canDigDirt(rc.getLocation().directionTo(hq).opposite())) {
//							rc.digDirt(rc.getLocation().directionTo(hq).opposite());
//							return;
//						}else {
//							Direction d=Util.getRandomDirection();
//							if(rc.canMove(d)) {
//								rc.move(d);
//							}
//						}
//					}
//				}
//			} else {
//				if (rc.canSenseLocation(hq)) {
//					hqHeight = rc.senseSoup(hq);
//				} else {
//					Direction d = Util.tryMove(rc, rc.getLocation().directionTo(hq));
//					if (d != null && rc.canMove(d)) {
//						rc.move(d);
//					}
//				}
//			}
//			return;
//		}
		if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
			for (Direction d : Util.dir) {
				boolean close_to_hq = Util.d_inf(rc.getLocation().add(d), hq) < 5;
				int goal = close_to_hq ? roundHqHeight : visina;
				// izkopljemo hrib ce ni previsok
				if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseElevation(rc.getLocation().add(d)) > goal && rc.senseElevation(rc.getLocation().add(d)) < omejitev_visine) {
					if (rc.canDigDirt(d)) {
						rc.digDirt(d);
						rc.setIndicatorDot(rc.getLocation().add(d), 255, 0, 0);
						return;
					}
				}
			}
			for (Direction d : Util.dir) {
				boolean close_to_hq = Util.d_inf(rc.getLocation().add(d), hq) < 5;
				int goal = close_to_hq ? roundHqHeight : visina;
				if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseElevation(rc.getLocation().add(d)) > goal) {// poskusimo s kopanjem zmanjsati hrib
					if (rc.canDigDirt(d)) {
						rc.digDirt(d);
						rc.setIndicatorDot(rc.getLocation().add(d), 255, 0, 0);
						return;
					}
				}
			}
			for (Direction d : Util.dir) {
				if (primerna_luknja(d, px, py)) {
					if (rc.canDigDirt(d)) {
						rc.digDirt(d);
						rc.setIndicatorDot(rc.getLocation().add(d), 255, 0, 0);
						return;
					}
				}
			}
			// nismo nasli primerne luknje...
			explore();
		} else {
			for (Direction d : Util.dir) {
				boolean close_to_hq = Util.d_inf(rc.getLocation().add(d), hq) < 5;
				int goal = close_to_hq ? roundHqHeight : visina;
				if (!primerna_luknja(d, px, py)) {
					if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseElevation(rc.getLocation().add(d)) < goal && rc.senseElevation(rc.getLocation().add(d)) > -omejitev_visine) { // landscapati
						if (rc.canDepositDirt(d) && noAllyBuilding(d)) {
							rc.depositDirt(d);
							rc.setIndicatorDot(rc.getLocation().add(d), 0, 255, 0);
							return;
						}
					}
				}
			}
			explore();
		}
	}

	private boolean noAllyBuilding(Direction d) throws GameActionException {
		if (!rc.canSenseLocation(rc.getLocation().add(d))) {
			return false;
		}
		RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(d));
		if (r == null) {
			return true;
		}
		if (r.type == RobotType.DESIGN_SCHOOL || r.type == RobotType.FULFILLMENT_CENTER || r.type == RobotType.HQ) {
			return false;
		}
		return true;
	}

	private void explore() throws GameActionException {
		if (explore == null) {
			explore = Util.getRandomDirection();
		}
		Direction d = Util.tryMove(rc, explore);
		if (d != null && !primerna_luknja(d, px, py)) {
			rc.move(d);
		} else {
			explore = null;
		}
	}

	private boolean primerna_luknja(Direction d, int px, int py) {
		int xm2 = rc.getLocation().add(d).x % 2;
		int ym2 = rc.getLocation().add(d).y % 2;
		return xm2 == px && ym2 == py;
	}

	public void bc_base_strategy(int[] message) {
		strategy = message[2];
	}

	@Override
	public void bc_home_hq(MapLocation pos) {
		hq = pos;
	}
	
	@Override
	public void bc_enemy_netgun(MapLocation pos) {
		if (!net_guns.contains(pos)) {
			net_guns.add(pos);
		}
	}
	
	@Override
	public void bc_enemy_netgun_gone(MapLocation pos) {
		if (net_guns.contains(pos)) {
			net_guns.remove(pos);
		}
	}
}
