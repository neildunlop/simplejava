package com.pathfinder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SimplejavaApplication.class)
public class SimplejavaApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void oneAddOneIsTwo() {

		assertEquals(1+1, 2);
	}

}
