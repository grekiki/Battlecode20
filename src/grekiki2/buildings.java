package grekiki2;

import java.util.ArrayList;
import battlecode.common.*;

class paket{
	int[] data;
	int cost;

	paket(int[] a,int b){
		data=a;
		cost=b;
	}
}

class HQ extends robot{
	RobotController rc;
	Direction[] dir={Direction.NORTH,Direction.NORTHEAST,Direction.EAST,Direction.SOUTHEAST,Direction.SOUTH,Direction.SOUTHWEST,Direction.WEST,Direction.NORTHWEST};
	MapLocation curr;
	int h,w;
	int prevPhase=-1;
	int phase=0;
	blockchain b;
	ArrayList<MapLocation> polja;
	ArrayList<MapLocation> refinery;
	// phase 0
	Direction[] goodMiners;
	int miners=0;
	//phase 1
	
	//phase 2
	int initMiner=-1;
	HQ(RobotController rc){
		this.rc=rc;
	}

	public int f(int a) {
		if(a==1) {
			return phase>1?5:4;
		}else {
			return 2*a+4;
		}
	}
	
	public void computeData(int phase) throws GameActionException{
		if(phase==0){
			int count=0;
			int lenLimit=10;
			// A se splaca delavca poslati v to smer?
			boolean left=curr.x>lenLimit;
			boolean right=(w-curr.x)>lenLimit;
			boolean top=(h-curr.y)>lenLimit;
			boolean bot=curr.y>lenLimit;
			if(top&&Math.abs(rc.senseElevation(curr)-rc.senseElevation(curr.add(Direction.NORTH)))<=3){
				count++;
			}
			if(top&&right&&Math.abs(rc.senseElevation(curr)-rc.senseElevation(curr.add(Direction.NORTHEAST)))<=3){
				count++;
			}
			if(right&&Math.abs(rc.senseElevation(curr)-rc.senseElevation(curr.add(Direction.EAST)))<=3){
				count++;
			}
			if(right&&bot&&Math.abs(rc.senseElevation(curr)-rc.senseElevation(curr.add(Direction.SOUTHEAST)))<=3){
				count++;
			}
			if(bot&&Math.abs(rc.senseElevation(curr)-rc.senseElevation(curr.add(Direction.SOUTH)))<=3){
				count++;
			}
			if(bot&left&&Math.abs(rc.senseElevation(curr)-rc.senseElevation(curr.add(Direction.SOUTHWEST)))<=3){
				count++;
			}
			if(left&&Math.abs(rc.senseElevation(curr)-rc.senseElevation(curr.add(Direction.WEST)))<=3){
				count++;
			}
			if(left&&top&&Math.abs(rc.senseElevation(curr)-rc.senseElevation(curr.add(Direction.NORTHWEST)))<=3){
				count++;
			}
			goodMiners=new Direction[count];
//			System.out.println(top+" "+right+" "+bot+" "+left);
//			System.out.println("Nasteli smo "+count+" pozicij za minerje");
			count=0;
			if(top){
				goodMiners[count++]=Direction.NORTH;
			}
			if(top&&right){
				goodMiners[count++]=Direction.NORTHEAST;
			}
			if(right){
				goodMiners[count++]=Direction.EAST;
			}
			if(right&&bot){
				goodMiners[count++]=Direction.SOUTHEAST;
			}
			if(bot){
				goodMiners[count++]=Direction.SOUTH;
			}
			if(bot&left){
				goodMiners[count++]=Direction.SOUTHWEST;
			}
			if(left){
				goodMiners[count++]=Direction.WEST;
			}
			if(left&&top){
				goodMiners[count++]=Direction.NORTHWEST;
			}
		}else if(phase==1){

		}
	}

