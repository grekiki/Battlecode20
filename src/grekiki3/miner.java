package grekiki3;

import java.util.ArrayList;
import java.util.HashSet;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

class MapCell{
	int soupCount=0;
	int pollutionLevel=0;
	int elevation=0;
	boolean flooded=false;
	int lastTurnSeen=-1;
	RobotInfo robot=null;

	MapCell(int soup,int pollution,int elevation,boolean flooded,int lastSeen,RobotInfo robot){
		soupCount=soup;
		pollutionLevel=pollution;
		this.elevation=elevation;
		this.flooded=flooded;
		lastTurnSeen=lastSeen;
		this.robot=robot;
	}
}

class minerPathFinder{
	private static final int NO_WALL=0;  // Ne sledi zidu.
	private static final int LEFT_WALL=1;  // Zid je na levi.
	private static final int RIGHT_WALL=2;
	private static final int LOOKAHEAD_STEPS=5;

	private RobotController rc;

	private MapLocation goal;
	private MapLocation closest;  // Uporablja se pri bug navigation.
	private Direction bug_wall_dir;  // V kateri smeri je zid, ki mu sledimo?
	private int bug_wall_tangent=NO_WALL;  // Na kateri strani je zid, ki mu sledimo?
	private MapLocation tangent_shortcut;  // Pomozna bliznjica.

	minerPathFinder(RobotController rc){
		this.rc=rc;
	}

	private boolean can_move(MapLocation from,Direction to){
		// Ta metoda ignorira cooldown ...

		MapLocation p=from.add(to);
		try{
			if(!rc.onTheMap(p)||rc.senseFlooding(p))
				return false;
			if(Math.abs(rc.senseElevation(from)-rc.senseElevation(p))>3)
				return false;
			if(rc.senseRobotAtLocation(p)!=null)
				return false;
		}catch(GameActionException e){
			return false;
		}
		return true;
	}

	private Direction fuzzy(MapLocation dest){
		MapLocation cur=rc.getLocation();
		Direction straight=cur.directionTo(dest);
		if(rc.canMove(straight))
			return straight;
		Direction left=straight.rotateLeft();
		if(rc.canMove(left))
			return left;
		Direction right=straight.rotateRight();
		if(rc.canMove(right))
			return right;
		left=left.rotateLeft();
		if(rc.canMove(left))
			return left;
		right=right.rotateRight();
		if(rc.canMove(right))
			return right;
		return null;
	}

	private Direction fuzzy_step(MapLocation cur,MapLocation dest){
		Direction straight=cur.directionTo(dest);
		if(can_move(cur,straight))
			return straight;
		Direction left=straight.rotateLeft();
		if(can_move(cur,left))
			return left;
		Direction right=straight.rotateRight();
		if(can_move(cur,right))
			return right;
		left=left.rotateLeft();
		if(can_move(cur,left))
			return left;
		right=right.rotateRight();
		if(can_move(cur,right))
			return right;
		return null;
	}
	private Direction fuzzy_step_short(MapLocation cur,MapLocation dest){
		Direction straight=cur.directionTo(dest);
		if(can_move(cur,straight))
			return straight;
		Direction left=straight.rotateLeft();
		if(can_move(cur,left))
			return left;
		Direction right=straight.rotateRight();
		if(can_move(cur,right))
			return right;
		left=left.rotateLeft();
		return null;
	}
	private Direction bug_step(MapLocation cur,MapLocation dest,int wall){
		Direction dir=fuzzy_step(cur,dest);
		if(dir!=null&&cur.add(dir).distanceSquaredTo(dest)<closest.distanceSquaredTo(dest)){
			bug_wall_dir=null;
			return dir;
		}

		// Ne moremo blizje, zato se drzimo zidu.
		// Drzimo se lahko leve ali desne strani: parameter 'wall'.
		if(bug_wall_dir==null)
			bug_wall_dir=cur.directionTo(dest);

		if(wall==LEFT_WALL){
			// V smeri urinega kazalca
			Direction right=bug_wall_dir;
			for(int i=0;i<8;++i){
				if(can_move(cur,right)){
					MapLocation wall_loc=cur.add(right.rotateLeft());
					bug_wall_dir=cur.add(right).directionTo(wall_loc);
					return right;
				}
				right=right.rotateRight();
			}
		}else{
			// Nasprotna smer urinega kazalca
			Direction left=bug_wall_dir;
			for(int i=0;i<8;++i){
				if(can_move(cur,left)){
					MapLocation wall_loc=cur.add(left.rotateRight());
					bug_wall_dir=cur.add(left).directionTo(wall_loc);
					return left;
				}
				left=left.rotateLeft();
			}
		}

		// To se lahko zgodi samo, ce je obkoljen ...
		return null;
	}

