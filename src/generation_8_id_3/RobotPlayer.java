package generation_8_id_3;

import java.util.ArrayList;
import java.util.Arrays;

import battlecode.common.*;

/*
 * Sifrirni sistem za sporocila. 
 * Prvi int je privatna cifra ki jo uporabimo za sifriranje. 
 * Drugi je tip sporocila.
 * msg[2]...msg[6] je vsebina
 * 
 * Tukaj bo slovar ki pove kaj dolocena stevilka pomeni ce je navedena kot vsebina
 * 
 * msg[1]==1--> msg[2] pove fazo v kateri je trenutno baza. msg[3] pove kateri worker gre gradit bazo
 * 
 * msg[1]==2--> msg[2],msg[3] so koordinate polja surovin.
 * 
 * msg[1]==3--> Polje na msg[2],msg[3] je prazno. 
 * 
 * msg[1]==4--> na msg[2],msg[3] sedaj stoji refinerija. 
 * 
 * msg[1]==5--> na msg[2],msg[3] sedaj stoji tovarna dronov. 
 * 
 * msg[1]==6--> na msg[2],msg[3] so koordinate na�ega hq. 
 * 
 * msg[1]==7--> enota bi �la iz msg[2],msg[3] v msg[4],msg[5]. Za prevoz potrebuje drona. 
 */

/*
 * Faze:
 * Faza 0: Nakup minerjev ki cimprej poiscejo kaksno surovino
 * 
 * Faza 1: Razvoj ekonomije. ce imajo minerji oznacena kaksna dobra surovinska polja, naj razmislijo
 * o rafineriji ce je to dalec, ali pa naj se naberejo ce je blizu. Nekje 6 minerjev lahko precej v redu zapolni rafinerijo. 
 * Rafinerije se gradi stran od baze, da ne skodimo zidu z pollutionom. 
 * Premik v fazo 3 naredimo ko imamo dovolj surovin za to. 
 * 
 * Faza 3: Sedaj bi morali imeti ogromno surovin, in se lahko lotimo napada/obrambe. Baza pokli�e enega minerja nazaj.
 * 
 * Faza 4: Baza ima zid, ki pa ga je treba izbolj�ati. Dronov se ne gradi prevec. 
 * 
 * 
 * Celoten plan temelji bolj na ekonomiji, tako da se verjetno splaca biti previden glede invazij.  
 * 
 */

/*Plan baze
 *             Faza 0:
 *              ...
 *              .B.
 *              ...
 *              
 *              V fazi 1 naredimo tovarne za drone
 *              D..
 *              .B.
 *              ..D
 *              V fazi 2 naredimo tovarne za diggerje
 *              D..
 *              .H.
 *              ..L
 * 				V fazi 3 okoli baze okrepimo obrambo
 * 				NNNNNNN...
 *              NWWWWWN
 *              NWD..WN
 *              NW.H.WN
 *              NW..LWN
 *              NWWWWWN
 *              NNNNNNN
 * 	
 */

/*
 * TODO list
 * -> Branje blockchaina se mora dovolj hitro ustaviti, in se nadaljuje med igro bota. Ne moremo vsega prebrati na za�etku! Oziroma
 * lahko ampak le do morda poteze 500
 */
class blockchain {
	ArrayList<paket> msg;
	RobotController rc;

	blockchain(RobotController rc) {
		this.rc = rc;
		msg = new ArrayList<paket>();
	}

	public void sendMsg(paket p) throws GameActionException {
		if (rc.canSubmitTransaction(p.data, p.cost)) {
			rc.submitTransaction(p.data, p.cost);
			System.out.println(rc.getLocation()+" je poslal posto");
			msg.add(new paket(p.data, p.cost));
		} else {
			msg.add(new paket(p.data, 0));
		}
	}

