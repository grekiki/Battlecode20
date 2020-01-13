package grekiki2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import battlecode.common.*;

class miner extends robot{
	RobotController rc;
	int phase=0;
	MapLocation hq;

	ArrayList<MapLocation> polja;
	HashSet<MapLocation> surovine;
	ArrayList<MapLocation> refinery;
	ArrayList<MapLocation> turret;
	ArrayList<MapLocation> dronespawn;
	blockchain b;

	// phase 0
	boolean done;// ali smo konec z premikanjem v tej smeri
	Direction init;
	// phase 1

	// phase 2
	boolean baseWorker=false;
	final int polmer_surovine=20;

	miner(RobotController rc){
		this.rc=rc;
	}

	public void readBlockchain() throws Exception{
		for(int i=1;i<rc.getRoundNum();i++){
			readBlockchain(i);
		}
	}

	public void readBlockchain(int round) throws GameActionException{
		Transaction[] t=rc.getBlock(round);
		for(Transaction tt:t){
			int[] msg=tt.getMessage();
			if(msg.length==7){
				if(msg[0]==123456789){
					if(msg[1]==1){// Sprememba faze
						int currentPhase=msg[2];
						phase=currentPhase;
						int id=msg[3];
						if(id==rc.getID()){
							baseWorker=true;
						}
						System.out.println(phase+". FAZA");
						if(phase==3){
							refinery.remove(hq);
						}
					}else if(msg[1]==2){
						MapLocation m=new MapLocation(msg[2],msg[3]);
						if(!polja.contains(m)){
							polja.add(m);
						}
					}else if(msg[1]==3){
						MapLocation m=new MapLocation(msg[2],msg[3]);
						if(polja.contains(m)){
							polja.remove(m);
						}
					}else if(msg[1]==4){
						MapLocation m=new MapLocation(msg[2],msg[3]);
						if(!refinery.contains(m)){
							refinery.add(m);
						}
					}else if(msg[1]==5){
						MapLocation m=new MapLocation(msg[2],msg[3]);
						if(!turret.contains(m)){
							turret.add(m);
						}
					}
				}
			}
		}
	}

	@Override public void init() throws Exception{
		surovine=new HashSet<MapLocation>();
		for(RobotInfo r:rc.senseNearbyRobots(2,rc.getTeam())){
			if(r.type==RobotType.HQ){
				hq=r.location;
			}
		}
		init=rc.getLocation().directionTo(hq).opposite();// Miner gre stran od HQ
		done=false;
		polja=new ArrayList<MapLocation>();
		b=new blockchain(rc);
		refinery=new ArrayList<MapLocation>();
		refinery.add(hq);
		turret=new ArrayList<MapLocation>();
		dronespawn=new ArrayList<MapLocation>();
		readBlockchain();
	}

	@Override public void precompute() throws Exception{
		if(rc.getRoundNum()>1){
			readBlockchain(rc.getRoundNum()-1);
		}
	}

