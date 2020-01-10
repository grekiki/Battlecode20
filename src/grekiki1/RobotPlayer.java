package grekiki1;
import java.util.Arrays;

import battlecode.common.*;
import battlecode.world.GameWorld;

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
	boolean remove(MapLocation ml){
		for(int i=0;i<p;i++){
			if(q[i].equals(ml)){
				for(int j=i;j<p-1;j++){
					q[j]=q[j+1];
				}
				size--;
				return true;
			}
		}
		return false;
	}
}
class vector_gl2{
	StringBuffer surovine=new StringBuffer("");
	int size=0;
	vector_gl2(){
	}
	String f(String a){
		char[] buffer=new char[10];
		for(int i=0;i<a.length();i++){
			buffer[i]=a.charAt(i);
		}
		return new String(buffer);
	}
	void add(MapLocation ml){
		size++;
		surovine.append(f(ml.x+","+ml.y)+"è");
	}
	MapLocation get(int a){
		String s=surovine.substring(11*a,11*a+10);
		int p=-1;
		for(int i=0;i<s.length();i++){
			if(s.charAt(i)==','){
				p=i;
				break;
			}
		}
		if(p==-1){
			System.out.println("Napaka");
			return null;
		}else{
			int x=Integer.parseInt(s.substring(0,p));
			int y=Integer.parseInt(s.substring(p+1,s.length()).replace(""+(char)0,""));
			return new MapLocation(x,y);
		}
	}
	boolean remove(MapLocation ml){
		String s=f(ml.x+","+ml.y)+"è";
		if(surovine.indexOf(s)!=-1){
			surovine.replace(surovine.indexOf(s),surovine.indexOf(s)+s.length(),"");
			size--;
			return true;
		}else{
			return false;
		}
	}
	boolean contains(MapLocation ml){
		return surovine.indexOf(f(ml.x+","+ml.y))!=-1;
	}
}

abstract class robot{
	public abstract void precompute();
	public abstract void runTurn();
	public abstract void postcompute();
}