	public void readBlockchain(int round) throws GameActionException{
		Transaction[] t=rc.getBlock(round);
		b.checkQueue();
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
					}else if(msg[1]==4) {
						MapLocation m=new MapLocation(msg[2],msg[3]);
						if(!refinery.contains(m)) {
							refinery.add(m);
						}
					}
				}
			}
		}
	}

	@Override public void init(){
		curr=rc.getLocation();
		h=rc.getMapHeight();
		w=rc.getMapWidth();
		b=new blockchain(rc);
		polja=new ArrayList<MapLocation>();
		refinery=new ArrayList<MapLocation>();
	}

	@Override public void precompute() throws GameActionException{
		if(prevPhase!=phase){
			try{
				computeData(phase);
			}catch(Exception e){
				e.printStackTrace();
			}
			prevPhase=phase;
		}
		if(rc.getRoundNum()>1){
			readBlockchain(rc.getRoundNum()-1);
		}
//		System.out.println("Faza "+phase);
	}

	@Override public void runTurn() throws GameActionException{
		if(!rc.isReady()){
			return;
		}
		RobotInfo closest=null;
		int dis=1000000;
		for(RobotInfo r:rc.senseNearbyRobots(15)) {
			if(r.team==rc.getTeam().opponent()&&r.getType()==RobotType.DELIVERY_DRONE) {
				int t=rc.getLocation().distanceSquaredTo(r.location);
				if(t<dis) {
					dis=t;
					closest=r;
				}
			}
		}
		if(closest!=null&&rc.canShootUnit(closest.ID)) {
			rc.shootUnit(closest.ID);
			return;
		}
		if(phase==0){
			if(miners<goodMiners.length&&rc.getTeamSoup()>=70&&rc.canBuildRobot(RobotType.MINER,goodMiners[miners])){
				rc.buildRobot(RobotType.MINER,goodMiners[miners]);
				if(miners==0) {
					initMiner=rc.senseRobotAtLocation(rc.getLocation().add(goodMiners[miners])).ID;
				}
				miners++;
				return;
			}
		}else if(phase==1||phase==2){
			if(rc.getTeamSoup()>=(phase>1?200:70)&&miners<f(polja.size())){//Nakup minerjev?
				if(polja.size()>0){//Ce imamo polja minerje usmerimo da so blizje
					MapLocation best=null;
					int dist=64*64;
					for(MapLocation m:polja){
						int op=rc.getLocation().distanceSquaredTo(m);
						if(op<dist){
							dist=op;
							best=m;
						}
					}
					Direction d=rc.getLocation().directionTo(best);
					if(rc.canBuildRobot(RobotType.MINER,d)){
						rc.buildRobot(RobotType.MINER,d);
						miners++;
						return;
					}else if(rc.canBuildRobot(RobotType.MINER,d.rotateLeft())){
						rc.buildRobot(RobotType.MINER,d.rotateLeft());
						miners++;
						return;
					}else if(rc.canBuildRobot(RobotType.MINER,d.rotateRight())){
						rc.buildRobot(RobotType.MINER,d.rotateRight());
						miners++;
						return;
					}else {
						for(Direction dd:Util.dir){
							if(rc.canBuildRobot(RobotType.MINER,dd)){
								rc.buildRobot(RobotType.MINER,dd);
								miners++;
								return;
							}
						}
					}
				}else{//Drugace pac kamor lahko
					for(Direction d:Util.dir){
						if(rc.canBuildRobot(RobotType.MINER,d)){
							rc.buildRobot(RobotType.MINER,d);
							miners++;
							return;
						}
					}
				}
			}
		}
	}

	@Override public void postcompute() throws GameActionException{
		if(phase==0){
			if(miners>=goodMiners.length||polja.size()>0){
				phase=1;
				int[] msg=new int[7];
				msg[0]=123456789;
				msg[1]=1;
				msg[2]=phase;
				b.sendMsg(new paket(msg,1));
			}
		}
		if(phase==1) {
			if(rc.getTeamSoup()>=150) {
				phase=2;
				int[] msg=new int[7];
				msg[0]=123456789;
				msg[1]=1;
				msg[2]=phase;
				int dist=1000000;
				for(RobotInfo u:rc.senseNearbyRobots(-1, rc.getTeam())) {
					if(u.type==RobotType.MINER) {
						int t=rc.getLocation().distanceSquaredTo(u.location);
						if(t<dist) {
							dist=t;
							msg[3]=u.ID;
						}
					}
				}
				if(dist==1000000) {
					msg[3]=initMiner;
				}
				b.sendMsg(new paket(msg,1));
			}
		}
		if(phase==2) {//Preverimo èe imamo zid
			boolean ok=true;
			for(MapLocation m:landscaper.place) {
				MapLocation check=new MapLocation(rc.getLocation().x+m.x,rc.getLocation().y+m.y);
				if(!rc.canSenseLocation(check)||(rc.senseRobotAtLocation(check)!=null&&rc.senseRobotAtLocation(check).type!=RobotType.LANDSCAPER)) {
					ok=false;
					break;
				}
			}
			if(ok) {
				phase=3;
				int[] msg=new int[7];
				msg[0]=123456789;
				msg[1]=1;
				msg[2]=phase;
				b.sendMsg(new paket(msg,1));
			}
		}
		for(MapLocation m:polja){
			rc.setIndicatorDot(m,0,255,255);
		}

	}

}