	@Override public void runTurn() throws Exception{
		if(!rc.isReady()){
			return;
		}
		if(baseWorker){
			if(runBaseBuild()){
				return;
			}
		}
		if(!baseWorker&&Util.d_inf(rc.getLocation(),hq)<=2&&phase==3){
			int t=1/0;
			System.out.println(t);
		}
		if(phase==0||phase==1||phase==2||phase==3){//TO-DO optimiziraj fazo 3. 
			//Obdelamo potrebo po refineriji
			if(surovine.size()>0&&rc.getTeamSoup()>=RobotType.REFINERY.cost+200||(refinery.size()<2&&rc.getTeamSoup()>=RobotType.REFINERY.cost)){
				MapLocation closest=findClosest(surovine);
				if(closest!=null&&rc.getLocation().distanceSquaredTo(closest)<10){
					//Preverimo èe je refinerija potrebna
					MapLocation ref=closest;
					if(Util.d_inf(hq,ref)>=6){//Ne na zidu... Da ne motimo baze
						int dist=20;
						for(MapLocation re:refinery){
							dist=Math.min(dist,re.distanceSquaredTo(ref));
						}
						if(dist==20){//ni refinerije bližje kot 20
							//Potrebujemo refinerijo.
							//Poskusimo èe smo dovolj blizu
							if(ref.distanceSquaredTo(rc.getLocation())<=2){
								int sum=0;
								for(int i=0;i<=4;i++) {
									for(MapLocation mm:pc.range[i]) {
										MapLocation m=new MapLocation(ref.x+mm.x,ref.y+mm.y);
										if(rc.canSenseLocation(m)) {
											sum+=rc.senseSoup(m);
										}
									}
								}
//								System.out.println(sum);
								if(sum>200) {
									Direction d=rc.getLocation().directionTo(ref);
									if(rc.getTeamSoup()>=RobotType.REFINERY.cost){
										if(rc.canBuildRobot(RobotType.REFINERY,d)){
											rc.buildRobot(RobotType.REFINERY,d);
											int[] msg={123456789,4,rc.getLocation().x+d.dx,rc.getLocation().y+d.dy,0,0,0};
											b.sendMsg(new paket(msg,1));
											return;
										}
									}	
								}
							}else{
								Direction d=Util.tryMove(rc,rc.getLocation().directionTo(ref));
								if(d!=null&&rc.canMove(d)){
									rc.move(d);
									return;
								}
							}
						}
					}
				}
			}

			//Najprej poskusimo èe smo polni
			if(rc.getSoupCarrying()==100){
				for(Direction d:Util.dir){
					if(rc.canDepositSoup(d)){
						rc.depositSoup(d,rc.getSoupCarrying());
						return;
					}
				}
				if(moveClosest(refinery)){
					return;
				}else{
//					System.out.println("Ne moremo priti do najblizje refinerije!");
				}
			}
			a:if(phase==2&&rc.getTeamSoup()>=RobotType.NET_GUN.cost+300){//Rafinerije potrebujejo drone za obrambo. Tovarna vsaj 3 stran po d_inf metriki
				MapLocation ref=findClosest(refinery);
				int optRange=ref.equals(hq)?4:2;
				int dist=1000000000;
				for(RobotInfo r:rc.senseNearbyRobots(-1,rc.getTeam())){
					if(r.team==rc.getTeam()&&r.type==RobotType.NET_GUN){
						if(rc.getLocation().distanceSquaredTo(r.location)<dist){
							dist=rc.getLocation().distanceSquaredTo(r.location);
						}
					}
				}
				if(dist<5*optRange){
					break a;
				}
				if(optRange-1<=Util.d_inf(ref,rc.getLocation())&&Util.d_inf(ref,rc.getLocation())<=optRange+1){//Morda se da razdaljo spraviti na 3
					for(Direction d:Util.dir){
						MapLocation op=rc.getLocation().add(d);
						if(Util.d_inf(op,ref)==optRange){
							if(rc.canBuildRobot(RobotType.NET_GUN,d)){
								rc.buildRobot(RobotType.NET_GUN,d);
								int[] msg={123456789,5,rc.getLocation().x+d.dx,rc.getLocation().y+d.dy,0,0,0};
								b.sendMsg(new paket(msg,1));
								return;
							}
						}
					}
				}
			}
			//Poskusimo kopati
			if(rc.getSoupCarrying()<RobotType.MINER.soupLimit){
				for(Direction d:Direction.allDirections()){
					if(rc.canMineSoup(d)){
						rc.mineSoup(d);
						return;
					}
				}
			}
			//Gremo do najbližje surovine
			if(surovine.size()>0){
				if(moveClosest(surovine)){
					return;
				}
			}
			//Gremo do najbližjega polja
			if(polja.size()>0){
				if(moveClosest(polja)){
					return;
				}
			}
			//Èe ni niè dela pametno raziskujemo
			Direction d=Util.tryMoveLite(rc,init);
			if(d==null){
				init=Util.getRandomDirection();
			}else{
				rc.move(d);
				return;
			}
		} // phase>1
	}