	private Object[] bug_step_simulate(MapLocation cur,MapLocation dest,int wall,int steps){
		// Vrne [0]: direction po prvem koraku
		// 	    [1]: wall dir po prvem koraku
		//      [2]: wall dir po zadnjem koraku
		//      [3]: koncna lokacija
		Object[] result=new Object[4];

		MapLocation prev_closest=closest;
		Direction prev_bug_wall_dir=bug_wall_dir;

		MapLocation end=cur;
		for(int i=0;i<steps;++i){
			Direction dir=bug_step(end,dest,wall);
			if(i==0){
				result[0]=dir;
				result[1]=bug_wall_dir;
			}
			if(dir==null)
				break;

			end=end.add(dir);
			if(end.distanceSquaredTo(dest)<closest.distanceSquaredTo(dest)){
				closest=end;
			}
			rc.setIndicatorDot(end,(255/steps)*i,wall*100,255);
		}

		result[2]=bug_wall_dir;
		result[3]=end;

		bug_wall_dir=prev_bug_wall_dir;
		closest=prev_closest;

		return result;
	}

	private boolean exists_path(MapLocation cur,MapLocation dest){
		Direction dir=cur.directionTo(dest);
		while(!cur.equals(dest)){
			if(!can_move(cur,dir))
				return false;
			cur=cur.add(dir);
		}
		return true;
	}

	public boolean exists_path2(MapLocation cur,MapLocation dest){
		Direction dir=fuzzy_step_short(cur,dest);
		while(!cur.equals(dest)){
			if(dir==null||!can_move(cur,dir))
				return false;
			cur=cur.add(dir);
		}
		return true;
	}
	private boolean exists_fuzzy_path(MapLocation cur,MapLocation dest,int max_steps){
		Direction dir=fuzzy_step(cur,dest);
		for(int steps=0;dir!=null&&!cur.equals(dest)&&steps<max_steps;++steps){
			if(!can_move(cur,dir))
				return false;
			cur=cur.add(dir);
			dir=fuzzy_step(cur,dest);
		}
		return cur.equals(dest);
	}

