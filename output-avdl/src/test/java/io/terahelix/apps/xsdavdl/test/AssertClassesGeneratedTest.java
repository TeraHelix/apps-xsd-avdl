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
		Assert.assertNotNull(io.terahelix.schemas.xsd_test.VehicleContainer.class); //Jaxb
		Assert.assertNotNull(Generated.AVRO.VehicleContainer.class); //AVRO
		Assert.assertNotNull(Generated.VehicleContainer.class); //Spear
		//Note: Package Names can be tweaked in configurations.
	}


}
