package benchmarks;

import java.util.ArrayList;
import java.util.Arrays;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import grekiki27.pc;

class bfs {
	int[] qq;

	public bfs(ArrayList<Integer> q) {
		qq = new int[q.size()];
		for (int i = 0; i < q.size(); i++) {
			qq[i] = q.get(i);
		}
	}
}

class Random {
	static int init = 17;
	final static int mult = 69069;
	final static int add = 1;
	public static int rand() {
		init = init * mult + add;
		return init;
	}

	public static double random() {
		return rand() / 4294967296l;
	}
}

public class HQ extends robot {

	public HQ(RobotController rc) {
		super(rc);

	}

	@Override
	public void init() {

	}

	@Override
	public void precompute() {

	}

	@Override
	public void runTurn() throws Exception {
//		iskanje0();
//		iskanje1();

//		tabela0(new boolean[] {true,true,false,true});
//		tabela1(new boolean[] {true,true,false,true});
//		int[]q=new int[100];
//		for(int i=0;i<100;i++) {
//			q[i]=((i+7)*i)%117;
//		}
		int t = Clock.getBytecodesLeft();
//		Arrays.sort(q);
		for (int i = 0; i < 1000; i++) {
			Random.rand();
		}
		System.out.println(t - Clock.getBytecodesLeft() + " operacij");
		rc.disintegrate();
	}

	@Override
	public void postcompute() {

	}

	private void iskanje0() {// Vrne 3961
		int t = Clock.getBytecodesLeft();
		int count = 0;
		for (int qq = 0; qq < 40; qq++) {
			for (MapLocation m : pc.range[qq]) {
				MapLocation mm = new MapLocation(m.x + rc.getLocation().x, m.y + rc.getLocation().y);
				count++;
			}
		}
		System.out.println(count);
		System.out.println(t - Clock.getBytecodesLeft() + " operacij");
	}

	private void iskanje1() {// Vrne 3044
		int t = Clock.getBytecodesLeft();
		int x = rc.getLocation().x;
		int y = rc.getLocation().y;
		int count = 0;
		for (int qq = 0; qq < 40; qq++) {
			MapLocation[] q = pc.range[qq];
			int i = q.length;
			while (i-- > 0) {
				MapLocation mm = new MapLocation(q[i].x + x, q[i].y + y);
				count++;
			}
		}
		System.out.println(count);
		System.out.println(t - Clock.getBytecodesLeft() + " operacij");
	}

	private void tabela0(boolean[] bl) {
		int t = Clock.getBytecodesLeft();
		char[] read = new char[500];
		char[] read2 = Arrays.copyOf(read, 500);
//		String s=new String(read2);
		System.out.println(t - Clock.getBytecodesLeft() + " Cost");
//		rc.disintegrate();
	}

	private void tabela1(boolean[] bl) {
		int t = Clock.getBytecodesLeft();
		char[] read = new char[500];
		char[] read2 = read.clone();
		String s = new String(read2);
		System.out.println(t - Clock.getBytecodesLeft() + " Cost");
	}

}
