package io.terahelix.spear.xsd.xjc;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is responsible inspecting a schema and
 * associated XJB file and enrich the code generation such that for enumerations
 * it also includes the descriptions per element.
 */
public class XJBEnumCodeGenEnricher
{

	public static final String CODE_GEN_TEMPLATE =

	"	private static final java.util.Map<##ENUM_NAME##, String> descriptions;\n" +
	"	static\n" +
	"	{\n" +
	"		java.util.Map<##ENUM_NAME##, String> val = new java.util.concurrent.ConcurrentHashMap<>();\n" +
	"##PER_ENUM_CODE_LINE## " +
	"		descriptions = java.util.Collections.unmodifiableMap(val);\n" +
	"   }\n" + "\n" +
	"   @Override\n" +
	"   public String toString()\n" +
	"	{\n" +
	"		return this.value();\n" +
	"	}\n" +
	"\n" +
	"	public String toDescription()\n" +
	"   {\n" +
	" 		return descriptions.get(this);\n" +
	"   }\n" +
	"\n" +
	"	public java.util.Map<##ENUM_NAME##, String> getAllDescriptions()\n" +
	"	{\n" +
	"		return descriptions;\n" +
	" 	}";

	public static final String PER_ENUM_CODE_LINE = "		val.put(##ENUM_NAME##.##ENUM_VALUE##, \"##ENUM_DESCRIPTION##\");\n";


	public static void main(String... args) throws Exception
	{
		if(args.length != 2)
		{
			System.err.println( "You need to specify 2 parameters to the class : \n" +
							    "[0] - The input xjb file you want to process \n" +
								"[1] - Where to write the output of the xjb file you have processed\n" );
			System.exit(1);
			return;
		}

		XJBEnumCodeGenEnricher enricher = new XJBEnumCodeGenEnricher(args[0], args[1]);
		enricher.performEnrichment();
		enricher.writeOutDocument();
	}

	private static final DocumentBuilderFactory docBuilderFactory;
	private static final DocumentBuilder docBuilder;
	private static final TransformerFactory transFactory;

	static
	{
		try
		{
			docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilder = docBuilderFactory.newDocumentBuilder();
			transFactory = TransformerFactory.newInstance();
		}
		catch (ParserConfigurationException e)
		{
			throw new RuntimeException("Could not create the Document Factories : " + e);
		}
	}

	private final String xjbInputFileStr;
	private final String xjbOutputFileStr;

	private final Path xjbInputFile;
	private final Path xjbOutputFile;

	private final Document xjbInput;

	private final String jaxbNamespacePrefix;
	private final String codeGenNamespacePrefix;

	private final List<Element> schemaBindingElements;

	public XJBEnumCodeGenEnricher(String xjbInputFileP, String xjbOutputFileP)
	{
		this.xjbInputFileStr = xjbInputFileP;
		this.xjbOutputFileStr = xjbOutputFileP;

		xjbInputFile = Paths.get(xjbInputFileStr);
		xjbOutputFile = Paths.get(xjbOutputFileStr);

		if(Files.exists(xjbInputFile) == false)
		{
			throw new IllegalArgumentException("The input path you specified does not exist - " + xjbInputFile);
		}

		try(InputStream ins = Files.newInputStream(xjbInputFile))
		{
			xjbInput = docBuilder.parse(ins);
			jaxbNamespacePrefix = extractNamespacePrefix(xjbInput, "http://java.sun.com/xml/ns/jaxb");
			codeGenNamespacePrefix = extractNamespacePrefix(xjbInput,"http://jaxb.dev.java.net/plugin/code-injector");
			schemaBindingElements = getSchemaBindingElements();

			System.out.println("Found a total of : " + schemaBindingElements.size() + " schema location bindings - will instrument these");

		}
		catch(Exception e)
		{
			throw new RuntimeException("Unable to read / parse : " + xjbInputFile + " - got back : " + e, e);
		}
	}