	int fazaBaze=-1;
	public boolean runBaseBuild() throws GameActionException{
		if(fazaBaze==-1){
			fazaBaze=0;
			int[] msg={123456789,6,hq.x,hq.y,0,0,0};
			b.sendMsg(new paket(msg,1));
		}
		if(fazaBaze==0){
			MapLocation f1=hq.add(Direction.NORTHWEST);
			if(rc.canSenseLocation(f1)&&rc.senseRobotAtLocation(f1)!=null&&rc.senseRobotAtLocation(f1).type==RobotType.FULFILLMENT_CENTER&&rc.getTeamSoup()>=RobotType.DESIGN_SCHOOL.cost+300){
				MapLocation f2=hq.add(Direction.SOUTHEAST);
				if(rc.getLocation().distanceSquaredTo(f2)<=2&&!f2.equals(rc.getLocation())){
					if(rc.canBuildRobot(RobotType.DESIGN_SCHOOL,rc.getLocation().directionTo(f2))){
						rc.buildRobot(RobotType.DESIGN_SCHOOL,rc.getLocation().directionTo(f2));
						fazaBaze=-2;
						return true;
					}
				}else if(f2.equals(rc.getLocation())){
					if(rc.canMove(Direction.NORTH)){
						rc.move(Direction.NORTH);
						return true;
					}else if(rc.canMove(Direction.WEST)){
						rc.move(Direction.WEST);
						return true;
					}
				}else{
					Direction d=Util.tryMove(rc,rc.getLocation().directionTo(f2));
					if(d!=null){
						rc.move(d);
						return true;
					}
				}
			}else if(rc.getTeamSoup()>=RobotType.FULFILLMENT_CENTER.cost){
				if(rc.getLocation().distanceSquaredTo(f1)<=2&&!f1.equals(rc.getLocation())){
					if(rc.getTeamSoup()>=RobotType.FULFILLMENT_CENTER.cost&&rc.canBuildRobot(RobotType.FULFILLMENT_CENTER,rc.getLocation().directionTo(f1))){
						rc.buildRobot(RobotType.FULFILLMENT_CENTER,rc.getLocation().directionTo(f1));
						return true;
					}
				}else if(f1.equals(rc.getLocation())){
					if(rc.canMove(Direction.SOUTH)){
						rc.move(Direction.SOUTH);
						return true;
					}else if(rc.canMove(Direction.EAST)){
						rc.move(Direction.EAST);
						return true;
					}
				}else{
					Direction d=Util.tryMove(rc,rc.getLocation().directionTo(f1));
					if(d!=null){
						rc.move(d);
						return true;
					}
				}
			}
		}
		if(fazaBaze==-2&&phase==3){
			fazaBaze=1;
		}
		if(fazaBaze==1){//NetCatcher
			int[] msg={123456789,7,rc.getLocation().x,rc.getLocation().y,hq.x+1,hq.y,0};
			b.sendMsg(new paket(msg,1));
			fazaBaze=2;
		}
		if(fazaBaze==2){
			if(rc.getLocation().x==hq.x+1&&rc.getLocation().y==hq.y){
				if(rc.canBuildRobot(RobotType.NET_GUN,Direction.NORTH)){
					rc.buildRobot(RobotType.NET_GUN,Direction.NORTH);
					return true;
				}
				if(rc.canBuildRobot(RobotType.NET_GUN,Direction.NORTHWEST)){
					rc.buildRobot(RobotType.NET_GUN,Direction.NORTHWEST);
					return true;
				}
				if(rc.senseRobotAtLocation(rc.getLocation().add(Direction.NORTH))!=null&&rc.senseRobotAtLocation(rc.getLocation().add(Direction.NORTH)).type==RobotType.NET_GUN&&rc.senseRobotAtLocation(rc.getLocation().add(Direction.NORTHWEST))!=null&&rc.senseRobotAtLocation(rc.getLocation().add(Direction.NORTHWEST)).type==RobotType.NET_GUN){
					fazaBaze=3;
				}else{
					return true;
				}
			}else{
				return true;
			}
		}
		if(fazaBaze==3){
			int[] msg={123456789,7,rc.getLocation().x,rc.getLocation().y,hq.x-1,hq.y,0};
			b.sendMsg(new paket(msg,1));
			fazaBaze=4;
		}
		if(fazaBaze==4){
			if(rc.getLocation().x==hq.x-1&&rc.getLocation().y==hq.y){
				if(rc.canBuildRobot(RobotType.NET_GUN,Direction.SOUTH)){
					rc.buildRobot(RobotType.NET_GUN,Direction.SOUTH);
					return true;
				}
				if(rc.canBuildRobot(RobotType.NET_GUN,Direction.SOUTHEAST)){
					rc.buildRobot(RobotType.NET_GUN,Direction.SOUTHEAST);
					return true;
				}
				if(rc.senseRobotAtLocation(rc.getLocation().add(Direction.SOUTH))!=null&&rc.senseRobotAtLocation(rc.getLocation().add(Direction.SOUTH)).type==RobotType.NET_GUN&&rc.senseRobotAtLocation(rc.getLocation().add(Direction.SOUTHEAST))!=null&&rc.senseRobotAtLocation(rc.getLocation().add(Direction.SOUTHEAST)).type==RobotType.NET_GUN){
					fazaBaze=5;
				}else{
					return true;
				}
			}else{
				return true;
			}
		}
		if(fazaBaze==5){
			int[] msg={123456789,7,rc.getLocation().x,rc.getLocation().y,rc.getLocation().x-2,rc.getLocation().y,0};
			b.sendMsg(new paket(msg,1));
			fazaBaze=6;
			return true;
		}else if(fazaBaze==6){
			return false;
		}
		return false;
	}

