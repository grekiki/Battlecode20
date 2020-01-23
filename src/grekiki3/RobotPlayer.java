package grekiki3;

import battlecode.common.*;

abstract class robot {
	RobotController rc;
	blockchain b;

	public robot(RobotController rc) {
		this.rc = rc;
		this.b = new blockchain(rc) {
			@Override
			public void handle_location(int type, MapLocation pos) throws GameActionException {
//				System.out.println("" + type + " " + pos);
				switch (type) {
				case LOC_SUROVINA:
					bc_polje_found(pos);
					break;
				case LOC_SUROVINA_PRAZNO:
					bc_polje_empty(pos);
					break;
				case LOC_SLABA_SUROVINA:
					bc_polje_slabo(pos);
					break;
				case NADGRADNJA_SUROVINE:
					bc_polje_upgrade(pos);
					break;
				case LOC_REFINERIJA:
					bc_rafinerija(pos);
					break;
				case LOC_TOVARNA_DRONOV:
					bc_tovarna_dronov(pos);
					break;
				case BUILD_TOVARNA_DRONOV:
					bc_build_tovarna_dronov(pos);
					break;
				case BUILD_TOVARNA_LANDSCAPERJEV:
					bc_build_tovarna_landscaperjev(pos);
					break;
				case LOC_HOME_HQ:
					bc_home_hq(pos);
					break;
				case LOC_ENEMY_HQ:
					bc_enemy_hq(pos);
					break;
				case LOC_WATER:
					bc_water(pos);
					break;
				case LOC_ENEMY_NETGUN:
					bc_enemy_netgun(pos);
					break;
				case LOC_ENEMY_NETGUN_GONE:
					bc_enemy_netgun_gone(pos);
					break;
				case LOC_ALLY_NETGUN:
					bc_ally_netgun(pos);
					break;
				case LOC_MINER_DRONE:
					bc_tovarna_dronov(pos);
					break;
				case LOC_MINER_LANDSCAPER:
					bc_tovarna_landscaperjev(pos);
					break;
				}
			}

			@Override
			public void handle_location2(int type, MapLocation m1, MapLocation m2, int id) {
				switch (type) {
				case LOC2_DRONE:
					bc_drone(m1, m2, id);
					break;
				case LOC2_DRONE_COMPLETE:
					bc_drone_complete(m1, m2, id);
					break;
				case LOCP_DRONE_ASSIST:
					bc_drone_assist(m1, id);
					break;
				case LOCP_DRONE_ASSIST_CLEAR:
					bc_drone_assist_clear(m1, id);
					break;
				case LOCP_DRONE_ATTACK:
					bc_drone_attack(m1, id);
					break;
				case LOCP_DRONE_ATTACK_STOP:
					bc_drone_attack_stop(m1);
					break;
				}
			}

			@Override
			public void handle_packet(int type, int[] message) throws GameActionException {
				switch (type) {
				case MINER_HELP_HQ:
					bc_miner_to_help(message);
					break;
				case MINER_RUSH:
					bc_miner_rush(message);
					break;
				case UNIT_ALIVE:
					bc_unit_alive(message);
					break;
				case BASE_STRATEGY:
					bc_base_strategy(message);
					break;
				case FULL_WALL:
					bc_full_wall(message);
					break;
				case RUSH:
					bc_rush(message);
					break;
				}
			}

		};
	}

	/**
	 * Ta metoda se poklice ko se robot spawna.
	 * 
	 * @throws Exception
	 */
	public abstract void init() throws GameActionException;

	public abstract void precompute() throws GameActionException;

	public abstract void runTurn() throws GameActionException;

	public abstract void postcompute() throws GameActionException;

	// bc_* metode se sprozijo, ko iz blockchaina preberemo ustrezen tip podatka.
	// Roboti implementirajo samo tiste metode, ki jih zelijo sprejeti.
	// Za branje blockchaina se uporabi: b.read_next_round();
	public void bc_polje_found(MapLocation pos) {
	}

	public void bc_polje_empty(MapLocation pos) {
	}

	public void bc_polje_slabo(MapLocation pos) {
	}

	public void bc_polje_upgrade(MapLocation pos) {
	}

	public void bc_rafinerija(MapLocation pos) {
	}

	public void bc_tovarna_dronov(MapLocation pos) {
	}

	public void bc_build_tovarna_dronov(MapLocation pos) {
	}

	public void bc_tovarna_landscaperjev(MapLocation pos) {
	};

	public void bc_build_tovarna_landscaperjev(MapLocation pos) {
	};

	public void bc_home_hq(MapLocation pos) {
	}

	public void bc_enemy_hq(MapLocation pos) throws GameActionException {
	}

	public void bc_drone(MapLocation from, MapLocation to, int id) {
	}

	public void bc_drone_complete(MapLocation from, MapLocation to, int id) {
	}

	public void bc_drone_assist(MapLocation pos, int priority) {
	}

	public void bc_drone_assist_clear(MapLocation pos, int priority) {
	}

	public void bc_drone_attack(MapLocation pos, int round) {
	}

	public void bc_drone_attack_stop(MapLocation pos) {
	}

	public void bc_water(MapLocation pos) {
	}

	public void bc_enemy_netgun(MapLocation pos) {
	}

	public void bc_enemy_netgun_gone(MapLocation pos) {
	}

	public void bc_ally_netgun(MapLocation pos) {
	}

	public void bc_miner_to_help(int[] message) {
	}

	public void bc_unit_alive(int[] message) {
	}

	public void bc_full_wall(int[] message) throws GameActionException {
	}

	public void bc_miner_rush(int[] message) {
	}
	public void bc_rush(int[] message) {
	}

	public void bc_base_strategy(int[] message) {
	}
}

public strictfp class RobotPlayer {
	static RobotController rc;

	public static void run(RobotController rci) throws GameActionException {
		rc = rci;
		robot r = null;
		try {
			switch (rc.getType()) {
			case HQ:
				r = new HQ(rc);
				break;
			case MINER:
				r = new miner(rc);
				break;
			case REFINERY:
				r = new refinery(rc);
				break;
			case VAPORATOR:
				r = new vaporator(rc);
				break;
			case DESIGN_SCHOOL:
				r = new design_school(rc);
				break;
			case FULFILLMENT_CENTER:
				r = new fulfillment_center(rc);
				break;
			case LANDSCAPER:
				r = new landscaper(rc);
				break;
			case DELIVERY_DRONE:
				r = new delivery_drone(rc);
				break;
			case NET_GUN:
				r = new net_gun(rc);
				break;
			case COW:
				break;
			default:
				break;
			}
		} catch (Exception e) {
			System.out.println(rc.getType() + " Exception");
			e.printStackTrace();
		}
		try {
			r.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (true) {
			try {
				int init = rc.getRoundNum();
				if (init > 1) {
					r.b.checkQueue();
					r.b.read_next_round();
				}
				r.precompute();
				r.runTurn();
				r.postcompute();
				if (rc.getRoundNum() != init) {
					System.out.println("Prevec racunanja!");
				}
				Clock.yield();
			} catch (Exception e) {
				e.printStackTrace();
				Clock.yield();
			}

		}
	}

}
