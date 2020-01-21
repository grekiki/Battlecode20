package grekiki3;

import java.util.ArrayList;
import java.util.Arrays;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

class vector_set_gl {
	private boolean[][] grid;
	MapLocation[] q;
	public int size = 0;
	private int cappacity = 20;
	private int load = 0;

	vector_set_gl() {
		q = new MapLocation[20];
		grid = new boolean[64][];
		grid[0] = new boolean[64];
		for (int i = 1; i < 64; i++) {
			grid[i] = grid[0].clone();
		}
	}

	void add(MapLocation ml) {
		if (!grid[ml.x][ml.y]) {
			load++;
			grid[ml.x][ml.y] = true;
			q[size++] = ml;
			if (size == cappacity) {// Redek pojav

				// Ce je load factor precej velik potem kloniramo. Drugace cistimo
				// Problem: Duplikati bodo ostali v klonu...
				double limit = 0.7;
				if (load / (double) cappacity > limit) {
					q = Arrays.copyOf(q, 2 * cappacity);
					cappacity *= 2;
				} else {
					MapLocation[] q2 = new MapLocation[cappacity];
					int p = 0;
					for (int i = 0; i < size; i++) {
						if (grid[q[i].x][q[i].y]) {
							q2[p++] = q[i];
						}
					}
					q = q2;
					size = load;
				}
			}
		}

	}

	MapLocation get(int a) {
		if (grid[q[a].x][q[a].y]) {
			return q[a];
		} else {
			return null;
		}
	}

	void remove(MapLocation ml) {
		if (grid[ml.x][ml.y]) {
			load--;
			grid[ml.x][ml.y] = false;
		}
	}

	boolean contains(MapLocation m) {
		return grid[m.x][m.y];
	}
}

class MapCell {
	int soupCount = 0;
	int pollutionLevel = 0;
	int elevation = 0;
	boolean flooded = false;
	int lastTurnSeen = -1;
	RobotInfo robot = null;

	MapCell(int soup, int pollution, int elevation, boolean flooded, int lastSeen, RobotInfo robot) {
		soupCount = soup;
		pollutionLevel = pollution;
		this.elevation = elevation;
		this.flooded = flooded;
		lastTurnSeen = lastSeen;
		this.robot = robot;
	}
}

