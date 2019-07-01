package net.bluemind.eas.endpoint.tests;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.eas.command.settings.SettingsEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class SettingsEndpointTests extends AbstractEndpointTest {

	/**
	 * <pre>
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <Settings xmlns="Settings">
	 *   <Oof>
	 *     <Get>
	 *       <BodyType>TEXT</BodyType>
	 *     </Get>
	 *   </Oof>
	 * </Settings>
	 * </pre>
	 * 
	 * iOS 9 fetches Oof only
	 * 
	 * Ex2k10 response
	 * 
	 * <pre>
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <Settings xmlns="Settings">
	 * <Status>1</Status>
	 * <Oof>
	 *   <Status>1</Status>
	 *   <Get>
	 *     <OofState>0</OofState>
	 *     <StartTime>2015-09-25T08:00:00.000Z</StartTime>
	 *     <EndTime>2015-09-26T08:00:00.000Z</EndTime>
	 *     <OofMessage>
	 *       <AppliesToInternal/>
	 *       <Enabled>1</Enabled>
	 *       <ReplyMessage>﻿&#13;
	 *       inside&#13;
	 *       </ReplyMessage>
	 *       <BodyType>TEXT</BodyType>
	 *     </OofMessage>
	 *     <OofMessage>
	 *       <AppliesToExternalKnown/>
	 *       <Enabled>1</Enabled>
	 *       <ReplyMessage>﻿&#13;
	 *       outside&#13;
	 *       </ReplyMessage>
	 *       <BodyType>TEXT</BodyType>
	 *     </OofMessage>
	 *     <OofMessage>
	 *       <AppliesToExternalUnknown/>
	 *       <Enabled>1</Enabled>
	 *       <ReplyMessage>﻿&#13;
	 *       outside&#13;
	 *       </ReplyMessage>
	 *       <BodyType>TEXT</BodyType>
	 *     </OofMessage>
	 *   </Get>
	 * </Oof>
	 * </Settings>
	 * </pre>
	 * 
	 * @throws TransformerException
	 * @throws IOException
	 */
	public void testOutOfOfficeFetchIOS9() throws TransformerException, IOException {
		Document document = createFetchOof();
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Document getOff = WBXMLTools.toXml(response.content.getBytes());
		DOMUtils.logDom(getOff);
		NodeList children = getOff.getDocumentElement().getChildNodes();
		assertEquals("we should just have Status & Oof as children of Settings", 2, children.getLength());
	}

	private Document createFetchOof() throws TransformerException {
		Document document = DOMUtils.createDoc("Settings", "Settings");
		Element root = document.getDocumentElement();
		Element oof = DOMUtils.createElement(root, "Oof");
		Element get = DOMUtils.createElement(oof, "Get");
		DOMUtils.createElementAndText(get, "BodyType", "TEXT");
		return document;
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SettingsEndpoint();
	}

}
