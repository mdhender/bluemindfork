package net.bluemind.dav.server.tests;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.PlatformManager;
import org.vertx.java.platform.VerticleConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;
import net.bluemind.addressbook.adapter.VCardAdapter;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.dav.server.proto.Depth;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.lib.vertx.VertxPlatform;
import net.fortuna.ical4j.vcard.VCard;

public abstract class AbstractServerTest extends TestCase {

	protected DavClient client;
	private Pattern vcardRE;

	protected abstract String getLogin();

	protected abstract String getPassword();

	protected abstract String getServerUrl();

	protected abstract String getPrefix();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		JdbcTestHelper.getInstance().beforeTest();

		//
		// JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		//
		// verticle("net.bluemind.locator.LocatorVerticle", false);
		// verticle("net.bluemind.core.rest.vertx.RestBusVerticle", true);
		// verticle("net.bluemind.core.rest.http.vertx.RestNetVerticle", false);

		String domain = getLogin().substring(getLogin().indexOf('@') + 1);
		JdbcActivator acti = JdbcActivator.getInstance();
		System.err.println("schema: " + acti.getSchemaName());
		// PopulateHelper.addGlobalVirt(acti.getDataSource());
		// PopulateHelper.addDomain(domain);

		// verticle("net.bluemind.vertx.common.bus.CoreAuth", true);
		// verticle("net.bluemind.vertx.common.bus.Locator", true);
		// verticle("net.bluemind.dav.server.ProtocolExecutorVerticle", true);
		// verticle("net.bluemind.dav.server.DavVerticle", false);