class minerPathFinder extends BasePathFinder {
	minerPathFinder(RobotController rc) {
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

class naloga {
	// ID-ji nalog ki jih lahko delamo
	final static int GRADNJA_REFINERIJE = 10;
	final static int GRADNJA_TOVARNE_ZA_DRONE = 11;
	final static int GRADNJA_TOVARNE_ZA_LANDSCAPERJE = 12;
	final static int NABIRANJE = 20;
	final static int PREMIK_DO_JUHE = 30;
	final static int PREMIK_DO_POLJA = 31;
	final static int PREMIKANJE_JUHE_V_BAZO = 40;
	final static int RAZISKOVANJE_JUHE = 50;
	final static int RAZISKOVANJE_MAPE = 60;
	miner m;
	MapLocation mesto;// ni nujno da naloga da mesto, je pa tako pogosto da pride prav
	int value;
	int type;
	int zacetek;
	RobotController rc;

	Direction raziskovanje = null;

	naloga(miner mi, MapLocation mm, int c, int t) {
		m = mi;
		rc = m.rc;
		mesto = mm;// lahko je null!
		value = c;
		type = t;
		zacetek = m.rc.getRoundNum();
	}

	public void run() throws GameActionException {
//		System.out.println("Naloga " + type);
		switch (type) {
		case GRADNJA_REFINERIJE:
			gradnja_refinerije();
			break;
		case GRADNJA_TOVARNE_ZA_DRONE:
			gradnja_tovarne_za_drone();
			break;
		case GRADNJA_TOVARNE_ZA_LANDSCAPERJE:
			gradnja_tovarne_za_landscaperje();
			break;
		case NABIRANJE:
			nabiranje();
			break;
		case PREMIK_DO_JUHE:
			premik_do_juhe();
			break;
		case PREMIK_DO_POLJA:
			premik_do_polja();
			break;
		case PREMIKANJE_JUHE_V_BAZO:
			premikanje_juhe_v_bazo();
			break;
		case RAZISKOVANJE_JUHE:
			raziskovanje_juhe();
			break;
		case RAZISKOVANJE_MAPE:
			raziskovanje_mape();
			break;
		}
	}

	private void raziskovanje_mape() throws GameActionException {
		if (raziskovanje == null) {
			raziskovanje = Util.getRandomDirection();
		}
		if (m.rc.canMove(raziskovanje)) {
			m.rc.move(raziskovanje);
		} else {
			raziskovanje = null;
		}
	}

	private void raziskovanje_juhe() throws GameActionException {
		m.path_finder.moveTowards(this.mesto);
	}

	private void premikanje_juhe_v_bazo() throws GameActionException {
		if (m.tryDepositSoup()) {
			value = 0;
			return;
		}
		m.path_finder.moveTowards(mesto);

	}

	private void premik_do_juhe() throws GameActionException {
		if (!m.juhe.contains(this.mesto)) {
			value = 0;
			return;
		}
		m.path_finder.moveTowards(this.mesto);
	}

	private void premik_do_polja() throws GameActionException {
		if (!m.polja.contains(this.mesto)) {
			value = 0;
			return;
		}
		m.path_finder.moveTowards(this.mesto);
	}

	private void nabiranje() throws GameActionException {
		if (m.rc.canSenseLocation(mesto) && m.rc.senseSoup(mesto) == 0) {
			value = 0;
			return;
		}
		if (m.tryMine()) {
			value = 0;
			return;
		}
		m.path_finder.moveTowards(this.mesto);
	}

	private void gradnja_refinerije() throws GameActionException {
		if (rc.canSenseLocation(mesto) && rc.senseRobotAtLocation(mesto) != null
				&& rc.senseRobotAtLocation(mesto).type == RobotType.REFINERY) {
			value = 0;
			return;
		}
		value = m.izracunaj_vrenost_refinerije(mesto);
		if (m.rc.getLocation().distanceSquaredTo(mesto) <= 2) {
			Direction d = m.rc.getLocation().directionTo(mesto);
			if (m.rc.canBuildRobot(RobotType.REFINERY, d)) {
				m.rc.buildRobot(RobotType.REFINERY, d);
				m.b.send_location(m.b.LOC_REFINERIJA, mesto);
				value = 0;
				return;
			}
		} else {
			m.path_finder.moveTowards(this.mesto);
		}
	}

	private void gradnja_tovarne_za_drone() throws GameActionException {
		if (m.rc.getLocation().distanceSquaredTo(mesto) <= 2) {
			Direction d = m.rc.getLocation().directionTo(mesto);
			if (m.rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, d)) {
				m.rc.buildRobot(RobotType.FULFILLMENT_CENTER, d);
				// TODO broadcast?
				value = 0;
				return;
			}
		} else {
			m.path_finder.moveTowards(this.mesto);
		}
	}

	private void gradnja_tovarne_za_landscaperje() throws GameActionException {
		if (m.rc.getLocation().distanceSquaredTo(mesto) <= 2) {
			Direction d = m.rc.getLocation().directionTo(mesto);
			if (m.rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
				m.rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
				// TODO broadcast?
				value = 0;
				return;
			}
		} else {
			m.path_finder.moveTowards(this.mesto);
		}
	}
}

public class miner extends robot {
	public static final int MINER_COST = RobotType.MINER.cost;
	public static final int razmik_med_polji = 20;
	public static final int optimize = 10;

	naloga task;
	ArrayList<naloga> toBuild;
	final static int GRADNJA = 1000;
	final static int PREMIKANJE_JUHE_V_BAZO = 300;
	final static int NABIRANJE = 250;// Vidimo juho do katere lahko dokazano pridemo
	final static int PREMIK_DO_JUHE = 200;// Baje da se do juhe da priti. Pathfinding
	final static int PREMIK_DO_POLJA = 150;// Baje da se do polja da priti. Pathfinding
	final static int RAZISKOVANJE_JUHE = 100;// Poiscemo pot do slabe juhe
	final static int RAZISKOVANJE_MAPE = 50;

	minerPathFinder path_finder;
	vector_set_gl juhe;
	vector_set_gl slabe_juhe;// tiste za katere ne vemo kako priti do njih
	vector_set_gl polja;
	vector_set_gl slaba_polja;// ne vemo a se da priti do njih
	vector_set_gl refinerije;
	vector_set_gl net_guns;
	int w, h;// dimenzije mape

