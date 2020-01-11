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
		surovine.append(f(ml.x+","+ml.y)+"�");
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
		String s=f(ml.x+","+ml.y)+"�";
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
 