	public MapLocation findClosest(Iterable<? extends MapLocation> somethings) throws GameActionException{
		MapLocation best=null;
		int dist=64*64;
		for(MapLocation m:somethings){
			int op=rc.getLocation().distanceSquaredTo(m);
			if(op<dist){
				dist=op;
				best=m;
			}
		}
		return best;
	}
	public boolean moveClosest(Iterable<? extends MapLocation> somethings) throws GameActionException{
		MapLocation best=null;
		int dist=64*64;
		for(MapLocation m:somethings){
			int op=rc.getLocation().distanceSquaredTo(m);
			if(op<dist){
				dist=op;
				best=m;
			}
		}
		if(best==null){
			return false;
		}
		Direction d=Util.tryMove(rc,rc.getLocation().directionTo(best));
		if(d!=null){
			rc.move(d);
			return true;
		}else{
			return false;
		}
	}

	@Override public void postcompute() throws Exception{
		int range=rc.getCurrentSensorRadiusSquared();
		if(Clock.getBytecodesLeft()<1000){
			return;
		}
		discardMissing();
		checkForEmptyField();
		findResources(range);
	}
	public void checkForEmptyField() throws GameActionException{
		int range=rc.getCurrentSensorRadiusSquared();
		for(MapLocation polje:polja){
			if(range>=rc.getLocation().distanceSquaredTo(polje)+polmer_surovine){
				boolean ok=false;
				for(int i=0;i<=polmer_surovine;i++){
//					if(Clock.getBytecodesLeft()<1000){
//						return;
//					}
					MapLocation[] scan=pc.range[i];
					for(MapLocation mm:scan){
						MapLocation m=new MapLocation(polje.x+mm.x,polje.y+mm.y);
						if(rc.canSenseLocation(m)&&!rc.senseFlooding(m)&&rc.senseSoup(m)>0&&rc.senseElevation(m)<=3+rc.senseElevation(rc.getLocation())){
							ok=true;
							i=1000;
							break;
						}
					}
				}
				if(!ok){
					int[] msg={123456789,3,polje.x,polje.y,0,0,0};
					b.sendMsg(new paket(msg,1));
				}
			}
		}
	}

