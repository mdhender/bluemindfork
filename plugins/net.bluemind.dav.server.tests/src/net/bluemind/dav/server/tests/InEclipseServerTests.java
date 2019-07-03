package net.bluemind.dav.server.tests;

import java.io.FileNotFoundException;

import javax.xml.transform.TransformerException;

public class InEclipseServerTests extends AbstractServerTest {

	@Override
	protected String getLogin() {
		return "tom@ex2016.vmw";
	}

	@Override
	protected String getUid() {
		return "admin";
	}

	@Override
	protected String getPassword() {
		return "Bluejob31!";
	}

	@Override
	protected String getServerUrl() {
		return "http://localhost:8080";
	}

	@Override
	protected String getPrefix() {
		return "/dav";
	}

	@Override
	public void testWhoAmI() {
		super.testWhoAmI();
	}

	@Override
	public void testAttendeeAutocomplete() {
		super.testAttendeeAutocomplete();
	}

	@Override
	public void testCalAndBookLocation() {
		super.testCalAndBookLocation();
	}

	@Override
	public void testCalProps() throws FileNotFoundException, TransformerException {
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
	public void testBookMultiputOneDList() {
		super.testBookMultiputOneDList();
	}

	@Override
	public void testBookMultiputContactInDlist() {
		super.testBookMultiputContactInDlist();
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
	public void testPutInDomainCal() {
		super.testPutInDomainCal();
	}

	@Override
	public void testExpandGroupMemberSetReport() {
		super.testExpandGroupMemberSetReport();
	}

	@Override
	public void testPatchValarmDatetime() {
		super.testPatchValarmDatetime();
	}

	@Override
	public void testSetupIsOk() {
		super.testSetupIsOk();
	}

	@Override
	public void testMkCalendar() throws Exception {
		super.testMkCalendar();
	}

	@Override
	public void testPropfindMissingAddressbook() throws TransformerException {
		super.testPropfindMissingAddressbook();
	}

}
