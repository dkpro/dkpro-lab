/*******************************************************************************
 * Copyright 2011
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas;

import static de.tudarmstadt.ukp.dkpro.lab.Util.close;
import static org.dom4j.DocumentHelper.createDocument;
import static org.dom4j.DocumentHelper.createElement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;

/**
 * Each deployment descriptor describes one service, associated with a single
 * UIMA descriptor (aggregate or primitive), and describes the deployment of
 * those UIMA components that are co-located, together with specifications of
 * connections to those subcomponents that are remote.
 * <li>
 * The deployment descriptor is used to augment information contained in an
 * analysis engine descriptor. It adds information concerning
 * <li>
 * which components are managed using AS
 * <li>
 * queue names for connecting components
 * <li>
 * error thresholds and recovery / terminate action specifications
 * <li>
 * error handling routine specifications
 * <p>
 * The application can include both Java and non-Java components; the deployment
 * descriptors are slightly different for non-Java components.
 *
 * @author Richard Eckart de Castilho
 */
public class AsDeploymentDescription
{
	public static final Namespace NS = Namespace.get("http://uima.apache.org/resourceSpecifier");
	public static final QName E_ROOT = QName.get("analysisEngineDeploymentDescription", NS);
	public static final QName E_NAME = QName.get("name", NS);
	public static final QName E_DESCRIPTION = QName.get("description", NS);
	public static final QName E_VERSION = QName.get("version", NS);
	public static final QName E_VENDOR = QName.get("vendor", NS);
	public static final QName E_DEPLOYMENT = QName.get("deployment", NS);
	public static final QName A_PROTOCOL = QName.get("protocol");
	public static final QName A_PROVIDER = QName.get("provider");
	public static final QName E_CAS_POOL = QName.get("casPool", NS);
	public static final QName A_NUMBER_OF_CASES = QName.get("numberOfCASes");
	public static final QName A_INITIAL_FS_HEAP_SIZE = QName.get("initialFsHeapSize");
	public static final QName E_SERVICE = QName.get("service", NS);
	public static final QName E_INPUT_QUEUE = QName.get("inputQueue", NS);
	public static final QName A_ENDPOINT = QName.get("endpoint");
	public static final QName A_BROKER_URL = QName.get("brokerURL");
	public static final QName A_PREFETCH = QName.get("prefetch");
	public static final QName E_TOP_DESCRIPTOR = QName.get("topDescriptor", NS);
	public static final QName E_IMPORT = QName.get("import", NS);
	public static final QName A_LOCATION = QName.get("location");

	public static final QName E_ANALYSIS_ENGINE = QName.get("analysisEngine", NS);
	public static final QName A_KEY = QName.get("key");
	public static final QName A_ASYNC = QName.get("async");
	public static final QName E_SCALEOUT = QName.get("scaleout", NS);
	public static final QName A_NUMBER_OF_INSTANCES = QName.get("numberOfInstances");
	public static final QName E_ASYNC_PRIMITIVE_ERROR_CONFIGURATION = QName.get("asyncPrimitiveErrorConfiguration", NS);
	public static final QName E_PROCESS_CAS_ERRORS = QName.get("processCasErrors", NS);
	public static final QName A_THRESHOLD_COUNT = QName.get("thresholdCount");
	public static final QName A_THRESHOLD_WINDOW = QName.get("thresholdWindow");
	public static final QName A_THRESHOLD_ACTION = QName.get("thresholdAction");
	public static final QName E_COLLECTION_PROCESS_COMPLETE_ERRORS = QName.get("collectionProcessCompleteErrors", NS);
	public static final QName A_TIMEOUT = QName.get("timeout");
	public static final QName A_ADDITIONAL_ERROR_ACTION = QName.get("additionalErrorAction");
	public static final QName E_DELEGATES = QName.get("delegates", NS);

	public static final String PROTOCOL_JMS = "jms";
	public static final String PROVIDER_ACTIVEMQ = "activemq";

	private String name;
	private String description;
	private String version;
	private String vendor;
	private String protocol = PROTOCOL_JMS;
	private String provider = PROVIDER_ACTIVEMQ;

	/**
	 * This element specifies information for managing CAS pools. Having more
	 * CASes in the pools enables more AS components to run at the same time.
	 * For instance, if your application had four components, but one was slow,
	 * you might deploy 10 instances of the slow component. To get all 10
	 * instances working on CASes simultaneously, your CAS pool should be at
	 * least 10 CASes. The casPool size should be small enough to avoid paging.
	 * <p>
	 * The initialFsHeapSize attribute is optional, and allows setting the size
	 * of the initial CAS Feature Structure heap. This number is specified in
	 * bytes, and the default is approximately 2 megabytes for Java top-level
	 * services, and 40 kilobytes for C++ top level services. The heap grows as
	 * needed; this parameter is useful for those cases where the expected heap
	 * size is much smaller than the default.
	 */
	int numberOfCases = 2;
	int initialFsHeapSize = 2000000;