	public void findResources(int range) throws GameActionException{
		for(int i=0;i<range;i++){
			MapLocation[] scan=pc.range[i];
			if(Clock.getBytecodesLeft()<1000){
				return;
			}
			for(MapLocation mm:scan){
				MapLocation m=new MapLocation(rc.getLocation().x+mm.x,rc.getLocation().y+mm.y);
				if(rc.canSenseLocation(m)&&!rc.senseFlooding(m)&&rc.senseSoup(m)>0){
					if(Clock.getBytecodesLeft()<1000){
						return;
					}
					surovine.add(m);
					boolean used=false;
					for(MapLocation center:polja){
						if(center.distanceSquaredTo(m)<=20){
							used=true;
							break;
						}
					}
					if(!used){
						// Dodamo polje v blockchain
						MapLocation interpolacija_centra=m.add(rc.getLocation().directionTo(m));
						polja.add(interpolacija_centra);
						int[] msg={123456789,2,interpolacija_centra.x,interpolacija_centra.y,0,0,0};
						b.sendMsg(new paket(msg,1));
					}
				}
			}
		}
	}

	public void discardMissing() throws GameActionException{
		// Tako se brise iz seta. Vir
		// :https://stackoverflow.com/questions/1110404/remove-elements-from-a-hashset-while-iterating
		Iterator<MapLocation> iterator=surovine.iterator();
		while(iterator.hasNext()){
			if(Clock.getBytecodesLeft()<1000){
				return;
			}
			MapLocation m=iterator.next();
			if(rc.canSenseLocation(m)&&rc.senseSoup(m)==0){
				iterator.remove();
			}
		}
	}
}

class landscaper extends robot{
	RobotController rc;
	MapLocation hq;
	int phase=0;
	public static MapLocation[] place={new MapLocation(0,-2),new MapLocation(0,2),new MapLocation(-2,0),new MapLocation(2,0),new MapLocation(2,-2),new MapLocation(2,2),new MapLocation(-2,2),new MapLocation(-2,-2)};

	landscaper(RobotController rc){
		this.rc=rc;
	}

	@Override public void init(){
		for(RobotInfo r:rc.senseNearbyRobots(10,rc.getTeam())){
			if(r.type==RobotType.HQ){
				hq=r.location;
			}
		}

	}
	public Direction direction(int p){
		if(place[p].y==2){
			return Direction.NORTH;
		}else if(place[p].y==-2){
			return Direction.SOUTH;
		}else if(place[p].x==-2){
			return Direction.WEST;
		}else if(place[p].x==2){
			return Direction.EAST;
		}else{
			System.out.println("NAPAKA!!!");
			return null;
		}
	}
	@Override public void precompute() throws GameActionException{
		boolean ok=true;
		for(MapLocation m:landscaper.place){
			MapLocation check=new MapLocation(hq.x+m.x,hq.y+m.y);
			if(!rc.canSenseLocation(check)||rc.senseRobotAtLocation(check)==null||(rc.senseRobotAtLocation(check)!=null&&rc.senseRobotAtLocation(check).type!=RobotType.LANDSCAPER)){
				ok=false;
				break;
			}
		}
		if(ok){
			phase=3;
		}
	}
	int p=0;
	boolean positioned=false;
	@Override public void runTurn() throws GameActionException{
		if(!rc.isReady()){
			return;
		}
		if(rc.getLocation().equals(new MapLocation(hq.x+place[p].x,hq.y+place[p].y))){
			positioned=true;
		}
		if(phase==3){
			positioned=true;
		}
		if(positioned){
			int min=10000000;
			Direction best=null;
			for(Direction d:Direction.allDirections()){
				if(Util.d_inf(hq,rc.getLocation().add(d))==2){
					if(rc.canDepositDirt(d)){
						if(rc.senseElevation(rc.getLocation().add(d))<min){
							min=rc.senseElevation(rc.getLocation().add(d));
							best=d;
						}
					}
				}
			}
			if(best!=null){
				rc.depositDirt(best);
				return;
			}
			for(Direction d:Util.dir){
				if(Util.d_inf(hq,rc.getLocation().add(d))==3){
					if(rc.canDigDirt(d)){
						rc.digDirt(d);
						return;
					}
				}
			}

		}else{
			for(int i=0;i<8;i++){
				MapLocation check=new MapLocation(hq.x+place[i].x,hq.y+place[i].y);
				if(rc.canSenseLocation(check)&&(rc.senseRobotAtLocation(check)==null||rc.senseRobotAtLocation(check).type!=RobotType.LANDSCAPER)){
					p=i;
				}
			}
			MapLocation goal=new MapLocation(hq.x+place[p].x,hq.y+place[p].y);
			Direction best=rc.getLocation().directionTo(goal);
			Direction good=Util.tryMoveLite(rc,best);
			if(good!=null){
				rc.move(good);
				return;
			}else{
				//Ne gre
				MapLocation curr=rc.getLocation();
				Direction[] dir={best,best.rotateLeft(),best.rotateRight()};
				for(Direction d:dir){
					if(rc.senseRobotAtLocation(curr.add(d))!=null){
						continue;
					}
					if(rc.senseFlooding(curr.add(d))){
						if(rc.canDepositDirt(d)){
							rc.depositDirt(d);
							return;
						}else{
							for(Direction d2:Util.dir){
								if(!d2.equals(d)&&rc.senseRobotAtLocation(curr.add(d))==null){
									rc.digDirt(d2);
								}
							}
						}
					}
					int h=rc.senseElevation(curr);
					int h2=rc.senseElevation(curr.add(d));
					if(h2>h){
						if(rc.canDigDirt(d)){
							rc.digDirt(d);
							return;
						}else if(rc.canDepositDirt(d)){
							rc.depositDirt(d);
							return;
						}
					}else{
						if(rc.canDepositDirt(d)){
							rc.depositDirt(d);
							return;
						}else if(rc.canDigDirt(d)){
							rc.digDirt(d);
							return;
						}
					}
				}
			}
		}

	}



