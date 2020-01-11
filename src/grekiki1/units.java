package grekiki1;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import battlecode.world.GameWorld;

class miner extends robot {
	int round;
	int alive = 0;
	RobotController rc;
	MapLocation HQ;
	vector_gl2 surovine = new vector_gl2();

	miner(RobotController rc) {
		this.rc = rc;
		round = rc.getRoundNum();
	}

	@Override
	public void precompute() {
		if (alive == 0) {
			for (RobotInfo r : rc.senseNearbyRobots(2)) {
				if (r.team == rc.getTeam() && r.type == RobotType.HQ) {
					HQ = r.location;
				}
			}
			if (HQ == null) {
				alive--;// �e ne vemo kje je baza, a se sploh spla�a obstajati?
			}
		}
		for (int i = 0; i < surovine.size; i++) {
			System.out.println(surovine.get(i).x + " " + surovine.get(i).y);
		}
		if (round > 0) {
			try {
				Transaction[] q = rc.getBlock(round - 1);
				for (Transaction t : q) {
//					System.out.println("Prejeto "+Arrays.toString(t.getMessage()));
					if (t.getMessage()[6] == 42 && t.getMessage()[0] == round - 1) {
						if (t.getMessage()[1] == 1) {// surovina
							if (!surovine.contains(new MapLocation(t.getMessage()[2], t.getMessage()[3]))) {
								surovine.add(new MapLocation(t.getMessage()[2], t.getMessage()[3]));
//								System.out.println("Dodana surovina "+new MapLocation(t.getMessage()[2],t.getMessage()[3]).x+" "+new MapLocation(t.getMessage()[2],t.getMessage()[3]).y);
							} else {
//								System.out.println("Surovina obstaja?");
							}
						} else if (t.getMessage()[1] == 2) {
							MapLocation rem = new MapLocation(t.getMessage()[2], t.getMessage()[3]);
							surovine.remove(rem);
						}
					}
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void runTurn() {
		try {
			if (rc.isReady() && alive > 0) {// inicializacija je bila upamo da uspe�na
				Direction[] dirs = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
						Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };
				if (rc.getSoupCarrying() == 100) {
					Direction dirHQ = rc.getLocation().directionTo(HQ);
					if (rc.canMove(dirHQ) && !rc.senseFlooding(rc.getLocation().add(dirHQ))) {
						rc.move(rc.getLocation().directionTo(HQ));
						return;
					}
				}
				for (Direction d : dirs) {
					if (rc.canMineSoup(d)) {
						rc.mineSoup(d);
						return;
					}
				}
				for (Direction d : dirs) {
					if (rc.canDepositSoup(d)) {
						rc.depositSoup(d, rc.getSoupCarrying());
						return;
					}
				}
				if (surovine.size > 0) {
					MapLocation best = null;
					int shortest = 64 * 64 + 1;
					for (int i = 0; i < surovine.size; i++) {
						MapLocation ml = surovine.get(i);
						int dist = (rc.getLocation().x - ml.x) * (rc.getLocation().x - ml.x)
								+ (rc.getLocation().y - ml.y) * (rc.getLocation().y - ml.y);
						if (dist < shortest) {
							shortest = dist;
							best = ml;
						}
					}
					Direction d = rc.getLocation().directionTo(best);
					if (rc.canMove(d) && !rc.senseFlooding(rc.getLocation().add(d))) {
						rc.move(d);
						return;
					}
				}
				for (int i = 0; i < 10; i++) {
					Direction d = dirs[(int) (Math.random() * dirs.length)];
					if (rc.canMove(d) && !rc.senseFlooding(rc.getLocation().add(d))) {
						rc.move(d);
						return;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postcompute() {// Drage operacije so lahko le tukaj. Potrebno je preverjati koliko bytecodov je
								// �e ostalo!
		alive++;
		round++;
		int range = rc.getCurrentSensorRadiusSquared();
		MapLocation curr = rc.getLocation();
		for (int i = 0; i < surovine.size; i++) {
			MapLocation check = surovine.get(i);
			try {
				if (rc.canSenseLocation(check) && rc.senseSoup(check) == 0) {
					int[] block = bitcoin.generateMessage("odstrani surovino", new int[] { check.x, check.y, 0, 0 },
							round - 1);
					if (rc.canSubmitTransaction(block, 2)) {
						rc.submitTransaction(block, 2);
						rc.setIndicatorDot(check, 255, 0, 0);
//						System.out.println("Poslano"+Arrays.toString(block));
						// System.out.println(t.x+" "+t.y);
					}
				}
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (int dx = 0; dx + dx < range; dx++) {
			int used = dx * dx;
			for (int dy = 0; dy * dy < range - used; dy++) {
				MapLocation[] tocke = { new MapLocation(curr.x + dx, curr.y + dy),
						new MapLocation(curr.x - dx, curr.y + dy), new MapLocation(curr.x + dx, curr.y - dy),
						new MapLocation(curr.x - dx, curr.y - dy) };
				for (MapLocation t : tocke) {
					if (Clock.getBytecodesLeft() > 1000) {
						try {
							if (rc.canSenseLocation(t)) {
								int soup = rc.senseSoup(t);
								if (soup > 0) {
									if (!surovine.contains(t)) {
										int money = rc.getTeamSoup();
										if (money > 5) {
											int[] block = bitcoin.generateMessage("surovina",
													new int[] { t.x, t.y, 0, 0 }, round - 1);
											if (rc.canSubmitTransaction(block, 2)) {
												rc.submitTransaction(block, 2);
												rc.setIndicatorDot(t, 0, 0, 255);
//												System.out.println("Poslano"+Arrays.toString(block));
												// System.out.println(t.x+" "+t.y);
											}
										}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						System.out.println(dx + " " + dy);
						return;
					}
				}
			}
		}
	}
}