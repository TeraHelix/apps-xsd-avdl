# TeraHelix Apps XSD-AVDL Project

This template project demonstrates how TeraHelix Spear can be used to generate Avro / AVDL bindings from a set of input XML Schema (XSD) inputs. 

![XSD-AVDL Build - Windows - Java 11](https://github.com/TeraHelix/apps-xsd-avdl/workflows/XSD-AVDL%20Build%20-%20Windows%20-%20Java%2011/badge.svg)
![XSD-AVDL Build - Linux - Java 11](https://github.com/TeraHelix/apps-xsd-avdl/workflows/XSD-AVDL%20Build%20-%20Linux%20-%20Java%2011/badge.svg)

![XSD-AVDL Build - Windows - Java 8](https://github.com/TeraHelix/apps-xsd-avdl/workflows/XSD-AVDL%20Build%20-%20Windows%20-%20Java%208/badge.svg)
![XSD-AVDL Build - Linux - Java 8](https://github.com/TeraHelix/apps-xsd-avdl/workflows/XSD-AVDL%20Build%20-%20Linux%20-%20Java%208/badge.svg)

## Setting up the Build

To build and run the build locally, please refer to our [README-Building.md guide](README-Building.md)
  
## Inputs and Outputs

Invoking the standard Maven build command (`mvn clean install`) will:

* Read XSD Inputs from the [input-xsd/src/main/resources/XSD](input-xsd/src/main/resources/XSD) directory.
* Produce *AVDL* Outputs to the [output-avdl/target/generated-spear-avdl-sources/avdl/.spear-avdl-source](output-avdl/target/generated-spear-avdl-sources/avdl/.spear-avdl-source) directory.

You will also note that additional outputs (Python, Typescript, Java) will also be produced in the [output-avdl/target](output-avdl/target) directory. The compiled Java classes (both generated from Spear itself or from AVDL) will be produced in the `output-avdl-2.1-SNAPSHOT.jar` file.

## Key Variables

If you wish to substitute the example with your own XSD file, you need to be aware of the following variables defined in the root [pom.xml](pom.xml) 

```xml
<properties>
  <!-- If you want to supplement your model with additional items, this is a default import -->
  <xsd.convert.defaultNamespaceImport>IO.TeraHelix</xsd.convert.defaultNamespaceImport>

  <!-- This class is called to generate Documentation. Override if you have your own-->
  <xsd.convert.docAndSourceLinksProviderClass>io.terahelix.xsd.spear.DefaultDocAndSourceLinksProvider</xsd.convert.docAndSourceLinksProviderClass>

  <!-- Change these items below to map / replace your XSD schema / package
       names as appropriate -->
   <xsd.convert.sourceStartsWith>io.terahelix.schemas.xsd_test</xsd.convert.sourceStartsWith>
   <xsd.convert.targetPackage>IO.TeraHelix.XSD_Test</xsd.convert.targetPackage>
   <xsd.convert.targetDirectory>XSD_Test</xsd.convert.targetDirectory>
   <xsd.convert.targetNamespace>http://schemas.terahelix.io/XSD-Test</xsd.convert.targetNamespace>
</properties>

```

## The Generated AVDL / JAXB Bridge Library

An auto-generated library that converts AVDL object to JAXB objects and vice versa can be found in the [output-avdl-fluent-lib](output-avdl-fluent-lib) directory. It has the following key features:

* All method invocations and object constructions are generated and does not use reflection. This ensures the conversion code is performant
* There are no dependencies on Spear libraries. This make it easier to package and ship this code as part of a large application.
* Utility functions are provided as part of the `AVDLConversionService` to inspect AVDL to JAXB class mappings.



 