	MapLocation hq_location;
	/**
	 * 0- obicajno<br>
	 * 10- pomagamo hq<br>
	 * 20- hq je napaden<br>
	 * 30- nasprotnik uporablja drone
	 */
	int stanje;

	public miner(RobotController rc) {
		super(rc);
	}

	@Override
	public void init() throws GameActionException {
		path_finder = new minerPathFinder(rc);
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		for (RobotInfo r : rc.senseNearbyRobots(2, rc.getTeam())) {
			if (r.type == RobotType.HQ) {
				hq_location = r.location;
			}
		}
		juhe = new vector_set_gl();
		slabe_juhe = new vector_set_gl();
		polja = new vector_set_gl();
		slaba_polja = new vector_set_gl();
		refinerije = new vector_set_gl();
		refinerije.add(hq_location);
		stanje = 0;
		toBuild = new ArrayList<naloga>();
		while (Clock.getBytecodesLeft() > 800 || rc.getCooldownTurns() > 1) {
			if (!b.read_next_round()) {
				return;
			}
		}
	}

	@Override
	public void precompute() throws GameActionException {
//		System.out.println("\n" + rc.getRoundNum());
	}

	@Override
	public void runTurn() throws GameActionException {
//		System.out.println("Pred potezo " + Clock.getBytecodesLeft());
		if (!rc.isReady()) {
			return;
		}
		findBestTask();
		if (task != null) {
			task.run();
		}
	}

	public void postcompute() throws GameActionException {
		// drone test
		/*
		 * if (rc.getRoundNum() % 100 == 0) { b.send_location2(b.LOC2_DRONE,
		 * rc.getLocation(), Util.randomPoint(rc.getMapHeight(), rc.getMapWidth()),
		 * rc.getID()); }
		 */

		if (stanje == 10) {
			if (rc.getRoundNum() % 10 == 0) {
				b.send_packet(b.UNIT_ALIVE, new int[] { b.PRIVATE_KEY, b.UNIT_ALIVE, rc.getID(), 0, 0, 0, 0 });
			}
		}
		update_soup();
		while (Clock.getBytecodesLeft() > 500) {
			if (!b.read_next_round()) {
				break;
			}
		}
	}

	// Util
	private void update_soup() throws GameActionException {
//		System.out.println("Za "+juhe.size+" juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		if (Clock.getBytecodesLeft() < 1000) {
			System.out.println("tle");
			return;
		}
		// Dodamo juhe ki jih vidimo
		MapLocation[] q = rc.senseNearbySoup();
		int i = q.length;
		while (i-- > 0) {
			if (i % optimize != rc.getRoundNum() % optimize) {
				continue;
			}
			MapLocation m = q[i];
			if (Clock.getBytecodesLeft() < 1000) {
				System.out.println("tle dodajanje juh");
				return;
			}
			if (!juhe.contains(m) && !slabe_juhe.contains(m)) {
				if (Util.d_inf(rc.getLocation(), m) <= 5 && path_finder.exists_path(rc.getLocation(), m)) {
					juhe.add(m);
					check_for_field(m);
				} else {
					slabe_juhe.add(m);
					check_for_bad_field(m);
				}
			}
		}

//		System.out.println("Za odstranjevanje juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		for (i = 0; i < juhe.size; i++) {
			if (i % optimize != rc.getRoundNum() % optimize) {
				continue;
			}
			if (Clock.getBytecodesLeft() < 1000) {
				System.out.println("tle odstranjevanje praznih juh");
				return;
			}
			MapLocation m = juhe.get(i);
			if (m == null) {
				continue;
			}
//			rc.setIndicatorDot(m, 0, 255, 0);
			if (rc.canSenseLocation(m) && rc.senseSoup(m) == 0) {
				juhe.remove(m);
				i--;
			}
		}

//		System.out.println("Za odstranjevanje slabih juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		for (i = 0; i < slabe_juhe.size; i++) {

			MapLocation m = slabe_juhe.get(i);
			if (m == null) {
				continue;
			}
			if (Clock.getBytecodesLeft() < 1000) {
				System.out.println("tle odstanjevanje slabih praznih juh");
				return;
			}
//			rc.setIndicatorDot(m, 255, 0, 0);
			if (rc.canSenseLocation(m) && rc.senseSoup(m) == 0) {
				slabe_juhe.remove(m);
				i--;
			}
		}

