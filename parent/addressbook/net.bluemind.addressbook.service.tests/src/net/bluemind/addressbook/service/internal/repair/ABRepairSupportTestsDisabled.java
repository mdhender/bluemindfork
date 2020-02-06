/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.addressbook.service.internal.repair;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.service.AbstractServiceTests;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.tag.api.TagRef;

public class ABRepairSupportTestsDisabled extends AbstractServiceTests {

	@Test
	public void dlist_CheckEverythingOk() {
		cardStoreService.create("test1", "test1", defaultVCard());
		cardStoreService.create("test2", "test2", defaultVCard());
		VCard.Organizational.Member.create(container.uid, "test1", "test1", null);
		VCard groupCard = new VCard();
		groupCard.kind = VCard.Kind.group;
		groupCard.identification.formatedName = VCard.Identification.FormatedName.create("el group");
		groupCard.organizational.member = ImmutableList.of(
				VCard.Organizational.Member.create(container.uid, "test1", "test1", null),
				VCard.Organizational.Member.create(container.uid, "test2", "test2", null));

		cardStoreService.create("group", "el group", groupCard);

		ABRepairSupport support = new ABRepairSupport(context, "test");
		DiagnosticReport report = DiagnosticReport.create();
		support.check(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("test", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void dlist_CheckNeedRepair() {
		VCard card = defaultVCard();
		card.communications.emails = ImmutableList.of(VCard.Communications.Email.create("blabla@toto.com"));
		cardStoreService.create("test1", "test1", card);
		cardStoreService.create("test2", "test2", defaultVCard());
		VCard.Organizational.Member.create(container.uid, "test1", "test1", null);
		VCard groupCard = new VCard();
		groupCard.kind = VCard.Kind.group;
		groupCard.identification.formatedName = VCard.Identification.FormatedName.create("el group");
		groupCard.organizational.member = ImmutableList.of(
				VCard.Organizational.Member.create(container.uid, "test1", "test1", "zob@el.com"),
				VCard.Organizational.Member.create(container.uid, "test2", "test2", null));

		cardStoreService.create("group", "el group", groupCard);

		ABRepairSupport support = new ABRepairSupport(context, "test");
		DiagnosticReport report = DiagnosticReport.create();
		support.check(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("test", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.KO, report.entries.get(0).state);
	}

	@Test
	public void dlist_RepairNeedRepair() {
		VCard card = defaultVCard();
		card.communications.emails = ImmutableList.of(VCard.Communications.Email.create("blabla@toto.com"));
		cardStoreService.create("test1", "test1", card);
		cardStoreService.create("test2", "test2", defaultVCard());
		VCard.Organizational.Member.create(container.uid, "test1", "test1", null);
		VCard groupCard = new VCard();
		groupCard.kind = VCard.Kind.group;
		groupCard.identification.formatedName = VCard.Identification.FormatedName.create("el group");
		groupCard.organizational.member = ImmutableList.of(
				VCard.Organizational.Member.create(container.uid, "test1", "test1", "zob@el.com"),
				VCard.Organizational.Member.create(container.uid, "test2", "test2", null));

		cardStoreService.create("group", "el group", groupCard);

		ABRepairSupport support = new ABRepairSupport(context, "test");
		DiagnosticReport report = DiagnosticReport.create();
		support.repair(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("test", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
		assertEquals("blabla@toto.com", cardStoreService.get("group", null).value.organizational.member.get(0).mailto);
	}

	@Test
	public void tags_CheckEverythingOk() {
		VCard card = defaultVCard();
		card.explanatory.categories = ImmutableList.of(TagRef.create(tagContainer.uid, "tag1", tag1));
		cardStoreService.create("test1", "test1", card);
		cardStoreService.create("test2", "test2", defaultVCard());

		ABRepairSupport support = new ABRepairSupport(context, "test");
		DiagnosticReport report = DiagnosticReport.create();
		support.check(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("test", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void tags_CheckNeedRepair() {
		VCard card = defaultVCard();
		card.explanatory.categories = ImmutableList.of(TagRef.create(tagContainer.uid, "tag1", "toRepair", "toRepair"));
		cardStoreService.create("test1", "test1", card);
		cardStoreService.create("test2", "test2", defaultVCard());

		ABRepairSupport support = new ABRepairSupport(context, "test");
		DiagnosticReport report = DiagnosticReport.create();
		support.check(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("test", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.KO, report.entries.get(0).state);
	}

	@Test
	public void tags_RepairNeedRepair() {
		VCard card = defaultVCard();
		card.explanatory.categories = ImmutableList.of(TagRef.create(tagContainer.uid, "tag1", "toRepair", "toRepair"));
		cardStoreService.create("test1", "test1", card);
		cardStoreService.create("test2", "test2", defaultVCard());

		ABRepairSupport support = new ABRepairSupport(context, "test");
		DiagnosticReport report = DiagnosticReport.create();
		support.repair(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("test", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
		ItemValue<VCard> cardItemValue = cardStoreService.get("test1", null);
		TagRef tagRef = cardItemValue.value.explanatory.categories.get(0);
		assertEquals(tag1.color, tagRef.color);
		assertEquals(tag1.label, tagRef.label);
	}

	@Override
	protected IAddressBook getService(SecurityContext context) throws ServerFault {
		return null;
	}

}