class refinery extends robot{
	//Ne vem kaj bi tukaj sploh napisal
	RobotController rc;
	public refinery(RobotController r){
		rc=r;
	}
	@Override public void init(){}
	@Override public void precompute(){}
	@Override public void runTurn(){}
	@Override public void postcompute(){}

}

class vaporator extends robot{
	//Ne vem kaj bi tukaj sploh napisal
	RobotController rc;
	public vaporator(RobotController r){
		rc=r;
	}
	@Override public void init(){}
	@Override public void precompute(){}
	@Override public void runTurn(){}
	@Override public void postcompute(){}

}


class design_school extends robot{
	RobotController rc;
	int diggers=0;
	public design_school(RobotController r){
		rc=r;
	}

	@Override public void init(){
		// TODO Auto-generated method stub

	}

	@Override public void precompute(){
		// TODO Auto-generated method stub

	}

	@Override public void runTurn() throws GameActionException{
		if(!rc.isReady()) {
			return;
		}
		if(diggers==0) {
			if(rc.getTeamSoup()>=RobotType.LANDSCAPER.cost) {
				if(rc.canBuildRobot(RobotType.LANDSCAPER,Direction.WEST)) {
					rc.buildRobot(RobotType.LANDSCAPER,Direction.WEST);
					diggers++;
					return;
				}
			}
		}else if(diggers<8) {
			if(rc.getTeamSoup()>=50+RobotType.LANDSCAPER.cost) {
				if(rc.canBuildRobot(RobotType.LANDSCAPER,Direction.WEST)) {
					rc.buildRobot(RobotType.LANDSCAPER,Direction.WEST);
					diggers++;
					return;
				}
			}
		}
		else if(diggers<16) {
			if(rc.getTeamSoup()>=400+RobotType.LANDSCAPER.cost) {
				if(rc.canBuildRobot(RobotType.LANDSCAPER,Direction.WEST)) {
					rc.buildRobot(RobotType.LANDSCAPER,Direction.WEST);
					diggers++;
					return;
				}
			}
		}
	}

	@Override public void postcompute(){
		// TODO Auto-generated method stub

	}

}

class fulfillment_center extends robot{
	RobotController rc;
	int droni=0;
	public fulfillment_center(RobotController r){
		rc=r;
	}

	@Override public void init(){
		// TODO Auto-generated method stub

	}

	@Override public void precompute(){
		// TODO Auto-generated method stub

	}

	@Override public void runTurn() throws Exception{
		if(!rc.isReady()) {
			return;
		}
		if(droni<1&&rc.getTeamSoup()>=RobotType.DELIVERY_DRONE.cost) {
			if(rc.canBuildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTHWEST)) {
				rc.buildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTHWEST);
				droni++;
				return;
			}
			if(rc.canBuildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTH)) {
				rc.buildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTH);
				droni++;
				return;
			}
		}
		if(droni<5&&rc.getTeamSoup()>=RobotType.DELIVERY_DRONE.cost+250) {//Refinerije imajo prednost
			if(rc.canBuildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTHWEST)) {
				rc.buildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTHWEST);
				droni++;
				return;
			}
			if(rc.canBuildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTH)) {
				rc.buildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTH);
				droni++;
				return;
			}
		}else if(rc.getTeamSoup()>=800) {
			if(rc.canBuildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTHWEST)) {
				rc.buildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTHWEST);
				droni++;
				return;
			}
			if(rc.canBuildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTH)) {
				rc.buildRobot(RobotType.DELIVERY_DRONE,Direction.SOUTH);
				droni++;
				return;
			}
		}

	}

	@Override public void postcompute(){
		// TODO Auto-generated method stub

	}

}

class net_gun extends robot{
	RobotController rc;

	public net_gun(RobotController r){
		rc=r;
	}

	@Override public void init(){}

	@Override public void precompute(){}

	@Override public void runTurn() throws GameActionException{
		if(!rc.isReady()) {
			return;
		}
		RobotInfo closest=null;
		int dist=1000000;
		for(RobotInfo r:rc.senseNearbyRobots()) {
			if(r.team==rc.getTeam().opponent()&&r.getType()==RobotType.DELIVERY_DRONE) {
				int t=rc.getLocation().distanceSquaredTo(r.location);
				if(t<dist) {
					dist=t;
					closest=r;
				}
			}
		}
		if(closest==null) {
			return;
		}
		if(rc.canShootUnit(closest.ID)) {
			rc.shootUnit(closest.ID);
		}
	}

	@Override public void postcompute(){}

}