	public void checkQueue() throws GameActionException {
		int minCost = 1000000;
		Transaction[] t = rc.getBlock(rc.getRoundNum() - 1);
		for (int i = 0; i < msg.size(); i++) {
			paket p = msg.get(i);
			for (Transaction tt : t) {
				if (Arrays.equals(tt.getMessage(), p.data)) {
					msg.remove(p);
					i--;
					break;
				}
				minCost = Math.min(minCost, tt.getCost());
			}
		}
		minCost = (minCost == 1000000 ? konst.min_cena_transakcije : minCost);
		for (int i = 0; i < msg.size(); i++) {
			paket p = msg.get(i);
			if (p.cost >= minCost) {// Placali smo ze dovolj
				continue;
			}
			int[] msgg = p.data;
			if (rc.canSubmitTransaction(msgg, minCost)) {
				rc.submitTransaction(msgg, minCost);
				msg.remove(p);
				System.out.println("Poslano");
				i--;
			}
		}
//		System.out.println(msg.size() + " paketov caka");
	}

}

class Util {
	public static Direction[] dir = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };

	// Garantirano terminira
	public static Direction tryMoveLite(RobotController rc, Direction dir) throws GameActionException {
		if (rc.canMove(dir) && rc.canSenseLocation(rc.getLocation().add(dir)) && !rc.senseFlooding(rc.getLocation().add(dir))) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateLeft())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateLeft()))) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateRight())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateRight()))) {
			return dir.rotateRight();
		} else {
			return null;
		}
	}

	public static Direction tryMove(RobotController rc, Direction dir) throws GameActionException {
		if (rc.canMove(dir) && rc.canSenseLocation(rc.getLocation().add(dir)) && !rc.senseFlooding(rc.getLocation().add(dir))) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateLeft())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateLeft()))) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateRight())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateRight()))) {
			return dir.rotateRight();
		} else if (rc.canMove(dir.rotateLeft().rotateLeft()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateLeft().rotateLeft())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateLeft().rotateLeft()))) {
			return dir.rotateLeft().rotateLeft();
		} else if (rc.canMove(dir.rotateRight().rotateRight()) && rc.canSenseLocation(rc.getLocation().add(dir.rotateRight().rotateRight())) && !rc.senseFlooding(rc.getLocation().add(dir.rotateRight().rotateRight()))) {
			return dir.rotateRight().rotateRight();
		} else {
			return null;
		}
	}

	public static Direction getRandomDirection() {
		return dir[(int) Math.floor(dir.length * Math.random())];
	}
	
	public static int d_inf(MapLocation l1,MapLocation l2) {//d neskon�no metrika. 
		return Math.max(Math.abs(l2.x-l1.x),Math.abs(l2.y-l1.y));
	}

	public static Direction tryMoveLiteDrone(RobotController rc,Direction dir){
		if (rc.canMove(dir)) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		} else if (rc.canMove(dir.rotateLeft().rotateLeft())) {
			return dir.rotateLeft().rotateLeft();
		} else if (rc.canMove(dir.rotateRight().rotateRight())) {
			return dir.rotateRight().rotateRight();
		} else {
			return null;
		}
	}
	public static Direction tryMoveDrone(RobotController rc,Direction dir){
		if (rc.canMove(dir)) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		} else {
			return null;
		}
	}
}

abstract class robot {
	public abstract void init() throws Exception;

	public abstract void precompute() throws Exception;

	public abstract void runTurn() throws Exception;

	public abstract void postcompute() throws Exception;

}

public strictfp class RobotPlayer {
	static RobotController rc;

	public static void run(RobotController rci) {
		rc = rci;
		robot r = null;
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
		try {
			r.init();
		}catch(ArithmeticException e) {
			System.out.println("DIE!");
			int t=1/0;
			System.out.println(t);
		}catch (Exception e) {
		
			System.out.println(Arrays.toString(e.getStackTrace()));
			System.out.println(e.getMessage());
		}
		while (true) {
			try {
				int init=rc.getRoundNum();
				r.precompute();
				r.runTurn();
				r.postcompute();
				if(rc.getRoundNum()!=init) {
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