//		System.out.println("Za premikanje juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		for (i = 0; i < slabe_juhe.size; i++) {
			if (i % optimize != rc.getRoundNum() % optimize) {
				continue;
			}

			MapLocation m = slabe_juhe.get(i);
			if (m == null) {
				continue;
			}
			if (Clock.getBytecodesLeft() < 1000) {
				System.out.println("tle premikanje slabih v dobre juhe");
				return;
			}
			if (Util.d_inf(rc.getLocation(), m) <= 5 && path_finder.exists_path(rc.getLocation(), m)) {
				juhe.add(m);// vedno ko dodamo juho, preverimo ce dodamo polje
				check_for_field(m);
				slabe_juhe.remove(m);// TO-DO odstrani polje povezano s to juho?
				i--;
			}
		}

		/**
		 * pogledamo ce imamo polje, da velja<br>
		 * 1. Vidimo vsaj razmik_med_polji stran od polje<br>
		 * 2. V tem obmocju ni nobene dosegljive juhe.
		 */
		for (i = 0; i < polja.size; i++) {
			MapLocation m = polja.get(i);
			if (m == null) {
				continue;
			}
			if (rc.getLocation().distanceSquaredTo(m) <= 2) {
				// Kako pogledati ce je okoli polja juha? Simple: Scan
				if (rc.senseNearbySoup(m, 20).length == 0) {
					polja.remove(m);
					b.send_location(b.LOC_SUROVINA_PRAZNO, m);
					i--;
				}
			}
		}
