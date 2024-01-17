package net.bluemind.eas.endpoint.tests;

import java.util.concurrent.TimeUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.buffer.Buffer;
import net.bluemind.eas.command.folder.sync.FolderSyncEndpoint;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.mock.RequestObject;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.validation.Validator;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.lib.vertx.VertxPlatform;

public class FolderSyncEndpointTests extends AbstractEndpointTest {

	/**
	 * Checks that even if the request is clearly broken, we get at least an error
	 * response.
	 * 
	 * This one should be rejected by {@link WbxmlHandlerBase} as the wbxml fails to
	 * parse.
	 */
	public void testEmptyDocument() {
		ImmutableMap<String, String> headers = ImmutableMap.of("titi", "tata");
		ImmutableMap<String, String> queryParams = ImmutableMap.of("john", "bang");
		AuthorizedDeviceQuery dq = reqFactory.authorized(VertxPlatform.getVertx(), devId, devType, headers, queryParams,
				device.uid);
		ResponseObject response = (ResponseObject) dq.request().response();
		try {
			endpoint.handle(dq);
			RequestObject theRequest = (RequestObject) dq.request();
			theRequest.trigger(new byte[0]);
		} catch (Throwable t) {
			t.printStackTrace();
			fail("endpoint.handle must not throw");
		}
		Buffer buf = response.waitForIt(5, TimeUnit.SECONDS);
		assertNotNull(buf);
		assertEquals("Status should be 400 (Bad request)", 400, response.getStatusCode());
	}

	/**
	 * This one should be rejected by schema {@link Validator}
	 */
	public void testInvalidXmlFolderSync() {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 400 (Bad request)", 400, response.getStatusCode());
	}

	/**
	 * This one should work. As the response is validated by {@link Validator}, we
	 * don't need to check it.
	 */
	public void testInitialFolderSync() {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
	}

	public void testIncrementalFolderSync() throws Exception {
		Document document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = document.getDocumentElement();
		Element sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);
		ResponseObject response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());

		Document syncResult = WBXMLTools.toXml(response.content.getBytes());
		String nextSk = syncResult.getElementsByTagName("SyncKey").item(0).getTextContent();

		document = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		root = document.getDocumentElement();
		sk = document.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent(nextSk);
		root.appendChild(sk);

		response = runEndpoint(document);
		assertEquals("Status should be 200", 200, response.getStatusCode());
		Document incrementalResult = WBXMLTools.toXml(response.content.getBytes());
		assertNotNull(incrementalResult);
	}

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new FolderSyncEndpoint();
	}

}