class bitcoin{
	public static int[] generateMessage(String type,int[] contents,int turn){
		int[] ans=new int[7];
		ans[0]=turn;
		if(type.equals("surovina")){
			ans[1]=1;
			for(int i=0;i<4;i++){
				ans[2+i]=contents[i];
			}
			ans[6]=42;
		}else if(type.equals("odstrani surovino")) {
			ans[1]=2;
			for(int i=0;i<4;i++){
				ans[2+i]=contents[i];
			}
			ans[6]=42;
		}
		return ans;
	}

}
class HQ extends robot{
	int round;
	RobotController rc;
	int miners;
	HQ(RobotController rc){
		this.rc=rc;
		round=rc.getRoundNum();
		miners=0;
	}
	@Override public void precompute(){

	}
	@Override public void runTurn(){
		try{
			round++;
			int soup=rc.getTeamSoup();
			if(miners<5&&soup>=70&&rc.isReady()){
				for(Direction d:Direction.allDirections()){
					if(rc.canBuildRobot(RobotType.MINER,d)){
						rc.buildRobot(RobotType.MINER,d);
						miners++;
						return;
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	@Override public void postcompute(){

	}
}
class miner extends robot{
	int round;
	int alive=0;
	RobotController rc;
	MapLocation HQ;
	vector_gl2 surovine=new vector_gl2();
	miner(RobotController rc){
		this.rc=rc;
		round=rc.getRoundNum();
	}
	@Override public void precompute(){
		if(alive==0){
			for(RobotInfo r:rc.senseNearbyRobots(2)){
				if(r.team==rc.getTeam()&&r.type==RobotType.HQ){
					HQ=r.location;
				}
			}
			if(HQ==null){
				alive--;//èe ne vemo kje je baza, a se sploh splaèa obstajati?
			}
		}
		for(int i=0;i<surovine.size;i++) {
			System.out.println(surovine.get(i).x+" "+surovine.get(i).y);
		}
		if(round>0){
			try{
				Transaction[] q=rc.getBlock(round-1);
				for(Transaction t:q){
//					System.out.println("Prejeto "+Arrays.toString(t.getMessage()));
					if(t.getMessage()[6]==42&&t.getMessage()[0]==round-1){
						if(t.getMessage()[1]==1){//surovina
							if(!surovine.contains(new MapLocation(t.getMessage()[2],t.getMessage()[3]))){
								surovine.add(new MapLocation(t.getMessage()[2],t.getMessage()[3]));
//								System.out.println("Dodana surovina "+new MapLocation(t.getMessage()[2],t.getMessage()[3]).x+" "+new MapLocation(t.getMessage()[2],t.getMessage()[3]).y);
							}else{
//								System.out.println("Surovina obstaja?");
							}
						}else if(t.getMessage()[1]==2) {
							MapLocation rem=new MapLocation(t.getMessage()[2],t.getMessage()[3]);
							surovine.remove(rem);
						}
					}
				}
			}catch(GameActionException e){
				e.printStackTrace();
			}
		}

	}
	@Override public void runTurn(){
		try{
			if(rc.isReady()&&alive>0){//inicializacija je bila upamo da uspešna
				Direction[] dirs={Direction.NORTH,Direction.NORTHEAST,Direction.EAST,Direction.SOUTHEAST,Direction.SOUTH,Direction.SOUTHWEST,Direction.WEST,Direction.NORTHWEST};
				if(rc.getSoupCarrying()==100){
					Direction dirHQ=rc.getLocation().directionTo(HQ);
					if(rc.canMove(dirHQ)&&!rc.senseFlooding(rc.getLocation().add(dirHQ))){
						rc.move(rc.getLocation().directionTo(HQ));
						return;
					}
				}
				for(Direction d:dirs){
					if(rc.canMineSoup(d)){
						rc.mineSoup(d);
						return;
					}
				}
				for(Direction d:dirs){
					if(rc.canDepositSoup(d)){
						rc.depositSoup(d,rc.getSoupCarrying());
						return;
					}
				}
				GameWorld gw;
				if(surovine.size>0){
					MapLocation best=null;
					int shortest=64*64+1;
					for(int i=0;i<surovine.size;i++){
						MapLocation ml=surovine.get(i);
						int dist=(rc.getLocation().x-ml.x)*(rc.getLocation().x-ml.x)+(rc.getLocation().y-ml.y)*(rc.getLocation().y-ml.y);
						if(dist<shortest){
							shortest=dist;
							best=ml;
						}
					}
					Direction d=rc.getLocation().directionTo(best);
					if(rc.canMove(d)&&!rc.senseFlooding(rc.getLocation().add(d))){
						rc.move(d);
						return;
					}
				}
				for(int i=0;i<10;i++){
					Direction d=dirs[(int)(Math.random()*dirs.length)];
					if(rc.canMove(d)&&!rc.senseFlooding(rc.getLocation().add(d))){
						rc.move(d);
						return;
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	@Override public void postcompute(){//Drage operacije so lahko le tukaj. Potrebno je preverjati koliko bytecodov je še ostalo!
		alive++;
		round++;
		int range=rc.getCurrentSensorRadiusSquared();
		MapLocation curr=rc.getLocation();
		for(int i=0;i<surovine.size;i++) {
			MapLocation check=surovine.get(i);
			try{
				if(rc.canSenseLocation(check)&&rc.senseSoup(check)==0){
					int[] block=bitcoin.generateMessage("odstrani surovino",new int[]{check.x,check.y,0,0},round-1);
					if(rc.canSubmitTransaction(block,2)){
						rc.submitTransaction(block,2);
						rc.setIndicatorDot(check,255,0,0);
//						System.out.println("Poslano"+Arrays.toString(block));
						//System.out.println(t.x+" "+t.y);
					}
				}
			}catch(GameActionException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(int dx=0;dx+dx<range;dx++){
			int used=dx*dx;
			for(int dy=0;dy*dy<range-used;dy++){
				MapLocation[] tocke={new MapLocation(curr.x+dx,curr.y+dy),new MapLocation(curr.x-dx,curr.y+dy),new MapLocation(curr.x+dx,curr.y-dy),new MapLocation(curr.x-dx,curr.y-dy)};
				for(MapLocation t:tocke){
					if(Clock.getBytecodesLeft()>1000){
						try{
							if(rc.canSenseLocation(t)) {
								int soup=rc.senseSoup(t);
								if(soup>0){
									if(!surovine.contains(t)){
										int money=rc.getTeamSoup();
										if(money>5){
											int[] block=bitcoin.generateMessage("surovina",new int[]{t.x,t.y,0,0},round-1);
											if(rc.canSubmitTransaction(block,2)){
												rc.submitTransaction(block,2);
												rc.setIndicatorDot(t,0,0,255);
//												System.out.println("Poslano"+Arrays.toString(block));
												//System.out.println(t.x+" "+t.y);
											}
										}
									}
								}
							}
						}catch(Exception e){
							e.printStackTrace();
						}
					}else{
						System.out.println(dx+" "+dy);
						return;
					}
				}
			}
		}
	}
}
public strictfp class RobotPlayer{
	static RobotController rc;
	static Direction[] directions={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};

	public static void run(RobotController rrrrr) throws GameActionException{
		rc=rrrrr;
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
