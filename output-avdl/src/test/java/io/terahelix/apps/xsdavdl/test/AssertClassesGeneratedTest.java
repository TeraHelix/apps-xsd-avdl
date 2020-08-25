package io.terahelix.apps.xsdavdl.test;

import org.junit.Assert;
import org.junit.Test;

/**
 * Make sure that we have compiled and generated the classes
 */
public class AssertClassesGeneratedTest
{
	@Test
	public void testGenExists() throws Exception
	{
		Assert.assertNotNull(io.terahelix.apps.xsdavdl.VehicleContainer.class); //Jaxb
		Assert.assertNotNull(XSD.AVDL.VehicleContainer.class); //AVRO
		Assert.assertNotNull(Generated.VehicleContainer.class); //Spear
		//Note: Package Names can be tweaked in configurations.
	}


}