		client = new DavClient(getLogin(), getPassword(), getServerUrl());
		vcardRE = Pattern.compile(getPrefix() + "/addressbooks/__uids__/([^/]+)/book_([^/]+)/(.+)\\.vcf");

	}

	private void verticle(VerticleConstructor ctor, boolean worker) throws InterruptedException {
		CountDownLatch cdl = new CountDownLatch(1);
		if (worker) {
			worker(cdl, ctor);
		} else {
			verticle(cdl, ctor);
		}
		cdl.await();
		Thread.sleep(1000);
		System.err.println("Done with " + ctor.className());
	}

	private void verticle(final CountDownLatch cdl, final VerticleConstructor klass) {
		PlatformManager pm = VertxPlatform.getPlatformManager();
		pm.deployVerticle(klass, null, new URL[0], 1, null, new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> event) {
				System.out.println(klass + ", successful: " + event.succeeded());
				cdl.countDown();
			}
		});
	}

	private void worker(final CountDownLatch cdl, final VerticleConstructor klass) {
		PlatformManager pm = VertxPlatform.getPlatformManager();
		pm.deployWorkerVerticle(true, klass, null, new URL[0], 1, null, new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> event) {
				System.out.println(klass + ", successful: " + event.succeeded());
				cdl.countDown();
			}
		});
	}

	public void testSetupIsOk() {

	}

	@Override
	protected void tearDown() throws Exception {
		client.close();
		client = null;
		// JdbcTestHelper.getInstance().afterTest();
		super.tearDown();
	}

	public void testWhoAmI() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		assertNotNull(princ);
		assertTrue(princ.contains("/principals/__uids__/"));
		System.out.println("principal: '" + princ + "'");
	}

	public void testCalAndBookLocation() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		assertNotNull(cr);
		assertNotNull(cr.getCalendarHome());
		System.out.println("cal: '" + cr.getCalendarHome() + "'");
		assertNotNull(cr.getAddressbookHome());
	}

	public void testPropfindMissingAddressbook() throws TransformerException {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		assertNotNull(cr);
		assertNotNull(cr.getAddressbookHome());
		System.out.println("book: '" + cr.getAddressbookHome() + "'");
		Document doc = client.propfind("/addressbooks/__uids__/does-not-exist/", "propfind_missing_addressbook.xml",
				Depth.ONE);
		DOMUtils.logDom(doc);
	}

	public void testCalProps() throws TransformerException, FileNotFoundException {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		assertNotNull(cr);
		assertNotNull(cr.getCalendarHome());
		System.out.println("cal: '" + cr.getCalendarHome() + "'");
		Document doc = client.propfind(cr.getCalendarHome(), "propfind_cal_props_depth1.xml", Depth.ONE);
		DOMUtils.logDom(doc);
		DOMUtils.serialise(doc, new FileOutputStream("/Users/tom/osx.xml"), true);
	}

	public void testListAddressbooks() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		assertNotNull(cr.getAddressbookHome());
		List<String> books = client.listAddressbooks(cr);
		assertNotNull(books);
		assertEquals(1, books.size());
		String book = books.get(0);
		System.out.println("book: '" + book + "'");
	}

	public void testBookMultiputOneContact() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		String book = client.listAddressbooks(cr).get(0);
		System.out.println("book: '" + book + "'");
		String uuid = UUID.randomUUID().toString();
		System.out.println("******** CREATING ********");
		Document doc = client.post(book, "book_multiput_create_contact.xml", ImmutableMap.of("uuid", (Object) uuid));
		NodeList nl = doc.getElementsByTagNameNS(NS.WEBDAV, "href");
		assertEquals(1, nl.getLength());
		String href = nl.item(0).getTextContent();
		System.out.println("**** href: " + href);
		String vcfText = client.get(href);
		assertNotNull(vcfText);
		try {
			List<VCard> vcards = VCardAdapter.parse(vcfText);
			assertNotNull(vcards);
			assertEquals(1, vcards.size());
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
		Map<String, Object> updateValues = new HashMap<>();
		updateValues.put("book", book);
		Matcher m = vcardRE.matcher(href);
		m.find();
		String idInUrl = m.group(3);
		System.out.println("id obtained from url: " + idInUrl);
		updateValues.put("contactId", idInUrl);
		updateValues.put("uuid", uuid);
		System.out.println("******** UPDATING ********");
		doc = client.post(book, "book_multiput_update_contact.xml", updateValues);
		assertNotNull(doc);
		// TODO check more...
		System.out.println("******** DELETING ********");
		doc = client.post(book, "book_multiput_delete_contact.xml", updateValues);
		assertNotNull(doc);
	}

	public void testBookMultiputOneDList() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		String book = client.listAddressbooks(cr).get(0);
		System.out.println("book: '" + book + "'");
		String uuid = UUID.randomUUID().toString();
		String listUuid = UUID.randomUUID().toString();
		Document doc = client.post(book, "book_multiput_create_contact_and_dlist.xml",
				ImmutableMap.of("uuid", (Object) uuid, "listUuid", (Object) listUuid));
		NodeList nl = doc.getElementsByTagNameNS(NS.WEBDAV, "href");
		assertEquals(2, nl.getLength());
		String dlistHref = null;
		String coHref = null;
		for (int i = 0; i < nl.getLength(); i++) {
			String href = nl.item(i).getTextContent();
			System.out.println("href: " + href);
			String vcfText = client.get(href);
			assertNotNull(vcfText);
			try {
				List<VCard> vcards = VCardAdapter.parse(vcfText);
				assertNotNull(vcards);
				assertEquals(1, vcards.size());
				if (isDList(vcards.get(0))) {
					dlistHref = href;
				} else {
					coHref = href;
				}
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}
		}
		assertNotNull(dlistHref);
		assertNotNull(coHref);
	}

	private boolean isDList(VCard card) {
		return false;
	}

	public void testBookMultiputContactInDlist() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		String book = client.listAddressbooks(cr).get(0);
		System.out.println("book: '" + book + "'");
		String uuid = UUID.randomUUID().toString();
		Document doc = client.post(book, "book_multiput_create_contact.xml", ImmutableMap.of("uuid", (Object) uuid));
		NodeList nl = doc.getElementsByTagNameNS(NS.WEBDAV, "href");
		assertEquals(1, nl.getLength());
		String coHref = nl.item(0).getTextContent();
		assertNotNull(coHref);
		System.out.println("href: " + coHref);
		String vcfText = client.get(coHref);
		assertNotNull(vcfText);

		String listUuid = UUID.randomUUID().toString();
		doc = client.post(book, "book_multiput_create_dlist.xml", ImmutableMap.of("listUuid", (Object) listUuid));
		nl = doc.getElementsByTagNameNS(NS.WEBDAV, "href");
		assertEquals(1, nl.getLength());
		String dlistHref = nl.item(0).getTextContent();
		assertNotNull(dlistHref);
		System.out.println("href: " + dlistHref);
		vcfText = client.get(coHref);
		assertNotNull(vcfText);

		doc = client.post(book, "book_multiput_move_contact_in_dlist.xml",
				ImmutableMap.of("listUuid", (Object) listUuid, "uuid", (Object) uuid, "listHref", (Object) dlistHref));
	}

	public void testFindCalendarAvailability() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		Document doc = client.propfind(cr.getScheduleInbox(), "propfind_calendar-availability.xml", Depth.ZERO);
		assertNotNull(doc);
	}

	public void testCalendarQueryReport() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		String veventContainer = cr.getCalendarHome() + ICalendarUids.defaultUserCalendar(getUid()) + "/";
		System.out.println("vevents in '" + veventContainer + "'");
		Document doc = client.report(veventContainer, "report_calendar-query.xml");
		assertNotNull(doc);
	}

	public void testAttendeeAutocomplete() {
		Document doc = client.report(getPrefix() + "/principals/", "report_calendar-principal-search.xml");
		assertNotNull(doc);
	}

	public void testPutInDomainCal() {
		String res = getPrefix() + "/calendars/__uids__/1687/calendar/22A6DEB9-EF3E-4EFC-899B-4394576E14AF.ics";
		client.put(res, "put_in_domain_cal.ics");
		String ics = client.get(res);
		assertNotNull(ics);
	}

	public void testExpandGroupMemberSetReport() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		Document doc = client.report(princ + "calendar-proxy-read/", "report_expand_group-member-set.xml");
		assertNotNull(doc);
	}

	public void testPatchValarmDatetime() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		Document doc = client.proppatch(cr.getCalendarHome(), "proppatch_calendar_default-alarm-vevent-datetime.xml");
		assertNotNull(doc);
	}

	public void testMkCalendar() throws Exception {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		assertNotNull(cr);
		assertNotNull(cr.getCalendarHome());
		System.out.println("cal: '" + cr.getCalendarHome() + "'");
		client.mkcalendar(cr.getCalendarHome() + "todolist:ju" + System.currentTimeMillis() + "/",
				"mkcalendar_vtodo.xml");
	}

	protected String getUid() {
		return getLogin();
	}

}
