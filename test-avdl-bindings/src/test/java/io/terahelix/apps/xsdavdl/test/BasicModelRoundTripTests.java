package io.terahelix.apps.xsdavdl.test;


import Generated.*;
import io.terahelix.common.HelixLogger;
import io.terahelix.spear.javaRuntime.util.SpearHelpers;
import org.junit.Assert;
import org.junit.Test;
import io.terahelix.xsd.mapper.*;

import java.io.StringReader;
import java.util.*;
/**
 * A collection of tests to generate some random data
 * and round-trips.
 */
public class BasicModelRoundTripTests
{
	private static final HelixLogger logger = HelixLogger.getLogger(BasicModelRoundTripTests.class);

	public static final int ITEMS = 10;

	private final List<Car> vehicles;
	private final SpearXMLMapperService xmlMapper;

	public BasicModelRoundTripTests()
	{
		List<Car> vs = new ArrayList<>();
		for (int i = 0; i < ITEMS; i++)
		{
			vs.add(SpearHelpers.randomize(Car.class));
		}
		vehicles = Collections.unmodifiableList(vs);
		logger.info("Created a total of " + ITEMS + " Cars (Spear Denominated)");

		xmlMapper = SpearXMLMapperService.getInstance();
	}

	@Test
	public void testSampleJSONOutput() throws Exception
	{
		Car car = vehicles.get(0);
		logger.info("Sample JSON:");
		logger.info(SpearHelpers.toJson(car, true));
	}




}
