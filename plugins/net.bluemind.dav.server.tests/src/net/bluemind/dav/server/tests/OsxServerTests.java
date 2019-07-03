package net.bluemind.dav.server.tests;

import java.io.FileNotFoundException;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;

import com.google.common.collect.ImmutableMap;

public class OsxServerTests extends AbstractServerTest {

	@Override
	protected String getLogin() {
		return "admin";
	}

	@Override
	protected String getPassword() {
		return "admin";
	}

	@Override
	protected String getServerUrl() {
		return "https://172.16.138.147";
	}

	@Override
	protected String getPrefix() {
		return "/principals/";
	}

	@Override
	public void testWhoAmI() {
		super.testWhoAmI();
	}

	@Override
	public void testCalAndBookLocation() {
		super.testCalAndBookLocation();
	}

	@Override
	public void testCalProps() throws TransformerException, FileNotFoundException {
		super.testCalProps();
	}

	@Override
	public void testListAddressbooks() {
		super.testListAddressbooks();
	}

	@Override
	public void testBookMultiputOneContact() {
		super.testBookMultiputOneContact();
	}

	@Override
	public void testFindCalendarAvailability() {
		super.testFindCalendarAvailability();
	}

	@Override
	public void testCalendarQueryReport() {
		super.testCalendarQueryReport();
	}

	@Override
	public void testExpandGroupMemberSetReport() {
		super.testExpandGroupMemberSetReport();
	}

	@Override
	public void testPatchValarmDatetime() {
		super.testPatchValarmDatetime();
	}

	public void testFB() {
		String princ = client.getCurrentUserPrincipal(getPrefix());
		UserResources cr = client.getCalendarLocation(princ);
		Document doc = client.post(cr.getScheduleOutbox(), "osx_request.ics", ImmutableMap.<String, Object> of());
		assertNotNull(doc);
	}

	@Override
	public void testMkCalendar() throws Exception {
		super.testMkCalendar();
	}

	@Override
	public void testPropfindMissingAddressbook() throws TransformerException {
		// TODO Auto-generated method stub
		super.testPropfindMissingAddressbook();
	}

}
