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

	@Test
	public void testXMLObjectRoundTrips() throws Exception
	{
		Car c1 = vehicles.get(0);
		CarRootElement c1_root = CarRootElement.Create().setCarElement(c1).build();

		Assert.assertNotNull(c1);
		Assert.assertNotNull(c1_root);

		io.terahelix.schemas.xsd_test.CarRootElement jaxb_car = xmlMapper.convertToJAXBObject(c1_root);

		Assert.assertNotNull(jaxb_car);

		//Or go straight to the XML.
		String jaxbXml = xmlMapper.convertToJAXBXML(jaxb_car);

		logger.info("JAXB XML :: \n " + jaxbXml);

		CarRootElement c1_from_xml  = xmlMapper.convertToSpearFromXML(new StringReader(jaxbXml));

		Assert.assertFalse("The object should not be different instances",c1_root == c1_from_xml);

		logger.info("c1_root :: \n " + SpearHelpers.toJson(c1_root, true));
		logger.info("c1_from_xml :: \n " + SpearHelpers.toJson(c1_from_xml, true));

		Assert.assertTrue("The object should be equal ",c1_root.equals(c1_from_xml));

	}


}