//		System.out.println("Za barvanje juh je ostalo " + Clock.getBytecodesLeft() + " casa");
		if (Clock.getBytecodesLeft() < 1000) {
			System.out.println("tle pred barvanjem");
			return;
		}
		for (i = 0; i < polja.size; i++) {
			MapLocation m = polja.get(i);
			if (m == null) {
				continue;
			}
			rc.setIndicatorDot(m, 0, 255, 0);
		}
		if (Clock.getBytecodesLeft() < 1000) {
			System.out.println("tle med barvanjem");
			return;
		}
		for (i = 0; i < slaba_polja.size; i++) {
			MapLocation m = slaba_polja.get(i);
			if (m == null) {
				continue;
			}
			rc.setIndicatorDot(m, 255, 0, 0);
		}

	}

	private void check_for_field(MapLocation m) throws GameActionException {
//		System.out.println("Preverjamo ce je " + m + "polje ");
//		System.out.println(juhe.size + " " + slabe_juhe.size);
		MapLocation closest = Util.closest(polja, m);
		if (closest != null && closest.distanceSquaredTo(m) < razmik_med_polji) {
			return;
		}
		if (rc.canSenseLocation(m)) {
			polja.add(m);
			b.send_location(b.LOC_SUROVINA, m);
		}
	}

	private void check_for_bad_field(MapLocation m) throws GameActionException {
		MapLocation closest = Util.closest(slaba_polja, m);
		if (closest != null && closest.distanceSquaredTo(m) < razmik_med_polji) {
			return;
		}
		if (rc.canSenseLocation(m)) {
			polja.add(m);
			b.send_location(b.LOC_SLABA_SUROVINA, m);
		}
	}

	public void findBestTask() throws GameActionException {
		int currentValue = (task == null ? 0 : task.value);
		if (currentValue < PREMIKANJE_JUHE_V_BAZO) {
			if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
				task = new naloga(this, Util.closest(refinerije, rc.getLocation()), PREMIKANJE_JUHE_V_BAZO,
						naloga.PREMIKANJE_JUHE_V_BAZO);
				currentValue = PREMIKANJE_JUHE_V_BAZO;
			}
		}
		if (currentValue < NABIRANJE && rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
			MapLocation ans = Util.closest(juhe, rc.getLocation());
			if (ans != null && path_finder.exists_path(rc.getLocation(), ans)) {
				task = new naloga(this, ans, NABIRANJE, naloga.NABIRANJE);
				currentValue = NABIRANJE;
			}
		}
		if (currentValue < PREMIK_DO_JUHE && rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
			MapLocation ans = Util.closest(juhe, rc.getLocation());
			if (ans != null) {
				task = new naloga(this, ans, PREMIK_DO_JUHE, naloga.PREMIK_DO_JUHE);
				currentValue = PREMIK_DO_JUHE;
			}
		}
		if (currentValue < PREMIK_DO_POLJA && rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
			MapLocation ans = Util.closest(polja, rc.getLocation());
			if (ans != null) {
				task = new naloga(this, ans, PREMIK_DO_POLJA, naloga.PREMIK_DO_POLJA);
				currentValue = PREMIK_DO_POLJA;
			}
		}
		if (currentValue < RAZISKOVANJE_JUHE) {
			MapLocation ans = Util.closest(slabe_juhe, rc.getLocation());
			if (ans != null) {
				task = new naloga(this, ans, RAZISKOVANJE_JUHE, naloga.RAZISKOVANJE_JUHE);
				currentValue = RAZISKOVANJE_JUHE;
			}
		}
		if (currentValue < RAZISKOVANJE_MAPE) {
			task = new naloga(this, null, RAZISKOVANJE_MAPE, naloga.RAZISKOVANJE_MAPE);
			currentValue = RAZISKOVANJE_MAPE;
		}
		if (stanje == 10&&task!=null&&task.value!=GRADNJA) {// gradnja?
//			System.out.println(toBuild.size());
			naloga closest = null;
			int dist = c.inf;
			for (naloga d : toBuild) {
				if (Util.d_inf(rc.getLocation(), d.mesto) < dist) {
					dist = Util.d_inf(rc.getLocation(), d.mesto);
					closest = d;
				}
			}
			if (closest != null) {
				task = closest;
				toBuild.remove(closest);
			}
		} else {// ce ne gradimo baze lahko gradimo refinerije
			int vrednost_refinerije = izracunaj_vrenost_refinerije(Util.closest(polja, rc.getLocation()));
//		System.out.println(vrednost_refinerije);
			if (vrednost_refinerije > currentValue) {
				MapLocation ans = Util.closest(polja, rc.getLocation());
				task = new naloga(this, ans, vrednost_refinerije, naloga.GRADNJA_REFINERIJE);
				currentValue = vrednost_refinerije;
			}
		}

	}

	public int izracunaj_vrenost_refinerije(MapLocation closest) throws GameActionException {
		if (closest == null) {
			return 0;
		}
		if (rc.getTeamSoup() < RobotType.REFINERY.cost) {
			return 0;
		}
		int currentRefineryDist = Util.d_inf(Util.closest(refinerije, closest), closest);
		if (currentRefineryDist < 5) {
			return 0;
		}
		int sumSoup = 0;
		for (MapLocation m : rc.senseNearbySoup(closest, 15)) {
			sumSoup += rc.senseSoup(m);
		}
		if (sumSoup < 300) {
			return 0;
		}
		int weightedSum = (int) Math.round(0.5 * rc.getTeamSoup() + 30 * currentRefineryDist + 0.2 * sumSoup);// Same //
																												// :)
		return weightedSum;
	}

	public boolean tryDepositSoup() throws GameActionException {
		for (Direction d : Util.dir) {
			if (rc.canDepositSoup(d) && rc.senseRobotAtLocation(rc.getLocation().add(d)).team == rc.getTeam()) {
				rc.depositSoup(d, rc.getSoupCarrying());
				return true;
			}
		}
		return false;
	}

	public boolean tryMine() throws GameActionException {
		for (Direction d : Util.dir) {
			if (rc.canMineSoup(d)) {
				rc.mineSoup(d);
				return true;
			}
		}
		return false;
	}

	// BLOCKCHAIN

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

	@Override
	public void bc_rafinerija(MapLocation pos) {
		System.out.println("Nova refinerija");
		if (!refinerije.contains(pos)) {
			refinerije.add(pos);
		}
	}

	@Override
	public void bc_ally_netgun(MapLocation pos) {
		if (!net_guns.contains(pos)) {
			net_guns.add(pos);
		}
	}

	@Override
	public void bc_build_tovarna_dronov(MapLocation pos) {
		if (stanje == 10) {
			toBuild.add(new naloga(this, pos, GRADNJA, naloga.GRADNJA_TOVARNE_ZA_DRONE));
		}
	}

	@Override
	public void bc_build_tovarna_landscaperjev(MapLocation pos) {
		if (stanje == 10) {
			toBuild.add(new naloga(this, pos, GRADNJA, naloga.GRADNJA_TOVARNE_ZA_LANDSCAPERJE));
		}
	}

	@Override
	public void bc_miner_to_help(int[] message) {
		if (rc.getID() == message[2]) {
			stanje = 10;
		}
	}

}