	public void performEnrichment()
	{
		for (int i = 0; i < schemaBindingElements.size(); i++)
		{
			final Element bindingElement = schemaBindingElements.get(i);
			final String schemaLocation = bindingElement.getAttribute("schemaLocation");
			final Path schemaInput = xjbInputFile.getParent().resolve(schemaLocation);

			final List<EnumDescriptor> enumDescriptors = getSchemaEnumDescriptors(schemaInput, schemaLocation);

			System.out.println("Processing - " + enumDescriptors.size() + " enum descriptors for : " + schemaLocation);

			addEnumDescriptorsToSchemaBindingElement(bindingElement, enumDescriptors);
		}

	}


	public void writeOutDocument()
	{
		try
		{
			if (Files.exists(xjbOutputFile.getParent()) == false)
			{
				Files.createDirectories(xjbOutputFile.getParent());
			}
		}
		catch(Exception e)
		{
			throw new RuntimeException("Could not create the parent directory for : " + xjbOutputFile.toAbsolutePath() + " - " + e);
		}

		try(BufferedWriter out = Files.newBufferedWriter(xjbOutputFile))
		{
			Transformer transformer = transFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			DOMSource source = new DOMSource(xjbInput);
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);

			System.out.println("Wrote output to : " + xjbOutputFile);
		}
		catch(Exception e)
		{
			String msg = "Unable to write the document to : " + xjbOutputFile + " - got the exception : " + e;
			throw new RuntimeException(msg, e);
		}

	}

	private void addEnumDescriptorsToSchemaBindingElement(Element bindingElement,
														  List<EnumDescriptor> enumDescriptors)
	{
		for(EnumDescriptor enumDescr : enumDescriptors)
		{
			final Element nodeElement = xjbInput.createElement(jaxbNamespacePrefix + ":bindings");
			nodeElement.setAttribute("node", "//xs:simpleType[@name='" + enumDescr.name + "']");
			nodeElement.setAttribute("multiple", "true");
			nodeElement.setAttribute("required", "false");

			final Element codeGenElement = xjbInput.createElement(codeGenNamespacePrefix + ":code");

			StringBuilder perElementLines = new StringBuilder();
			for(EnumItem item : enumDescr.items)
			{
				perElementLines.append(PER_ENUM_CODE_LINE.replace("##ENUM_NAME##", enumDescr.name)
														 .replace("##ENUM_VALUE##", item.value)
														 .replace("##ENUM_DESCRIPTION##", item.escapedJava));
			}

			String code = CODE_GEN_TEMPLATE.replace("##ENUM_NAME##", enumDescr.name)
									       .replace("##PER_ENUM_CODE_LINE##", perElementLines.toString());

			CDATASection codeCData = xjbInput.createCDATASection(code);

			codeGenElement.appendChild(codeCData);

			nodeElement.appendChild(codeGenElement);

			bindingElement.appendChild(nodeElement);
		}

	}


	private List<EnumDescriptor> getSchemaEnumDescriptors(final Path schemaInput, final String schemaLocation)
	{
		try(InputStream schemaInputStream = Files.newInputStream(schemaInput))
		{
			final Document schema = docBuilder.parse(schemaInputStream);
			final String tagName = schema.getDocumentElement().getTagName();

			if(tagName.endsWith(":schema") == false)
			{
				throw new RuntimeException("The root element of your schema - " + schemaInput + " - is not <...:schema> - hence this is not a valid file.");

			}

			final String xsdPrefix = tagName.substring(0, tagName.indexOf(":"));
			final NodeList allSimpleTypes = schema.getElementsByTagName(xsdPrefix + ":simpleType" );

			final List<EnumDescriptor> result = new ArrayList<>();

			for (int i = 0; i < allSimpleTypes.getLength(); i++)
			{
				Element simpleType = (Element)  allSimpleTypes.item(i);
				//See if it is an enum restriction.
				NodeList enumRestrictions = simpleType.getElementsByTagName(xsdPrefix + ":enumeration");

				if(enumRestrictions.getLength() == 0) continue;

				List<EnumItem> enumItems = new ArrayList<>();

				for (int j = 0; j< enumRestrictions.getLength(); j++)
				{
					Element enumElement = (Element)enumRestrictions.item(j);
					String enumValue = enumElement.getAttribute("value");
					String docAnnotation = extractDocumentAnotation(enumElement);

					enumItems.add(new EnumItem(enumValue, docAnnotation));
				}

				result.add(new EnumDescriptor(simpleType.getAttribute("name"),
											  extractDocumentAnotation(simpleType),
											  enumItems));
			}

			return result;
		}
		catch(Exception e)
		{
			String msg = "Could not read the schema location pointer - " + schemaLocation + " - it resolved to file : " + schemaInput.toAbsolutePath() + " - but I got back : " + e;
			throw new RuntimeException(msg, e);
		}

	}

	private String extractDocumentAnotation(Element enumElement)
	{
		NodeList childNodes = enumElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++)
		{
			if(childNodes.item(i) instanceof Element == false) continue;
			final Element childEl = (Element) childNodes.item(i);

			if(childEl.getTagName().endsWith(":annotation"))
			{
				// ok - now get its documentation !
				NodeList grandChildren = childEl.getChildNodes();
				for (int j = 0; j < grandChildren.getLength(); j++)
				{
					if(grandChildren.item(j) instanceof Element == false) continue;

					Element grandChildEl = (Element)grandChildren.item(j);
					if(grandChildEl.getTagName().endsWith(":documentation"))
					{
						return grandChildEl.getTextContent();
					}
				}
			}
		}
		return "";
	}



	private List<Element> getSchemaBindingElements()
	{
		final String jaxbBindingsSearch = jaxbNamespacePrefix + ":bindings";
		final NodeList allBindingsElements = xjbInput.getElementsByTagName(jaxbBindingsSearch);

		final List<Element> result = new ArrayList<>();

		for (int i = 0; i < allBindingsElements.getLength(); i++)
		{
			Element candidate = (Element)allBindingsElements.item(i);
			if(candidate.hasAttribute("schemaLocation"))
			{
				result.add(candidate);
			}
		}
		return Collections.unmodifiableList(result);
	}



	private static String extractNamespacePrefix(Document doc, String fullNamespace)
	{
		Element rootEl = doc.getDocumentElement();
		NamedNodeMap allAttribues = rootEl.getAttributes();

		StringBuilder scannedItems = new StringBuilder();

		for (int i = 0; i < allAttribues.getLength(); i++)
		{
			Attr item = (Attr)allAttribues.item(i);
			scannedItems.append(item.getName() + " - " + item.getLocalName() + " - " + item.getValue() + "\n");
			if(item.getValue().equals(fullNamespace))
			{
				return item.getName().replace("xmlns:", "");
			}
		}

		throw new IllegalArgumentException("Could not find : " + fullNamespace + " - all items scanned : \n" + scannedItems);
	}

	class EnumDescriptor
	{
		private final String name;
		public final String documentation;
		public final List<EnumItem> items;

		public EnumDescriptor(String name, String documentation, List<EnumItem> itemsP)
		{
			this.documentation = documentation;
			this.name = name;
			this.items = Collections.unmodifiableList(new CopyOnWriteArrayList<>(itemsP));
		}
	}

	class EnumItem
	{
		public final String value;
		public final String documenation;
		public final String escapedJava;

		public EnumItem(String value, String documenationP)
		{
			this.value = value;
			this.documenation = documenationP;

			String espd = documenation;
			if(espd.contains("\n"))
			{
				espd = espd.replace("\n", "\\n");
			}
			if(espd.contains("\t"))
			{
				espd = espd.replace("\t", "\\t");
			}
			if(espd.contains("\""))
			{
				espd = espd.replace("\"", "\\\\");
			}

			escapedJava = espd;

		}

	}


}