	@Override public void postcompute() throws GameActionException{

	}

}

class delivery_drone extends robot{
	RobotController rc;
	Direction init;
	MapLocation hq;
	boolean full=false;
	ArrayList<MapLocation> aqua=new ArrayList<MapLocation>();
	MapLocation s,drain;
	int time=1;
//	boolean[][]flooding;
	delivery_drone(RobotController rc){
		this.rc=rc;
	}

	@Override public void init(){
		for(RobotInfo r:rc.senseNearbyRobots(10,rc.getTeam())){
			if(r.type==RobotType.HQ){
				hq=r.location;
			}
		}
		s=null;
		drain=null;
		init=rc.getLocation().directionTo(hq).opposite();
//		flooding=new boolean[rc.getMapWidth()][rc.getMapHeight()];
	}

	@Override public void precompute() throws GameActionException{
		if(s!=null||drain!=null){
			return;
		}
		for(int i=rc.getRoundNum()-1;i<rc.getRoundNum();i++){
			Transaction[] q=rc.getBlock(rc.getRoundNum()-1);
//			System.out.println(Clock.getBytecodesLeft());
			for(Transaction t:q){
				if(t.getMessage()[0]==123456789&&t.getMessage()[1]==7){
					s=new MapLocation(t.getMessage()[2],t.getMessage()[3]);
					drain=new MapLocation(t.getMessage()[4],t.getMessage()[5]);
					time=rc.getRoundNum();
					return;
				}
			}
		}
	}

