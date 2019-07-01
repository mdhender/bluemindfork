package net.bluemind.eas.endpoint.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.util.MimeUtil;
import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.device.api.Device;
import net.bluemind.eas.endpoint.tests.helpers.TestMail;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.EasHeaders;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.testhelper.device.TestDeviceHelper;
import net.bluemind.eas.testhelper.device.TestDeviceHelper.TestDevice;
import net.bluemind.eas.testhelper.mock.RequestObject;
import net.bluemind.eas.testhelper.mock.RequestsFactory;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.testhelper.vertx.Deploy;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.validation.IProtocolValidator;
import net.bluemind.eas.validation.Validator;
import net.bluemind.eas.wbxml.WBXMLTools;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.user.api.User;

public abstract class AbstractEndpointTest extends TestCase {

	protected WbxmlHandlerBase endpoint;
	protected RequestsFactory reqFactory;
	protected String devId;
	protected String devType;
	protected ItemValue<Device> device;
	protected String login;
	protected String password;
	protected String vmHostname;
	protected ItemValue<User> owner;
	protected TestDevice testDevice;
	protected String domainUid;

	protected ResponseObject lastAsyncResponse;
	private Set<String> deploymentIDs;
	private List<TestMail> addedMails = new LinkedList<>();

	public void setUp() throws Exception {
		IProtocolValidator validator = Validator.get();
		System.out.println("Validator is " + validator);
		this.testDevice = TestDeviceHelper.beforeTest("junit-" + getName());
		this.login = testDevice.loginAtDomain;
		this.password = testDevice.password;
		this.vmHostname = testDevice.vmHostname;
		this.reqFactory = new RequestsFactory(login, password, "http://" + vmHostname);
		this.devId = testDevice.devId;
		this.devType = testDevice.devType;
		this.owner = testDevice.owner;
		this.device = testDevice.device;
		this.domainUid = testDevice.domainUid;
		this.endpoint = createEndpoint();

		deploymentIDs = Deploy.beforeTest(new String[0], new String[] { "net.bluemind.eas.protocol.impl.ProtocolWorker",
				"net.bluemind.eas.impl.vertx.WorkerLazyLoader",
				"net.bluemind.eas.wbxml.builder.vertx.ByteSourceEventProducer",
				"net.bluemind.eas.busmods.SendMailVerticle", "net.bluemind.eas.busmods.CollectionListenerVerticle" });
		CountDownLatch cdl = new CountDownLatch(1);
		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {
				cdl.countDown();
			}
		});
		assertTrue(cdl.await(30, TimeUnit.SECONDS));

	}

	public abstract WbxmlHandlerBase createEndpoint();

	public void tearDown() throws Exception {
		rmAddedMails();
		TestDeviceHelper.afterTest(testDevice);
		Deploy.afterTest(deploymentIDs);
		this.endpoint = null;
		System.out.println("Device " + device.uid + " deleted.");
	}

	protected ResponseObject runEndpoint(Document document) {
		return runEndpoint(endpoint, document, new HashMap<String, String>());
	}

	protected ResponseObject runEndpoint(WbxmlHandlerBase handler, Document document) {
		return runEndpoint(handler, document, new HashMap<String, String>());
	}

	protected ResponseObject runEndpointNoBody(String cmd) {
		ImmutableMap<String, String> headers = ImmutableMap.of(EasHeaders.Client.PROTOCOL_VERSION, "14.1");
		ImmutableMap<String, String> queryParams = ImmutableMap.of("Cmd", cmd);
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
		Buffer buf = response.waitForIt(2, TimeUnit.SECONDS);
		assertNotNull(buf);
		return response;
	}

	protected ResponseObject runEndpoint(WbxmlHandlerBase handler, Document document,
			Map<String, String> customHeaders) {
		System.out.println("run.......");
		Map<String, String> pimped = new HashMap<String, String>();
		pimped.put(EasHeaders.Client.PROTOCOL_VERSION, "14.1");
		pimped.putAll(customHeaders);
		ImmutableMap<String, String> headers = ImmutableMap.copyOf(pimped);
		ImmutableMap<String, String> queryParams = ImmutableMap.of("Cmd", document.getDocumentElement().getNodeName());
		AuthorizedDeviceQuery dq = reqFactory.authorized(VertxPlatform.getVertx(), devId, devType, headers, queryParams,
				device.uid);
		ResponseObject response = (ResponseObject) dq.request().response();
		try {
			handler.handle(dq);
			RequestObject theRequest = (RequestObject) dq.request();
			lastAsyncResponse = response;
			byte[] wbxml = WBXMLTools.toWbxml(document.getDocumentElement().getNamespaceURI(), document);
			theRequest.trigger(wbxml);
		} catch (Throwable t) {
			t.printStackTrace();
			fail("endpoint.handle must not throw");
		}
		Buffer buf = response.waitForIt(20, TimeUnit.SECONDS);
		assertNotNull(buf);
		return response;
	}

	protected Document newDocument(String ns, String rootElem) {
		Document document = DOMUtils.createDoc(ns, rootElem);
		return document;
	}

	protected TestMail appendEmail(String subject) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		MessageServiceFactory msf = MessageServiceFactory.newInstance();
		Message mm = getRandomMessage(msf, subject);
		MessageWriter writer = msf.newMessageWriter();
		writer.writeMessage(mm, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

		return appendEmail("INBOX", in, new FlagsList());
	}

	protected TestMail appendEmail(String mbox, InputStream in, FlagsList fl) throws Exception {
		try (StoreClient sc = new StoreClient(vmHostname, 143, login, password)) {
			assertTrue(sc.login());
			sc.select(mbox);

			int id = sc.append(mbox, in, fl);
			assertTrue(id > 0);
			TestMail ret = new TestMail(mbox, id);
			addedMails.add(ret);
			ESearchActivator.refreshIndex("mailspool");
			return ret;
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return null;
	}

	protected boolean fetchEmail(String folder, int uid) throws IMAPException, IOException {
		try (StoreClient sc = new StoreClient(vmHostname, 1143, login, password)) {
			assertTrue(sc.login());
			sc.select(folder);
			IMAPByteSource m = sc.uidFetchMessage(uid);
			boolean ret = m.source().size() > 0;
			m.close();
			return ret;
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return false;
	}

	protected InputStream stream(String res) {
		return AbstractEndpointTest.class.getClassLoader().getResourceAsStream(res);
	}

	protected TestMail appendEml(String mbox, String emlPath, FlagsList fl) throws Exception {
		return appendEmail(mbox, stream(emlPath), fl);
	}

	protected void updateMessage(TestMail toFlag, FlagsList fl) throws IMAPException {
		try (StoreClient sc = new StoreClient(vmHostname, 143, login, password)) {
			assertTrue(sc.login());
			sc.select(toFlag.mailbox);
			boolean flagged = sc.uidStore(Arrays.asList(toFlag.uid), fl, true);
			assertTrue(flagged);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	protected void rmAddedMails() throws IMAPException {
		try (StoreClient sc = new StoreClient(vmHostname, 143, login, password)) {
			assertTrue(sc.login());
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			for (TestMail tm : addedMails) {
				sc.select(tm.mailbox);
				List<Integer> asList = Arrays.asList(tm.uid);
				sc.uidStore(asList, fl, true);
				sc.uidExpunge(asList);
			}
			sc.expunge();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	private Message getRandomMessage(MessageServiceFactory msf, String subject) throws UnsupportedEncodingException {
		MessageBuilder builder = msf.newMessageBuilder();
		Message mm = builder.newMessage();
		BasicBodyFactory bbf = new BasicBodyFactory();
		Header h = builder.newHeader();
		h.setField(Fields.contentType("text/html; charset=UTF-8"));
		h.setField(Fields.contentTransferEncoding(MimeUtil.ENC_8BIT));
		mm.setHeader(h);
		TextBody text = bbf.textBody("<html><body>osef</body></html>", "UTF-8");
		mm.setBody(text);
		Date now = new Date();
		mm.setSubject(subject);
		mm.setDate(now);

		Mailbox mbox = new Mailbox("John Bang", "john.bang", "local.lan");
		mm.setFrom(mbox);

		String[] email = owner.value.defaultEmail().address.split("@");
		Mailbox to = new Mailbox(owner.displayName, email[0], email[1]);
		mm.setTo(to);

		return mm;
	}
}
