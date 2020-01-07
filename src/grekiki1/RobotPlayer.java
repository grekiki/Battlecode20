package grekiki1;
import java.util.Arrays;

import battlecode.common.*;

class vector_gl{
	MapLocation[] q;
	int p=0;
	int size=20;
	vector_gl(){
		q=new MapLocation[20];
	}
	void add(MapLocation ml){
		q[p++]=ml;
		if(p==size){
			q=Arrays.copyOf(q,2*size);
			size*=2;
		}
	}
	MapLocation get(int a){
		if(a<0||a>=size){
			System.out.println("Vector index out of bounds exception");
			return null;
		}else{
			return q[a];
		}
	}
}
abstract class robot{
	public abstract void precompute();
	public abstract void runTurn();
	public abstract void postcompute();
}
class HQ extends robot{
	int round;
	RobotController rc;
	HQ(RobotController rc){
		this.rc=rc;
		round=rc.getRoundNum();
	}
	@Override public void precompute(){
		// TODO Auto-generated method stub

	}
	@Override public void runTurn(){
		try{
			round++;
			int soup=rc.getTeamSoup();
			if(soup>=70&&rc.isReady()){
				for(Direction d:Direction.allDirections()){
					if(rc.canBuildRobot(RobotType.MINER,d)){
						rc.buildRobot(RobotType.MINER,d);
						return;
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	@Override public void postcompute(){
		// TODO Auto-generated method stub

	}
}
class miner extends robot{
	vector_gl surovine=new vector_gl();
	int round;
	int alive=0;
	RobotController rc;
	MapLocation HQ;
	miner(RobotController rc){
		this.rc=rc;
		round=rc.getRoundNum();
	}
	@Override public void precompute(){
		alive++;
		round++;
		if(alive==1){
			for(RobotInfo r:rc.senseNearbyRobots(2)){
				if(r.team==rc.getTeam()&&r.type==RobotType.HQ){
					HQ=r.location;
				}
			}
			if(HQ==null){
				alive--;//èe ne vemo kje je baza, a se sploh splaèa obstajati?
			}
		}
	}
	@Override public void runTurn(){
		try{
			if(rc.isReady()){
				if(rc.getSoupCarrying()==100){
					if(HQ==null){
						System.out.println("Kje je baza?");
					}else{
						if(rc.canMove(rc.getLocation().directionTo(HQ))){
							rc.move(rc.getLocation().directionTo(HQ));
							return;
						}
					}
				}
				for(Direction d:Direction.allDirections()){
					if(rc.canMineSoup(d)){
						rc.mineSoup(d);
						return;
					}
				}
				for(Direction d:Direction.allDirections()){
					if(rc.canDepositSoup(d)){
						rc.depositSoup(d,rc.getSoupCarrying());
						return;
					}
				}
				for(int i=0;i<10;i++){
					Direction d=Direction.allDirections()[(int)(Math.random()*Direction.allDirections().length)];
					if(rc.canMove(d)){
						rc.move(d);
						return;
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	@Override public void postcompute(){
		// TODO Auto-generated method stub

	}

}
public strictfp class RobotPlayer{
	static RobotController rc;
	static Direction[] directions={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};

	public static void run(RobotController rrrrr) throws GameActionException{
		rc=rrrrr;
		System.out.println(rc.getType()+" spawned");
		robot r=null;
		try{
			switch(rc.getType()){
				case HQ:
					r=new HQ(rc);
					break;
				case MINER:
					r=new miner(rc);
					break;
				case REFINERY:
//						runRefinery();
					break;
				case VAPORATOR:
//						runVaporator();
					break;
				case DESIGN_SCHOOL:
//						runDesignSchool();
					break;
				case FULFILLMENT_CENTER:
//						runFulfillmentCenter();
					break;
				case LANDSCAPER:
//						runLandscaper();
					break;
				case DELIVERY_DRONE:
//						runDeliveryDrone();
					break;
				case NET_GUN:
//						runNetGun();
					break;
				case COW:
					break;
				default:
					break;
			}

		}catch(Exception e){
			System.out.println(rc.getType()+" Exception");
			e.printStackTrace();
		}
		while(true){
			if(r!=null){
				r.precompute();
				r.runTurn();
				r.postcompute();
			}else{
				System.out.println("NAPAKA!!!");
			}
			Clock.yield();
		}
	}

}