	/**
	 * The inputQueue element is required. It identifies the input queue for the
	 * service.
	 *
	 * <pre>
	 * &lt;inputQueue brokerURL="tcp://x.y.z:portnumber"
	 *    endpoint="queue_name" prefetch="1"/>
	 * </pre>
	 *
	 * The queue broker address includes a protocol specification, which should
	 * be set to either "tcp", or "http". The brokerURL attribute specifies the
	 * queue broker URL, typically its network address and port.
	 * <p>
	 * The http protocol is similar to the tcp protocol, but is preferred for
	 * wide-area-network connections where there may be firewall issues, as it
	 * supports http tunnelling.
	 */
	private String endpoint;
	private String brokerUrl;
	private String prefetch = "1";

	/**
	 * Each service must indicate some analysis engine to run, using this
	 * element.
	 *
	 * <pre>
	 * &lt;topDescriptor>
	 *   &lt;import location="..." /> <!-- or name="..." -->
	 * &lt;/topDescriptor>
	 * </pre>
	 *
	 * This is the standard UIMA import element. Imports can be by name or by
	 * location; see Section 2.2, “Imports” in UIMA References.
	 */
	private AnalysisEngineDescription topDescriptor;
	private File topDescriptorFile;

	public AsDeploymentDescription(AnalysisEngineDescription aTopDescriptor, String aEndpoint, String aBrokerUrl)
	{
		setName(aTopDescriptor.getMetaData().getName());
		setDescription(aTopDescriptor.getMetaData().getDescription());
		setVersion(aTopDescriptor.getMetaData().getVersion());
		setVendor(aTopDescriptor.getMetaData().getVendor());
		setTopDescriptor(aTopDescriptor);
		setEndpoint(aEndpoint);
		setBrokerUrl(aBrokerUrl);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String aName)
	{
		name = aName;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String aDescription)
	{
		description = aDescription;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String aVersion)
	{
		version = aVersion;
	}

	public String getVendor()
	{
		return vendor;
	}

	public void setVendor(String aVendor)
	{
		vendor = aVendor;
	}

	public String getProtocol()
	{
		return protocol;
	}

	public void setProtocol(String aProtocol)
	{
		protocol = aProtocol;
	}

	public String getProvider()
	{
		return provider;
	}

	public void setProvider(String aProvider)
	{
		provider = aProvider;
	}

	public int getNumberOfCases()
	{
		return numberOfCases;
	}

	public void setNumberOfCases(int aNumberOfCases)
	{
		numberOfCases = aNumberOfCases;
	}

	public int getInitialFsHeapSize()
	{
		return initialFsHeapSize;
	}

	public void setInitialFsHeapSize(int aInitialFsHeapSize)
	{
		initialFsHeapSize = aInitialFsHeapSize;
	}

	public String getEndpoint()
	{
		return endpoint;
	}

	public void setEndpoint(String aEndpoint)
	{
		endpoint = aEndpoint;
	}

	public String getBrokerUrl()
	{
		return brokerUrl;
	}

	public void setBrokerUrl(String aBrokerUrl)
	{
		brokerUrl = aBrokerUrl;
	}

	public String getPrefetch()
	{
		return prefetch;
	}

	public void setPrefetch(String aPrefetch)
	{
		prefetch = aPrefetch;
	}

	public AnalysisEngineDescription getTopDescriptor()
	{
		return topDescriptor;
	}

	public void setTopDescriptor(AnalysisEngineDescription aTopDescriptor)
	{
		topDescriptor = aTopDescriptor;
	}

	public void toXML(File aOutput) throws IOException
	{
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(aOutput);
			toXML(os);
		}
		finally {
			close(os);
		}
	}

	public void toXML(OutputStream aOutput) throws IOException
	{
		Element root = createElement(E_ROOT);
		if (getName() != null) {
			root.addElement(E_NAME).setText(getName());
		}
		if (getDescription() != null) {
			root.addElement(E_DESCRIPTION).setText(getDescription());
		}
		if (getVendor() != null) {
			root.addElement(E_VENDOR).setText(getVendor());
		}
		if (getVersion() != null) {
			root.addElement(E_VERSION).setText(getVersion());
		}

		Element deployment = root.addElement(E_DEPLOYMENT)
			.addAttribute(A_PROTOCOL, getProtocol())
			.addAttribute(A_PROVIDER, getProvider());

		Element service = deployment.addElement(E_SERVICE);
		service.addElement(E_INPUT_QUEUE)
			.addAttribute(A_ENDPOINT,getEndpoint())
			.addAttribute(A_BROKER_URL, getBrokerUrl())
			.addAttribute(A_PREFETCH, getPrefetch());
		service.addElement(E_TOP_DESCRIPTOR).addElement(E_IMPORT).addAttribute(
				A_LOCATION, getTopDescriptorFile().getAbsolutePath());

		OutputFormat outformat = OutputFormat.createPrettyPrint();
		outformat.setEncoding("UTF-8");
		XMLWriter writer = new XMLWriter(aOutput, outformat);
		writer.write(createDocument(root));
		writer.flush();
	}

	private File getTopDescriptorFile() throws IOException
	{
		if (topDescriptorFile == null) {
			FileOutputStream os = null;
			try {
				topDescriptorFile = File.createTempFile(getClass()
						.getSimpleName(), ".xml");
				topDescriptorFile.deleteOnExit();
				os = new FileOutputStream(topDescriptorFile);
				getTopDescriptor().toXML(os);
			}
			catch (SAXException e) {
				throw new IOException(e);
			}
			finally {
				close(os);
			}
		}
		return topDescriptorFile;
	}
}