	@Override public void runTurn() throws GameActionException{
		if(!rc.isReady()){
			return;
		}
		System.out.println("lol");
		full=rc.isCurrentlyHoldingUnit();
		a:if(s!=null&&!full){
			if(rc.getRoundNum()-time>100){
				s=null;
				drain=null;
			}
			if(rc.canSenseLocation(s)){
				if(rc.senseRobotAtLocation(s)==null||rc.senseRobotAtLocation(s).type==RobotType.DELIVERY_DRONE){
					s=null;
					drain=null;
					break a;
				}else{
					if(rc.getLocation().distanceSquaredTo(s)<=2){
						if(rc.canPickUpUnit(rc.senseRobotAtLocation(s).ID)){
							rc.pickUpUnit(rc.senseRobotAtLocation(s).ID);
							full=true;
							s=null;
							return;
						}
					}
				}
			}
			Direction d=Util.tryMoveLiteDrone(rc,rc.getLocation().directionTo(s));
			if(d!=null){
				rc.move(d);
				return;
			}else{
				return;
			}
		}
		if(s==null&&drain!=null&&full){
			System.out.println(drain.x+" "+drain.y);
			if(rc.getLocation().distanceSquaredTo(drain)<=2){
				if(rc.getLocation().distanceSquaredTo(drain)==0){
					for(Direction d:Util.dir){
						if(rc.canMove(d)){
							rc.move(d);
							return;
						}
					}
				}else{
					Direction d=rc.getLocation().directionTo(drain);
					if(rc.canDropUnit(d)){
						rc.dropUnit(d);
						drain=null;
						return;
					}
				}
			}
			Direction d=Util.tryMoveLiteDrone(rc,rc.getLocation().directionTo(drain));
			if(d!=null&&rc.canMove(d)){
				rc.move(d);
				return;
			}
			if(d!=null&&rc.canMove(d)){
				rc.move(d);
				return;
			}
		}
		if(!full){
//			System.out.println(Clock.getBytecodesLeft());
			int closest=64*64;
			MapLocation best=null;
			RobotInfo[] rr=rc.senseNearbyRobots();
			for(RobotInfo r:rr) {
				if(r.team!=rc.getTeam()&&Util.d_inf(rc.getLocation(),r.location)<closest) {
					best=r.location;
					closest=Util.d_inf(rc.getLocation(),r.location);
				}
			}
			if(best!=null) {
				if(closest<=1) {
					if(rc.canPickUpUnit(rc.senseRobotAtLocation(best).ID)) {
						rc.pickUpUnit(rc.senseRobotAtLocation(best).ID);
						full=true;
						return;
					}
				}else {
					Direction d=Util.tryMoveLiteDrone(rc,rc.getLocation().directionTo(best));
					if(d!=null) {
						rc.move(d);
					}
					return;
				}

			}
			//ni dobrih opcij
//			System.out.println(Clock.getBytecodesLeft());
			int dist=Util.d_inf(hq,rc.getLocation());
			if(dist<3) {
				Direction d=Util.tryMoveLiteDrone(rc,rc.getLocation().directionTo(hq).opposite());
				if(d!=null) {
					rc.move(d);
				}
			}
			if(dist>6) {
				Direction d=Util.tryMoveLiteDrone(rc,rc.getLocation().directionTo(hq));
				if(d!=null) {
					rc.move(d);
				}
			}
			if(3<=dist&&dist<=6) {
				for(int i=0;i<10;i++) {
					Direction d=Util.getRandomDirection();
					if(rc.canMove(d)) {
						rc.move(d);
					}
				}
			}
//			System.out.println(Clock.getBytecodesLeft());
		}
		if(full&&drain==null){
			for(Direction d:Util.dir){
				if(rc.canSenseLocation(rc.getLocation().add(d))&&rc.senseFlooding(rc.getLocation().add(d))){
					if(rc.canDropUnit(d)){
						rc.dropUnit(d);
						full=false;
						return;
					}
				}
			}
			//Gremo do najbližje vode
			int range=Math.min(20,rc.getCurrentSensorRadiusSquared());
//			int l=Clock.getBytecodesLeft();
			for(int i=0;i<range;i++){
				for(MapLocation m:pc.range[i]){
					if(rc.canSenseLocation(m)){
						if(rc.senseFlooding(m)){
							Direction d=Util.tryMoveLiteDrone(rc,rc.getLocation().directionTo(m));
							if(d!=null){
								if(safeMove(d)){
									rc.move(d);
									return;
								}else if(safeMove(d.rotateLeft())){
									rc.move(d.rotateLeft());
									return;
								}else if(safeMove(d.rotateRight())){
									rc.move(d.rotateRight());
									return;
								}else if(safeMove(d.rotateLeft().rotateLeft())){
									rc.move(d.rotateLeft().rotateLeft());
									return;
								}else if(safeMove(d.rotateRight().rotateRight())){
									rc.move(d.rotateRight().rotateRight());
									return;
								}
							}
						}
					}
				}
			}
//			System.out.println("Cena "+(l-Clock.getBytecodesLeft()));
			MapLocation best=findClosest(aqua);
			Direction d;
			if(best!=null){
				d=Util.tryMoveLiteDrone(rc,rc.getLocation().directionTo(best));
			}else{
				d=init;
			}
			for(int i=0;i<10;i++){
				if(safeMove(d)){
					rc.move(d);
					return;
				}else{
					init=Util.getRandomDirection();
				}
			}
		}
	}
//	private Direction find_path(MapLocation drain2) throws GameActionException{
//		int[][] dist=new int[64][64];
//		Queue<MapLocation> bfs=new LinkedList<MapLocation>();
//		dist[rc.getLocation().x][rc.getLocation().y]=1;
//		bfs.add(rc.getLocation());
//		int[] dy={1,1,0,-1,-1,-1,0,1};
//		int[] dx={0,1,1,1,0,-1,-1,-1};
//		while(!bfs.isEmpty()){
//			MapLocation t=bfs.poll();
//			if(t.equals(drain2)){
//				while(true){
//					int curr=dist[t.x][t.y];
//					for(int i=0;i<8;i++){
//						int x=t.x+dx[i];
//						int y=t.y+dy[i];
//						if(dist[x][y]==curr-1){
//							t=new MapLocation(x,y);
//							if(t.equals(rc.getLocation())){
//								return Util.dir[i].opposite();
//							}
//							break;
//						}
//					}
//				}
//			}
//			int d=dist[t.x][t.y];
//			for(int i=0;i<8;i++){
//				int x=t.x+dx[i];
//				int y=t.y+dy[i];
//				if(x<0||x>=rc.getMapWidth()||y<0||y>=rc.getMapHeight()){
//					continue;
//				}
//				if(rc.canSenseLocation(new MapLocation(x,y))&&rc.senseRobotAtLocation(new MapLocation(x,y))!=null){
//					continue;
//				}
//				if(dist[x][y]==0){
//					dist[x][y]=1+d;
//					bfs.add(new MapLocation(x,y));
//				}
//			}
//		}
//		return null;
//	}

