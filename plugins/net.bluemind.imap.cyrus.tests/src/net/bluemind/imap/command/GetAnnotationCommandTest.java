package net.bluemind.imap.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import net.bluemind.imap.Annotation;
import net.bluemind.imap.impl.IMAPResponse;

public class GetAnnotationCommandTest {
	@Test
	public void privAndSharedValue_quoted() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload(
				"* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.priv\" \"priv value\" \"value.shared\" \"shared value\")");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertEquals("priv value", a.valuePriv);
		assertEquals("shared value", a.valueShared);
	}

	@Test
	public void privAndSharedValue_braket() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload(
				"* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.priv\" {10}priv value \"value.shared\" {12}shared value)");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertEquals("priv value", a.valuePriv);
		assertEquals("shared value", a.valueShared);
	}

	@Test
	public void privAndSharedValue_quoted_multiLines() {
		IMAPResponse ir1 = new IMAPResponse();
		ir1.setStatus("OK");
		ir1.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.shared\" \"shared value\")");
		IMAPResponse ir2 = new IMAPResponse();
		ir2.setStatus("OK");
		ir2.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.priv\" \"priv value\")");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir1, ir2);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertEquals("priv value", a.valuePriv);
		assertEquals("shared value", a.valueShared);
	}

	@Test
	public void privAndSharedValue_braket_multiLines() {
		IMAPResponse ir1 = new IMAPResponse();
		ir1.setStatus("OK");
		ir1.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.shared\" {12}shared value)");
		IMAPResponse ir2 = new IMAPResponse();
		ir2.setStatus("OK");
		ir2.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.priv\" {10}priv value)");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir1, ir2);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertEquals("priv value", a.valuePriv);
		assertEquals("shared value", a.valueShared);
	}

	@Test
	public void privValue_bracket() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent \"/specialuse\" (\"value.priv\" {5}\\Sent)");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/specialuse");
		assertNotNull(a);

		assertEquals("\\Sent", a.valuePriv);
		assertNull(a.valueShared);
	}

	@Test
	public void sharedValue_quoted() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.shared\" \"shared value\")");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertNull(a.valuePriv);
		assertEquals("shared value", a.valueShared);
	}

	@Test
	public void sharedValue_bracket() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent \"/specialuse\" (\"value.shared\" {6}shared)");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/specialuse");
		assertNotNull(a);

		assertEquals("shared", a.valueShared);
		assertNull(a.valuePriv);
	}

	@Test
	public void privValue_quoted() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.priv\" \"priv value\")");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertNull(a.valueShared);
		assertEquals("priv value", a.valuePriv);
	}

	@Test
	public void nilPrivAndSharedValue() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.priv\" NIL \"value.shared\" NIL)");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertNull(a.valueShared);
		assertNull(a.valuePriv);
	}

	@Test
	public void nilPrivValue() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.priv\" NIL)");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertNull(a.valueShared);
		assertNull(a.valuePriv);
	}

	@Test
	public void nilSharedValue() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.shared\" NIL)");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertNull(a.valueShared);
		assertNull(a.valuePriv);
	}

	@Test
	public void invalid_breakAfterMailboxName() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(0, gac.data.size());

		ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sen");
		imapResponse = ImmutableList.of(ir);

		gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(0, gac.data.size());
	}

	@Test
	public void notQuotedAnnotationName() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent /vendor/cmu/cyrus-imapd/squat (\"value.shared\" NIL)");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertNull(a.valueShared);
		assertNull(a.valuePriv);
	}

	@Test
	public void invalid_noParenthesis() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" \"value.shared\" NIL");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(0, gac.data.size());
	}

	@Test
	public void notQuotedAnnotationValueKind() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload("* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (value.shared \"value shared\")");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertNull(a.valueShared);
		assertNull(a.valuePriv);
	}

	@Test
	public void unknownAnnotationValueKind() {
		IMAPResponse ir = new IMAPResponse();
		ir.setStatus("OK");
		ir.setPayload(
				"* ANNOTATION Sent \"/vendor/cmu/cyrus-imapd/squat\" (\"value.invalid\" \"value invalid\" \"value.shared\" \"shared value\" \"value.priv\" \"priv value\")");
		List<IMAPResponse> imapResponse = ImmutableList.of(ir);

		GetAnnotationCommand gac = new GetAnnotationCommand("Sent");
		gac.responseReceived(imapResponse);

		assertNotNull(gac.data);
		assertEquals(1, gac.data.size());

		Annotation a = gac.data.get("/vendor/cmu/cyrus-imapd/squat");
		assertNotNull(a);

		assertEquals("shared value", a.valueShared);
		assertEquals("priv value", a.valuePriv);
	}
}