	private Direction tangent_bug(MapLocation dest){
		// Odlocimo se med levo in desno stranjo in potem
		// nadaljujemo po izbrani poti.
		// Ce najdemo bliznjico, gremo do nje po najkrajsi poti
		// in potem nadaljujemo pot.

		MapLocation cur=rc.getLocation();
		if(cur.equals(tangent_shortcut)){
			tangent_shortcut=null;
		}
		if(tangent_shortcut!=null){
			// Naj bi obstajala fuzzy pot do tam ...?
			Direction dir=fuzzy(tangent_shortcut);
			if(can_move(cur,dir))
				return dir;
			// Zgubili smo se ali pa je ovira ...
			tangent_shortcut=null;
			bug_wall_tangent=NO_WALL;
			bug_wall_dir=null;
		}

		// Stran zidu je ze izbrana
		// Simularmo pot z izbranim zidom
		if(bug_wall_tangent!=NO_WALL){
			// bug_step(cur, dest, bug_wall_tangent);
			Object[] simulation=bug_step_simulate(cur,dest,bug_wall_tangent,LOOKAHEAD_STEPS);
			MapLocation shortcut=(MapLocation)simulation[3];
			if(exists_fuzzy_path(cur,shortcut,LOOKAHEAD_STEPS-1)){
				tangent_shortcut=shortcut;
				bug_wall_dir=(Direction)simulation[2];
				return fuzzy(tangent_shortcut);
			}
			bug_wall_dir=(Direction)simulation[1];
			return (Direction)simulation[0];
		}

		// Odlocimo se med levo in desno stranjo
		Object[] left_simulation=bug_step_simulate(cur,dest,LEFT_WALL,LOOKAHEAD_STEPS);
		Object[] right_simulation=bug_step_simulate(cur,dest,RIGHT_WALL,LOOKAHEAD_STEPS);
		MapLocation left_pos=(MapLocation)left_simulation[3];
		MapLocation right_pos=(MapLocation)right_simulation[3];

		int d1=right_pos.distanceSquaredTo(dest);
		int d2=left_pos.distanceSquaredTo(dest);
		if(d1<=d2){
			bug_wall_tangent=RIGHT_WALL;
			if(exists_fuzzy_path(cur,right_pos,LOOKAHEAD_STEPS-1)){
				tangent_shortcut=right_pos;
				bug_wall_dir=(Direction)right_simulation[2];
				return fuzzy(tangent_shortcut);
			}
			bug_wall_dir=(Direction)right_simulation[1];
			return (Direction)right_simulation[0];
		}else{
			bug_wall_tangent=LEFT_WALL;
			if(exists_fuzzy_path(cur,left_pos,LOOKAHEAD_STEPS-1)){
				tangent_shortcut=left_pos;
				bug_wall_dir=(Direction)left_simulation[2];
				return fuzzy(tangent_shortcut);
			}
			bug_wall_dir=(Direction)left_simulation[1];
			return (Direction)left_simulation[0];
		}
	}

	public Direction get_move_direction(MapLocation dest){
		MapLocation cur=rc.getLocation();
		if(cur.isAdjacentTo(dest))
			return null;

		if(!dest.equals(goal)){
			goal=dest;
			closest=cur;
			bug_wall_dir=null;
		}else{
			if(cur.distanceSquaredTo(dest)<closest.distanceSquaredTo(dest)){
				closest=cur;
			}
		}
		// fuzzy(goal);
		// tangent_bug(dest);
		// Direction dir = bug_step(cur, dest, RIGHT_WALL);

		if(tangent_shortcut!=null)
			rc.setIndicatorDot(tangent_shortcut,255,0,0);

		/*
		 * MapLocation tmp = bug_step_simulate(cur, dest, LEFT_WALL, LOOKAHEAD_STEPS); if (tmp != null) rc.setIndicatorDot(tmp, 255, 0, 255);
		 */

		Direction dir=tangent_bug(dest);
		return dir;
	}

	public boolean moveTowards(MapLocation dest) throws GameActionException{
		Direction dir=get_move_direction(dest);

		if(dir!=null){
			rc.move(dir);
			return true;
		}
		return false;
	}

}

public class miner extends robot{
	public static final int MINER_COST=RobotType.MINER.cost;
	public static final int razmik_med_polji=30;

	MapLocation goal;// kam hoce miner priti
	//TO-DO dodaj sistem prioritet. Recimo gradnja-1000, nabiranje-200, premikanje juhe v bazo-300, raziskovanje-100

	minerPathFinder path_finder;
	HashSet<MapLocation> juhe;
	HashSet<MapLocation> slabe_juhe;//tiste za katere ne vemo kako priti do njih
	ArrayList<MapLocation> polja;
	ArrayList<MapLocation> slaba_polja;//ne vemo a se da priti do njih
	int w,h;// dimenzije mape

	MapLocation hq_location;

	public miner(RobotController rc){
		super(rc);
	}

