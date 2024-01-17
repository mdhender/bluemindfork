package net.bluemind.eas.client.tests;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import junit.framework.TestCase;
import net.bluemind.eas.client.Add;
import net.bluemind.eas.client.Collection;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.client.SyncResponse;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class Proto14Ex2k10Tests extends TestCase {

	public void testOptions() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "iPad", "Apple-iPad3C1/1208.321",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();

	}

	public void testFolderSyncZero() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "iPad", "Apple-iPad3C1/1208.321",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();
		Document folderSync = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = folderSync.getDocumentElement();
		Element sk = folderSync.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);

		Document returned = op.postXml("FolderHierarchy", folderSync, "FolderSync");
		DOMUtils.logDom(returned);
	}

	public void testFolderBrokenProvKey() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "iPad", "Apple-iPad3C1/1208.321",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();
		Document folderSync = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = folderSync.getDocumentElement();
		Element sk = folderSync.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);

		Document returned = op.postXml("FolderHierarchy", folderSync, "FolderSync",
				ImmutableMap.of("X-Ms-PolicyKey", "123456"));
		DOMUtils.logDom(returned);
	}

	public void testItemOperationsFetchFileRef() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "iPad", "Apple-iPad3C1/1208.321",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();
		Document doc = open("proto14/fetch_fileref_multipart_req.xml");
		Document returned = op.postXml("ItemOperations", doc, "ItemOperations");
		DOMUtils.logDom(returned);
	}

	public void testItemOperationsFetchFileRefMultipart() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "iPad", "Apple-iPad3C1/1208.321",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();
		Document doc = open("proto14/fetch_fileref_multipart_req.xml");
		byte[] multipartStuff = op.post("ItemOperations", doc, "ItemOperations", null, true);
		ByteBuf buf = Unpooled.wrappedBuffer(multipartStuff).order(ByteOrder.LITTLE_ENDIAN);
		String hex = ByteBufUtil.hexDump(buf);
		System.out.println("hexDump: " + hex);
		int partCount = buf.readInt();
		System.out.println("partCount: " + partCount);
		List<Integer> length = new LinkedList<>();
		for (int i = 0; i < partCount; i++) {
			int offset = buf.readInt();
			int len = buf.readInt();
			length.add(len);
			System.out.println("Found part meta with offset " + offset + " and length: " + len);
		}
		List<byte[]> reRead = new ArrayList<>(partCount);
		for (int l : length) {
			byte[] dest = new byte[l];
			buf.readBytes(dest);
			reRead.add(dest);
		}
		Document wbxmlPart = WBXMLTools.toXml(reRead.get(0));
		DOMUtils.logDom(wbxmlPart);
		byte[] attach = reRead.get(1);
		checkImage(attach);
	}

	private void checkImage(byte[] attach) throws IOException {
		System.setProperty("java.awt.headless", "true");
		BufferedImage theImage = ImageIO.read(new ByteArrayInputStream(attach));
		assertNotNull(theImage);
		System.out.println("theJpeg: " + theImage);
	}

	public void testSyncInbox() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "iPad", "Apple-iPad3C1/1208.321",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();
		Document folderSync = DOMUtils.createDoc("FolderHierarchy", "FolderSync");
		Element root = folderSync.getDocumentElement();
		Element sk = folderSync.createElementNS("FolderHierarchy", "SyncKey");
		sk.setTextContent("0");
		root.appendChild(sk);

		Document returned = op.postXml("FolderHierarchy", folderSync, "FolderSync");
		DOMUtils.logDom(returned);
		NodeList adds = returned.getDocumentElement().getElementsByTagName("Add");
		Integer inboxServerId = null;
		for (int i = 0; i < adds.getLength(); i++) {
			Element add = (Element) adds.item(i);
			if ("2".equals(DOMUtils.getElementText(add, "Type"))) {
				inboxServerId = Integer.parseInt(DOMUtils.getElementText(add, "ServerId"));
				break;
			}
		}
		assertNotNull(inboxServerId);
		Document firstSync = DOMUtils.createDoc("AirSync", "Sync");
		Element syncRoot = firstSync.getDocumentElement();
		Element collections = DOMUtils.createElement(syncRoot, "Collections");
		Element collection = DOMUtils.createElement(collections, "Collection");
		DOMUtils.createElementAndText(collection, "SyncKey", "0");
		DOMUtils.createElementAndText(collection, "CollectionId", Integer.toString(inboxServerId));
		DOMUtils.createElementAndText(collection, "GetChanges", "0");
		SyncResponse sr = op.sync(firstSync);
		assertNotNull(sr);
		DOMUtils.logDom(sr.dom);
		for (String colId : sr.getCollections().keySet()) {
			Collection col = sr.getCollection(colId);
			System.out.println(" * " + colId + " => " + col.getSyncKey());
		}
		boolean done = false;
		int total = 0;
		while (!done) {
			Document sync = DOMUtils.createDoc("AirSync", "Sync");
			Element sRoot = sync.getDocumentElement();
			Element cols = DOMUtils.createElement(sRoot, "Collections");
			for (String colId : sr.getCollections().keySet()) {
				Element c = DOMUtils.createElement(cols, "Collection");
				Collection col = sr.getCollection(colId);
				DOMUtils.createElementAndText(c, "SyncKey", col.getSyncKey());
				DOMUtils.createElementAndText(c, "CollectionId", colId);
				System.out.println(" Collection: " + colId + ", SyncKey: " + col.getSyncKey() + ", MoreAvail: "
						+ col.hasMoreAvailable());
				Element options = DOMUtils.createElement(c, "Options");
				DOMUtils.createElementAndText(options, "Class", "Email");
				DOMUtils.createElementAndText(options, "FilterType", "0");
				DOMUtils.createElementAndText(collection, "GetChanges", "0");

				// DOMUtils.createElementAndText(options, "MIMESupport", "2");
				// DOMUtils.createElementAndText(options, "MIMETruncation",
				// "8");
				Element bp = DOMUtils.createElement(options, "AirSyncBase:BodyPreference");
				DOMUtils.createElementAndText(bp, "Type", "1");
				DOMUtils.createElementAndText(bp, "TruncationSize", "32");
			}
			long time = System.currentTimeMillis();
			sr = op.sync(sync);
			time = System.currentTimeMillis() - time;
			for (String colId : sr.getCollections().keySet()) {
				Collection col = sr.getCollection(colId);
				if (!col.hasMoreAvailable()) {
					done = true;
					System.out.println("Done");
				}
				List<Add> serverAdds = col.getAdds();
				total += serverAdds.size();
				System.out.println("Received " + serverAdds.size() + " adds in " + time + "ms.");
			}

		}
		System.out.println("Received " + total + " emails.");
		DOMUtils.logDom(sr.dom);
	}

	public void testGetOof() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "iPad", "Apple-iPad3C1/1208.321",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();
		Document getOof = createFetchOof();

		Document returned = op.postXml("Settings", getOof, "Settings");
		DOMUtils.logDom(returned);
	}

	private Document createFetchOof() throws TransformerException {
		Document document = DOMUtils.createDoc("Settings", "Settings");
		Element root = document.getDocumentElement();
		Element oof = DOMUtils.createElement(root, "Oof");
		Element get = DOMUtils.createElement(oof, "Get");
		DOMUtils.createElementAndText(get, "BodyType", "TEXT");
		return document;
	}

	public void testProvision() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "iPad", "Apple-iPad3C1/1208.321",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();
		Document doc = open("proto14/provision_req.xml");
		Document returned = op.postXml("Provision", doc, "Provision");
		DOMUtils.logDom(returned);
		String srvSk = DOMUtils.getUniqueElement(returned.getDocumentElement(), "PolicyKey").getTextContent();

		// <?xml version="1.0" encoding="UTF-8"?><Provision xmlns="Provision">
		// <Policies>
		// <Policy>
		// <PolicyType>MS-EAS-Provisioning-WBXML</PolicyType>
		// <PolicyKey>2147483647</PolicyKey>
		// <Status>1</Status>
		// </Policy>
		// </Policies>
		// </Provision>
		Document phase2 = open("proto14/provision_p2_req.xml");
		phase2.getElementsByTagName("PolicyKey").item(0).setTextContent(srvSk);
		Document phase3 = op.postXml("Provision", phase2, "Provision");
		DOMUtils.logDom(phase3);

	}

	public void testProvisionAndroid() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "Android", "androidC123456",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();
		Document doc = open("proto14/android_provision_req.xml");
		Document returned = op.postXml("Provision", doc, "Provision", ImmutableMap.of("X-MS-PolicyKey", "0"));
		DOMUtils.logDom(returned);
		String srvSk = DOMUtils.getUniqueElement(returned.getDocumentElement(), "PolicyKey").getTextContent();

		// <?xml version="1.0" encoding="UTF-8"?><Provision xmlns="Provision">
		// <Policies>
		// <Policy>
		// <PolicyType>MS-EAS-Provisioning-WBXML</PolicyType>
		// <PolicyKey>2147483647</PolicyKey>
		// <Status>1</Status>
		// </Policy>
		// </Policies>
		// </Provision>
		Document phase2 = open("proto14/android_provision_p2_req.xml");
		phase2.getElementsByTagName("PolicyKey").item(0).setTextContent(srvSk);
		Document phase3 = op.postXml("Provision", phase2, "Provision");
		DOMUtils.logDom(phase3);

	}

	public void testResolveRecipients() throws Exception {
		OPClient op = new OPClient("tom@ex2k10.wmv", "Bluejob31_", "devId", "iPad", "Apple-iPad3C1/1208.321",
				"https://ex2k10.blue-mind.loc/Microsoft-Server-ActiveSync");
		op.setProtocolVersion(ProtocolVersion.V141);
		op.options();
		Document doc = open("proto14/resolve_req.xml");
		Document returned = op.postXml("ResolveRecipients", doc, "ResolveRecipients");
		DOMUtils.logDom(returned);
	}

	private Document open(String name) throws Exception {
		try (InputStream in = Proto14Ex2k10Tests.class.getClassLoader().getResourceAsStream("data/" + name)) {
			return DOMUtils.parse(in);
		}
	}

}
