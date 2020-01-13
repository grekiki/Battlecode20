package grekiki2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

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
		int curr=rc.getRoundNum();
		for(int i=1;i<curr;i++){
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
		if(phase==0||phase==1||phase==2||phase==3){//TO-DO optimiziraj fazo 3. 
			//Obdelamo potrebo po refineriji
			if(surovine.size()>0&&rc.getTeamSoup()>=RobotType.REFINERY.cost){
				MapLocation closest=findClosest(surovine);
				if(rc.getLocation().distanceSquaredTo(closest)<10){
					//Preverimo èe je refinerija potrebna
					MapLocation ref=closest.add(hq.directionTo(closest));
					if(Util.d_inf(hq,ref)>=4){//Ne na zidu... Pa ne motimo baze
						int dist=20;
						for(MapLocation re:refinery){
							dist=Math.min(dist,re.distanceSquaredTo(ref));
						}
						if(dist==20){
							//Potrebujemo refinerijo.
							//Poskusimo èe smo dovolj blizu
							if(ref.distanceSquaredTo(rc.getLocation())<=2){
								Direction d=rc.getLocation().directionTo(ref);
								if(rc.getTeamSoup()>=RobotType.REFINERY.cost){
									if(rc.canBuildRobot(RobotType.REFINERY,d)){
										rc.buildRobot(RobotType.REFINERY,d);
										int[] msg={123456789,4,rc.getLocation().x+d.dx,rc.getLocation().y+d.dy,0,0,0};
										b.sendMsg(new paket(msg,1));
										return;
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
			a:if(phase==2&&rc.getTeamSoup()>=RobotType.NET_GUN.cost+200){//Rafinerije potrebujejo drone za obrambo. Tovarna vsaj 3 stran po d_inf metriki
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
				for(Direction d:Util.dir){
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
			if(rc.canSenseLocation(f1)&&rc.senseRobotAtLocation(f1)!=null&&rc.senseRobotAtLocation(f1).type==RobotType.FULFILLMENT_CENTER&&rc.getTeamSoup()>=RobotType.DESIGN_SCHOOL.cost){
				MapLocation f2=hq.add(Direction.SOUTHEAST);
				if(rc.getLocation().distanceSquaredTo(f2)<=2&&!f2.equals(rc.getLocation())){
					if(rc.getTeamSoup()>=RobotType.DESIGN_SCHOOL.cost&&rc.canBuildRobot(RobotType.DESIGN_SCHOOL,rc.getLocation().directionTo(f2))){
						rc.buildRobot(RobotType.DESIGN_SCHOOL,rc.getLocation().directionTo(f2));
						fazaBaze=1;
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
		if(fazaBaze==1){//postavimo design schoole

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
		checkForEmptyField();
		findResources(range);
		discardMissing();
	}

	public void checkForEmptyField() throws GameActionException{
		int range=rc.getCurrentSensorRadiusSquared();
		for(MapLocation polje:polja){
			if(range>=rc.getLocation().distanceSquaredTo(polje)+polmer_surovine){
				boolean ok=false;
				for(int i=0;i<=polmer_surovine;i++){
					if(Clock.getBytecodesLeft()<1000){
						return;
					}
					MapLocation[] scan=pc.range[i];
					for(MapLocation mm:scan){
						MapLocation m=new MapLocation(polje.x+mm.x,polje.y+mm.y);
						if(rc.canSenseLocation(m)&&!rc.senseFlooding(m)&&rc.senseSoup(m)>0){
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
	@Override public void precompute(){
		// TODO Auto-generated method stub

	}
	int p=0;
	boolean positioned=false;
	@Override public void runTurn() throws GameActionException{
		if(rc.getLocation().equals(new MapLocation(hq.x+place[p].x,hq.y+place[p].y))){
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
			if(Util.d_inf(goal,rc.getLocation())==1){
				//Skoraj smo na cilju
				if(rc.canMove(best)) {
					rc.move(best);
					return;
				}
				int currH=rc.senseElevation(rc.getLocation());
				int next=rc.senseElevation(rc.getLocation().add(best));
				if(next>currH){
					if(rc.canDigDirt(best)){
						rc.digDirt(best);
						return;
					}else{
						rc.depositDirt(Direction.CENTER);
						return;
					}
				}else {
					if(rc.canDepositDirt(best)){
						rc.depositDirt(best);
						return;
					}else{
						rc.digDirt(Direction.CENTER);
						return;
					}
				}

			}
			Direction d=Util.tryMove(rc,best);
			if(d!=null&&rc.canMove(d)){
				rc.move(d);
				return;
			}else{
				System.out.println("Premik ne dela "+best);
				System.out.println(place[p].x+" "+place[p].y);
			}
		}

	}

	@Override public void postcompute(){
		// TODO Auto-generated method stub

	}

}

class delivery_drone extends robot{
	RobotController rc;
	Direction init;
	MapLocation hq;
	boolean full=false;
	ArrayList<MapLocation> aqua=new ArrayList<MapLocation>();
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
		init=rc.getLocation().directionTo(hq).opposite();
//		flooding=new boolean[rc.getMapWidth()][rc.getMapHeight()];
	}

	@Override public void precompute(){
		// TODO Auto-generated method stub

	}

	@Override public void runTurn() throws GameActionException{
		if(!rc.isReady()){
			return;
		}
		if(!full){
			for(Direction d:Util.dir){
				if(rc.canSenseLocation(rc.getLocation().add(d))&&rc.senseRobotAtLocation(rc.getLocation().add(d))!=null&&rc.senseRobotAtLocation(rc.getLocation().add(d)).team!=rc.getTeam()){
					if(rc.canPickUpUnit(rc.senseRobotAtLocation(rc.getLocation().add(d)).ID)){
						rc.pickUpUnit(rc.senseRobotAtLocation(rc.getLocation().add(d)).ID);
						full=true;
						return;
					}
				}
			}
			int best=1000000;
			MapLocation m=null;
			for(RobotInfo r:rc.senseNearbyRobots()){
				if(r.team!=rc.getTeam()&&(r.type==RobotType.COW||r.type==RobotType.MINER||r.type==RobotType.LANDSCAPER)&&rc.getLocation().distanceSquaredTo(r.location)<best){
					best=rc.getLocation().distanceSquaredTo(r.location);
					m=r.location;
				}
			}
			if(m!=null&&!full){
				init=rc.getLocation().directionTo(m);
			}
			for(int i=0;i<7;i++){
				if(rc.canMove(init)){
					rc.move(init);
					return;
				}else{
					init=Util.getRandomDirection();
				}
			}
		}
		if(full){
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
			int l=Clock.getBytecodesLeft();
			for(int i=0;i<range;i++){
				for(MapLocation m:pc.range[i]){
					if(rc.canSenseLocation(m)){
						if(rc.senseFlooding(m)){
							Direction d=Util.tryMoveLiteDrone(rc,rc.getLocation().directionTo(m));
							if(d!=null){
								rc.move(d);
								return;
							}
						}
					}
				}
			}
			System.out.println("Cena "+(Clock.getBytecodesLeft()-l));
			MapLocation best=findClosest(aqua);
			if(best==null){
				best=hq;//mogoèe dela?
			}
			Direction d=Util.tryMoveLiteDrone(rc,rc.getLocation().directionTo(best));
			if(d!=null){
				rc.move(d);
				return;
			}
		}
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
							System.out.println(m.x+" "+m.y+" voda");
							aqua.add(m);
						}
					}
				}
			}
		}
	}

}