	public boolean safeMove(Direction d){
		if(d==null){
			return false;
		}
		if(!rc.canMove(d)){
			return false;
		}
		MapLocation end=rc.getLocation().add(d);
		for(RobotInfo r:rc.senseNearbyRobots(-1,rc.getTeam().opponent())){
			if((r.type==RobotType.NET_GUN||r.type==RobotType.HQ)&&r.team==rc.getTeam().opponent()){
				if(end.distanceSquaredTo(r.location)<=GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED){
					return false;
				}
			}
		}
		return true;
	}
	public MapLocation findClosest(Iterable<? extends MapLocation> somethings) throws GameActionException{
		MapLocation best=null;
		int dist=64*64;
		for(MapLocation m:somethings){
			int op=rc.getLocation().distanceSquaredTo(m);
			if(op<dist){
				dist=op;
				best=m;
			}
		}
		return best;
	}
	@Override public void postcompute() throws Exception{
		int range=rc.getCurrentSensorRadiusSquared();
		for(int i=0;i<range;i++){
			for(MapLocation mmm:pc.range[i]){
				if(Clock.getBytecodesLeft()<2000){
					return;
				}
//				System.out.println(Clock.getBytecodesLeft());
				MapLocation m=new MapLocation(rc.getLocation().x+mmm.x,rc.getLocation().y+mmm.y);
				if(rc.canSenseLocation(m)){
					if(rc.senseFlooding(m)){
						boolean ok=true;
						for(MapLocation mm:aqua){
							if(m.distanceSquaredTo(mm)<20){
								ok=false;
								break;
							}
						}
						if(ok){
							aqua.add(m);
						}
					}
				}
			}
		}
	}

}
