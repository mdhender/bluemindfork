package net.bluemind.eas.endpoint.tests;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.bluemind.eas.command.provision.ProvisionEndpoint;
import net.bluemind.eas.dto.provision.ProvisionRequest.RemoteWipe;
import net.bluemind.eas.dto.provision.ProvisionResponse;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class ProvisionEndpointTests extends AbstractEndpointTest {

	/**
	 * <pre>
	 * <?xml version="1.0" encoding="utf-8"?>
	 * <Provision xmlns="Provision:" xmlns:settings="Settings:">
	 *  <settings:DeviceInformation>
	 *     <settings:Set>
	 *         <settings:Model>...</settings:Model>
	 *         <settings:IMEI>...</settings:IMEI>
	 *         <settings:FriendlyName>...</settings:FriendlyName>
	 *         <settings:OS>...</settings:OS>
	 *         <settings:OSLanguage>...</settings:OSLanguage>
	 *         <settings:PhoneNumber>...</settings:PhoneNumber>
	 *         <settings:MobileOperator>...</settings:MobileOperator>
	 *         <settings:UserAgent>...</settings:UserAgent>
	 *     </settings:Set>
	 *  </settings:DeviceInformation>
	 *  <Policies>
	 *       <Policy>
	 *            <PolicyType>MS-EAS-Provisioning-WBXML</PolicyType> 
	 *       </Policy>
	 *  </Policies>
	 * </Provision>
	 * </pre>
	 * 
	 * Phase2:
	 * https://msdn.microsoft.com/en-us/library/ee202231(v=exchg.80).aspx
	 * <p>
	 * Client Downloads Policy from Server
	 * </p>
	 * 
	 * @throws TransformerException
	 * @throws IOException
	 */
	public void testProvisionPhase2() throws TransformerException, IOException {
		Document document = createPhase2Document();
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Document phase2Xml = WBXMLTools.toXml(response.content.getBytes());
		String pKeyString = DOMUtils.getUniqueElement(phase2Xml.getDocumentElement(), "PolicyKey").getTextContent();
		assertNotNull(pKeyString);
	}

	public void testProvisionPhase3() throws TransformerException, IOException {
		Document document = createPhase2Document();
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Document phase2Xml = WBXMLTools.toXml(response.content.getBytes());
		String tmpPolicyKey = DOMUtils.getUniqueElement(phase2Xml.getDocumentElement(), "PolicyKey").getTextContent();
		long pKey = Long.parseLong(tmpPolicyKey);
		assertTrue(pKey > 0);

		Document docP3 = createPhase3Document(tmpPolicyKey);
		ResponseObject phase3Response = runEndpoint(docP3);
		assertEquals("Status should be 200", 200, phase3Response.getStatusCode());
		Document phase3Xml = WBXMLTools.toXml(phase3Response.content.getBytes());
		DOMUtils.logDom(phase3Xml);
		String finalPolicyKey = DOMUtils.getUniqueElement(phase3Xml.getDocumentElement(), "PolicyKey").getTextContent();
		assertFalse(tmpPolicyKey.equals(finalPolicyKey));
	}

	public void testProvisionWrongPolicyKey() throws TransformerException, IOException {
		Document document = createPhase3Document("9999999");
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Document respDoc = WBXMLTools.toXml(response.content.getBytes());
		DOMUtils.logDom(respDoc);
		Element policy = DOMUtils.getUniqueElement(respDoc.getDocumentElement(), "Policy");
		assertNotNull(policy);
		assertEquals(ProvisionResponse.Policies.Policy.Status.Success.xmlValue(),
				DOMUtils.getUniqueElement(policy, "Status").getTextContent());
		Element pk = DOMUtils.getUniqueElement(respDoc.getDocumentElement(), "PolicyKey");
		assertNotNull(pk);

		Element data = DOMUtils.getUniqueElement(respDoc.getDocumentElement(), "Data");
		assertNotNull(data);

		Element provDoc = DOMUtils.getUniqueElement(data, "EASProvisionDoc");
		assertNotNull(provDoc);
	}

	public void testAckRemoteWipe() throws Exception {
		Document document = DOMUtils.createDoc("Provision", "Provision");
		Element remoteWipe = DOMUtils.createElement(document.getDocumentElement(), "RemoteWipe");
		DOMUtils.createElementAndText(remoteWipe, "Status", RemoteWipe.Status.Success.xmlValue());
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
	}

	private Document createPhase2Document() throws TransformerException {
		Document document = DOMUtils.createDoc("Provision", "Provision");
		Element root = document.getDocumentElement();
		Element devInf = DOMUtils.createElement(root, "Settings:DeviceInformation");
		Element set = DOMUtils.createElement(devInf, "Settings:Set");
		DOMUtils.createElementAndText(set, "Settings:IMEI", "123456789" + System.currentTimeMillis());
		Element policies = DOMUtils.createElement(root, "Policies");
		Element policy = DOMUtils.createElement(policies, "Policy");
		DOMUtils.createElementAndText(policy, "PolicyType", "MS-EAS-Provisioning-WBXML");
		return document;
	}

	private Document createPhase3Document(String pKey) throws TransformerException {
		Document document = DOMUtils.createDoc("Provision", "Provision");
		Element root = document.getDocumentElement();
		Element policies = DOMUtils.createElement(root, "Policies");
		Element policy = DOMUtils.createElement(policies, "Policy");
		DOMUtils.createElementAndText(policy, "PolicyType", "MS-EAS-Provisioning-WBXML");
		DOMUtils.createElementAndText(policy, "PolicyKey", pKey);
		DOMUtils.createElementAndText(policy, "Status", "1");
		return document;
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new ProvisionEndpoint();
	}

}
