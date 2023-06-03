package com.tencent.polaris.configuration.client.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author fabian 2023-06-02
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFileIT {

	@Test(expected = IllegalArgumentException.class)
	public  void test() {
		System.out.println("Integration test is running...");
	}

}
