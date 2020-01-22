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
		for (Direction d : Util.dir) {
			if (rc.onTheMap(hq.add(d))) {
				wall1.add(hq.add(d));
			}
		}
	}

	@Override
	public void precompute() throws GameActionException {
		px = hq.x % 2;
		py = hq.y % 2;
		System.out.println("Ravnanje");
		if (rc.getRoundNum() == 300) {
			System.out.println(wall1);
			int t = (int) Math.floor(Math.random() * wall1.size());
			goal = wall1.get(t);
			System.out.println(goal);
			b.send_location2(b.LOC2_DRONE, rc.getLocation(), wall1.get(t), rc.getID());
			System.out.println("Gremo na zid.");
		}
		if (rc.getRoundNum() > 300 && rc.getRoundNum() % 50 == 0 && (!rc.getLocation().equals(goal)&&!rc.getLocation().isAdjacentTo(goal))) {
			b.send_location2(b.LOC2_DRONE, rc.getLocation(), goal, rc.getID());
		}
		System.out.println("blockchain.... " + b.messages.size());
	}

	@Override
	public void runTurn() throws GameActionException {
		if (goal != null && rc.getLocation().equals(goal)||rc.getLocation().isAdjacentTo(goal)) {
			makeAWall();
		} else if (attacking) {
			doAttacking();
		} else {
			doLandscaping();
		}
	}

	private void makeAWall() {
		return;
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
		if (rc.getDirtCarrying() == 0) {
			for (Direction d : Util.dir) {
				if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseElevation(rc.getLocation().add(d)) > visina && rc.senseElevation(rc.getLocation().add(d)) < omejitev_visine) {
					if (rc.canDigDirt(d)) {
						rc.digDirt(d);
						return;
					}
				}
			}
			for (Direction d : Util.dir) {
				if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseElevation(rc.getLocation().add(d)) > visina) {// poskusimo s kopanjem zmanjsati hrib
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
					if (rc.canSenseLocation(rc.getLocation().add(d)) && rc.senseElevation(rc.getLocation().add(d)) < visina && rc.senseElevation(rc.getLocation().add(d)) > -omejitev_visine) {// poskusimo
																																																// s
																																																// nalaganjem
																																																// landscapati
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
