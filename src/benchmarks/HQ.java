package benchmarks;

import java.util.Arrays;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import grekiki25.pc;

public class HQ extends robot{

	public HQ(RobotController rc){
		super(rc);
		
	}

	@Override public void init(){
		

	}

	@Override public void precompute(){
		

	}

	@Override public void runTurn() throws Exception{
//		iskanje0();
//		iskanje1();

		tabela0(new boolean[] {true,true,false,true});
		tabela1(new boolean[] {true,true,false,true});
		rc.disintegrate();
	}
	


	@Override public void postcompute(){
		

	}
	private void iskanje0(){//Vrne 3961
		int t=Clock.getBytecodesLeft();
		int count=0;
		for(int qq=0;qq<40;qq++){
			for(MapLocation m:pc.range[qq]) {
				MapLocation mm=new MapLocation(m.x+rc.getLocation().x,m.y+rc.getLocation().y);
				count++;
			}
		}
		System.out.println(count);
		System.out.println(t-Clock.getBytecodesLeft()+" operacij");
	}
	private void iskanje1(){//Vrne 3044
		int t=Clock.getBytecodesLeft();
		int x=rc.getLocation().x;
		int y=rc.getLocation().y;
		int count=0;
		for(int qq=0;qq<40;qq++){
			MapLocation[]q=pc.range[qq];
			int i=q.length;
			while(i --> 0) {
				MapLocation mm=new MapLocation(q[i].x+x,q[i].y+y);
				count++;
			}
		}
		System.out.println(count);
		System.out.println(t-Clock.getBytecodesLeft()+" operacij");
	}

	private void tabela0(boolean[]bl) {
		int t=Clock.getBytecodesLeft();
		char[] read=new char[500];
		char[] read2=Arrays.copyOf(read, 500);
//		String s=new String(read2);
		System.out.println(t-Clock.getBytecodesLeft()+" Cost");
//		rc.disintegrate();
	}
	private void tabela1(boolean[]bl) {
		int t=Clock.getBytecodesLeft();
		char[] read=new char[500];
		char[] read2=read.clone();
		String s=new String(read2);
		System.out.println(t-Clock.getBytecodesLeft()+" Cost");
	}

}
