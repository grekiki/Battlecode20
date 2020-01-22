package grekiki3;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class landscaper extends robot {
	int strategy = -1;
	boolean attacking = false;
	MapLocation hq = null;
	final static int visina = 8;
	final static int omejitev_visine = 200;
	Direction explore = null;
	ArrayList<MapLocation> wall1;
	int px;
	int py;
	MapLocation goal = null;
	int previousRound = -1;
	MapLocation[] holes;

	public landscaper(RobotController rc) {
		super(rc);
	}

	@Override
	public void init() throws GameActionException {
		while (Clock.getBytecodesLeft() > 2000 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				break;
			}
		}
		if (hq == null && strategy == 1000) {
			attacking = true;
		}
		wall1 = new ArrayList<MapLocation>();
		for (int x = -2; x <= 2; x++) {
			for (int y = -2; y <= 2; y++) {
				if (x != 0 || y != 0) {
					int ax = Math.abs(x);
					int ay = Math.abs(y);
					if ((ax == 2 && ay == 0) || (ax == 0 && ay == 2)) {

					} else {
						wall1.add(new MapLocation(hq.x + x, hq.y + y));
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
			analyze_drone_delivery_failure();
		}
		previousRound = rc.getRoundNum();
		px = hq.x % 2;
		py = hq.y % 2;
		if (!attacking) {
			if (rc.getRoundNum() >= 300 && goal == null) {
				int t = (int) Math.floor(Math.random() * wall1.size());
				goal = wall1.get(t);
				System.out.println(goal);
				b.send_location2(b.LOC2_DRONE, rc.getLocation(), wall1.get(t), rc.getID());
			}
			if (rc.getRoundNum() > 300 && Math.random() < 1.0 / 30 && !rc.getLocation().equals(goal)) {
				b.send_location2(b.LOC2_DRONE, rc.getLocation(), goal, rc.getID());
			}
		}
	}

	private void analyze_drone_delivery_failure() throws GameActionException {
		if (goal != null && rc.getLocation().equals(goal)) {
			return;
		}
		for (int t = 0; t < wall1.size(); t++) {
			MapLocation m = wall1.get(t);
			if (rc.canSenseLocation(m)) {
				RobotInfo r = rc.senseRobotAtLocation(m);
				if (r != null && r.type == RobotType.LANDSCAPER && r.team == rc.getTeam()) {
					wall1.remove(m);
					t--;
				}
			}
		}
		if(wall1.size()==0) {
			goal=null;
			return;
		}
		int t = (int) Math.floor(Math.random() * wall1.size());
		goal = wall1.get(t);
		System.out.println(goal);
		b.send_location2(b.LOC2_DRONE, rc.getLocation(), wall1.get(t), rc.getID());
		
	}

	@Override
	public void runTurn() throws GameActionException {
		if(!rc.isReady()) {
			return;
		}
		if (goal != null && rc.getLocation().equals(goal)) {
			makeAWall();
		} else if (attacking) {
			doAttacking();
		} else {
			doLandscaping();
		}
	}

	private void makeAWall() throws GameActionException {
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
				rc.digDirt(rc.getLocation().directionTo(hq).opposite());
				return;
			}
		}

	}

	@Override
	public void postcompute() throws GameActionException {
		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	private void doAttacking() throws GameActionException {
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
		System.out.println("Landscaping");
		int goal = Util.d_inf(rc.getLocation(), hq) < 5 ? 5 : visina;
		if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
			for (Direction d : Util.dir) {
				// izkopljemo hrib ce ni previsok
				if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseElevation(rc.getLocation().add(d)) > goal && rc.senseElevation(rc.getLocation().add(d)) < omejitev_visine) {
					if (rc.canDigDirt(d)) {
						rc.digDirt(d);
						return;
					}
				}
			}
			for (Direction d : Util.dir) {
				if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseElevation(rc.getLocation().add(d)) > goal) {// poskusimo s kopanjem zmanjsati hrib
					if (rc.canDigDirt(d)) {
						rc.digDirt(d);
						return;
					}
				}
			}
			for (Direction d : Util.dir) {
				if (primerna_luknja(d, px, py)) {
					if (rc.canDigDirt(d)) {
						rc.digDirt(d);
						return;
					}
				}
			}
			// nismo nasli primerne luknje...
			explore();
		} else {
			for (Direction d : Util.dir) {
				if (!primerna_luknja(d, px, py)) {
					if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseElevation(rc.getLocation().add(d)) < goal && rc.senseElevation(rc.getLocation().add(d)) > -omejitev_visine) { // landscapati
						if (rc.canDepositDirt(d) && noAllyBuilding(d)) {
							rc.depositDirt(d);
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
}