	@Override public void init() throws GameActionException{
		path_finder=new minerPathFinder(rc);
		w=rc.getMapWidth();
		h=rc.getMapHeight();

		for(RobotInfo r:rc.senseNearbyRobots(2,rc.getTeam())){
			if(r.type==RobotType.HQ){
				hq_location=r.location;
			}
		}
		juhe=new HashSet<MapLocation>();
		slabe_juhe=new HashSet<MapLocation>();
		polja=new ArrayList<MapLocation>();
		slaba_polja=new ArrayList<MapLocation>();
	}

	@Override public void precompute() throws GameActionException{
		b.checkQueue();
		for(MapLocation m:rc.senseNearbySoup()){
			if(!juhe.contains(m)&&!slabe_juhe.contains(m)){
				if(path_finder.exists_path2(rc.getLocation(),m)){
					juhe.add(m);
				}else{
					slabe_juhe.add(m);
				}
			}
		}
	}

	@Override public void runTurn() throws GameActionException{
		if(!rc.isReady()){
			return;
		}
		// Ce smo polni gremo do baze
		if(rc.getSoupCarrying()==RobotType.MINER.soupLimit){
			goal=hq_location;
			if(tryDepositSoup()){
				goal=null;
				return;
			}else{
				if(path_finder.moveTowards(goal)){
					return;
				}
			}
		}

		if(tryMine()){
			goal=null;//nasli smo surovino, zato jo izbrisemo da se ne bi kaj zaciklalo ce se sprazne
			return;
		}

		//ce nimamo dela ga najdemo
		if(goal==null){
			goal=findWork();
		}
		System.out.println(goal);
		path_finder.moveTowards(goal);

	}

	public void postcompute() throws GameActionException{
		while(Clock.getBytecodesLeft()>800){
			if(!b.read_next_round()){
				return;
			}
		}
	}

	//Util
	private MapLocation findWork() throws GameActionException{
		//Ce je najblizja juha blizu jo vzamemo
		MapLocation ans=Util.closest(juhe,rc.getLocation());
		if(ans!=null&&ans.distanceSquaredTo(rc.getLocation())<40){//neka konstanta!
			return ans;
		}
		//drugace pogledamo ce je kje kaksno polje
		ans=Util.closest(polja,rc.getLocation());
		//Nakljucno raziskovanje ker ni cilja
		if(ans!=null){
			return ans;
		}
		//drugace pa gremo do poljubne najblizje surovine
		ans=Util.closest(juhe,rc.getLocation());
		if(ans!=null){//neka konstanta!
			return ans;
		}
		//ce smo tukaj ni niti surovin, niti polj do katerih verjetno obstaja dostop. 
		//morda bi bilo dobro poklicati kaksnega drona ali landscaperja

		//poiscemo polje, morda v resnici obstaja kaksna pot
		ans=Util.closest(slaba_polja,rc.getLocation());
		if(ans!=null){
			return ans;
		}
		//ali pa kaksna slaba surovina?
		ans=Util.closest(slabe_juhe,rc.getLocation());
		if(ans!=null){
			return ans;
		}
		return Util.randomPoint(h,w);
	}
	public boolean tryDepositSoup() throws GameActionException{
		for(Direction d:Util.dir){
			if(rc.canDepositSoup(d)&&rc.senseRobotAtLocation(rc.getLocation().add(d)).team==rc.getTeam()){
				rc.depositSoup(d,rc.getSoupCarrying());
				return true;
			}
		}
		return false;
	}

	public boolean tryMine() throws GameActionException{
		for(Direction d:Util.dir){
			if(rc.canMineSoup(d)){
				rc.mineSoup(d);
				return true;
			}
		}
		return false;
	}

	@Override public void bc_polje_found(MapLocation pos){
		System.out.println("BC SUROVINA: "+pos);
		polja.add(pos);
	}
	@Override public void bc_polje_empty(MapLocation pos){
		System.out.println("BC SUROVINA: "+pos);
		if(polja.contains(pos)){
			polja.remove(pos);
		}
	}

	@Override public void bc_drone(MapLocation from,MapLocation to){
		System.out.println("BC DRONE: "+from+" "+to);
	}

}
