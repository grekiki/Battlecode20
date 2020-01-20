package grekiki3;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import battlecode.common.*;
import grekiki3.set_mp;

public class RobotPlayerTest{

	@Test public void testSanity(){
		set_mp set1 = new set_mp();
		set1.add(new MapLocation(0,0));
		System.out.println(Arrays.toString(set1.q));
		assertEquals(set1.contains(new MapLocation(0,0)),true);
		
	}

}
