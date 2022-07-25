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
package net.bluemind.mailmessage.service;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailflow.common.api.Recipient;
import net.bluemind.mailflow.common.api.Recipient.AddressType;
import net.bluemind.mailflow.common.api.Recipient.RecipientType;
import net.bluemind.mailflow.common.api.SendingAs;
import net.bluemind.mailmessage.api.IMailTip;
import net.bluemind.mailmessage.api.MailTipContext;
import net.bluemind.mailmessage.api.MailTipFilter;
import net.bluemind.mailmessage.api.MailTipFilter.FilterType;
import net.bluemind.mailmessage.api.MailTips;
import net.bluemind.mailmessage.api.MessageContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailMessageServiceTests {

	private static final String domain = "testdomain.loc";

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt();
		PopulateHelper.addDomain(domain);
	}

	@Test
	public void testNoFilterListShouldEvaluateAllHandlers() {
		IMailTip service = getService();

		List<MailTips> mailTips = service.getMailTips(getContext());

		Assert.assertEquals(3, mailTips.size());
		assertContainsForMessage(mailTips, "TestTip1");
		assertContains(mailTips, "him", "TestTip1", "TestTip2");
		assertContains(mailTips, "him2", "TestTip1");
	}

	@Test
	public void testEmptyListFilterListShouldEvaluateNothing() {
		IMailTip service = getService();

		List<MailTips> mailTips = service.getMailTips(getContext(FilterType.INCLUDE, new String[0]));

		Assert.assertEquals(0, mailTips.size());
	}

	@Test
	public void testFindingAMatchingTipsFor1RequestedTipType() {
		IMailTip service = getService();

		List<MailTips> mailTips = service.getMailTips(getContext(FilterType.INCLUDE, "TestTip1"));

		Assert.assertEquals(3, mailTips.size());
		assertContainsForMessage(mailTips, "TestTip1");
		assertContains(mailTips, "him", "TestTip1");
		assertContains(mailTips, "him2", "TestTip1");

		mailTips = service.getMailTips(getContext(FilterType.INCLUDE, "TestTip2"));

		Assert.assertEquals(1, mailTips.size());
		assertContains(mailTips, "him", "TestTip2");
	}

	@Test
	public void testFindingAMatchingTipsForMultipleRequestedTipType() {
		IMailTip service = getService();

		List<MailTips> mailTips = service.getMailTips(getContext(FilterType.INCLUDE, "TestTip1", "TestTip2"));

		Assert.assertEquals(3, mailTips.size());
		assertContainsForMessage(mailTips, "TestTip1");
		assertContains(mailTips, "him", "TestTip1");
		assertContains(mailTips, "him2", "TestTip1");
	}

	@Test
	public void testFindingAMatchingTipsExcludedForMultipleRequestedTipType() {
		IMailTip service = getService();

		List<MailTips> mailTips = service.getMailTips(getContext(FilterType.EXCLUDE, "TestTip1", "TestTip2"));

		Assert.assertEquals(0, mailTips.size());
	}

	@Test
	public void testFindingAMatchingTipsExcludedForTip1RequestedTipType() {
		IMailTip service = getService();

		List<MailTips> mailTips = service.getMailTips(getContext(FilterType.EXCLUDE, "TestTip1"));

		Assert.assertEquals(1, mailTips.size());
		assertContains(mailTips, "him", "TestTip2");
	}

	@Test
	public void testFindingAMatchingTipsExcludedForTip2RequestedTipType() {
		IMailTip service = getService();

		List<MailTips> mailTips = service.getMailTips(getContext(FilterType.EXCLUDE, "TestTip2"));

		Assert.assertEquals(3, mailTips.size());
		assertContainsForMessage(mailTips, "TestTip1");
		assertContains(mailTips, "him", "TestTip1");
		assertContains(mailTips, "him2", "TestTip1");
	}

	@Test
	public void testFindingAMatchingRuleUsingNonMatchingExcludeContext() {
		IMailTip service = getService();

		List<MailTips> mailTips = service.getMailTips(getContext(FilterType.EXCLUDE, "i-dont-exist"));

		Assert.assertEquals(3, mailTips.size());
		assertContainsForMessage(mailTips, "TestTip1");
		assertContains(mailTips, "him", "TestTip1", "TestTip2");
		assertContains(mailTips, "him2", "TestTip1");
	}

	@Test
	public void testFindingAMatchingRuleUsingNonMatchingIncludeContext() {
		IMailTip service = getService();

		List<MailTips> mailTips = service.getMailTips(getContext(FilterType.INCLUDE, "i-dont-exist"));

		Assert.assertEquals(0, mailTips.size());
	}

	private MailTipContext getContext() {
		return getContext(null, (String[]) null);
	}

	private MailTipContext getContext(FilterType type, String... filterContext) {
		MailTipContext context = new MailTipContext();
		context.messageContext = new MessageContext();
		context.messageContext.fromIdentity = new SendingAs();
		context.messageContext.fromIdentity.from = "me@" + domain;
		context.messageContext.fromIdentity.sender = "me@" + domain;
		context.messageContext.recipients = new ArrayList<>();
		if (null != type) {
			context.filter = new MailTipFilter();
			context.filter.filterType = type;
			context.filter.mailTips = Arrays.asList(filterContext);
		}
		Recipient rec = new Recipient();
		rec.addressType = AddressType.SMTP;
		rec.email = "him@" + domain;
		rec.name = "him";
		rec.recipientType = RecipientType.TO;
		context.messageContext.recipients.add(rec);
		Recipient rec2 = new Recipient();
		rec2.addressType = AddressType.SMTP;
		rec2.email = "him2@" + domain;
		rec2.name = "him2";
		rec2.recipientType = RecipientType.CC;
		context.messageContext.recipients.add(rec2);
		return context;
	}

	private IMailTip getService() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailTip.class, domain);
	}

	private void assertContains(List<MailTips> result, String recipientName, String... tips) {
		for (MailTips tipResult : result) {
			if (tipResult.forRecipient != null && tipResult.forRecipient.name.equals(recipientName)) {
				for (int i = 0; i < tips.length; i++) {
					final int index = i;
					assertTrue(tipResult.matchingTips.stream().anyMatch(tr -> tr.mailtipType.equals(tips[index])));
				}
			}
		}
	}

	private void assertContainsForMessage(List<MailTips> result, String... tips) {
		for (MailTips tipResult : result) {
			if (tipResult.forRecipient == null) {
				for (int i = 0; i < tips.length; i++) {
					final int index = i;
					assertTrue(tipResult.matchingTips.stream().anyMatch(tr -> tr.mailtipType.equals(tips[index])));
				}
			}
		}
	}

}
