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
	blockchain b;

	// phase 0
	boolean done;// ali smo konec z premikanjem v tej smeri
	Direction init;
	// phase 1

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

		if(phase==0||phase==1){
			//Obdelamo potrebo po refineriji
			if(surovine.size()>0){
				MapLocation closest=findClosest(surovine);
				if(rc.getLocation().distanceSquaredTo(closest)<10&&closest.distanceSquaredTo(hq)>30){
					//Preverimo èe je refinerija potrebna
					MapLocation ref=closest.add(hq.directionTo(closest));
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
								}
							}
						}else{
							Direction d=Util.tryMove(rc,rc.getLocation().directionTo(ref));
							if(rc.canMove(d)){
								rc.move(d);
								return;
							}
						}
					}
				}
			}
			//Najprej poskusimo èe smo polni
			if(rc.getSoupCarrying()==100) {
				for(Direction d:Util.dir){
					if(rc.canDepositSoup(d)){
						rc.depositSoup(d,rc.getSoupCarrying());
						return;
					}
				}
				if(moveClosest(refinery)){
					return;
				}else{
					System.out.println("Ne moremo priti do najblizje refinerije!");
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
		for(Iterator<MapLocation> it=surovine.iterator();it.hasNext();){
			if(Clock.getBytecodesLeft()<1000){
				return;
			}
			MapLocation m=it.next();
			if(rc.canSenseLocation(m)&&rc.senseSoup(m)==0){
				surovine.remove(m);
			}
		}
	}
}

class landscaper extends robot{
	RobotController rc;

	landscaper(RobotController rc){
		this.rc=rc;
	}

	@Override public void init(){
		// TODO Auto-generated method stub

	}

	@Override public void precompute(){
		// TODO Auto-generated method stub

	}

	@Override public void runTurn(){
		// TODO Auto-generated method stub

	}

	@Override public void postcompute(){
		// TODO Auto-generated method stub

	}

}

class delivery_drone extends robot{
	RobotController rc;

	delivery_drone(RobotController rc){
		this.rc=rc;
	}

	@Override public void init(){
		// TODO Auto-generated method stub

	}

	@Override public void precompute(){
		// TODO Auto-generated method stub

	}

	@Override public void runTurn(){
		// TODO Auto-generated method stub

	}

	@Override public void postcompute(){
		// TODO Auto-generated method stub

	}

}