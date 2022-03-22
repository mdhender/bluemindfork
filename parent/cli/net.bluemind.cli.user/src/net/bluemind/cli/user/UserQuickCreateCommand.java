/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import com.github.javafaker.Faker;

import io.netty.util.internal.ThreadLocalRandom;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.IServer;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "quickcreate", description = "Create a mail-enabled user")
public class UserQuickCreateCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserQuickCreateCommand.class;
		}
	}

	@Parameters(paramLabel = "<login@domain>", description = "login@domain of the user to create")
	public String loginAtDomain;

	@Option(names = "--pass", description = "password to apply, otherwise localpart will be used")
	public String password;

	@Option(names = "--random", description = "Generate random infos into the VCard")
	public Boolean randomData = false;

	@Option(names = "--random-datalocation", description = "Random datalocation attribution")
	public Boolean randomDatalocation = false;

	private CliContext ctx;

	private static final com.github.javafaker.Name nameFaker = Faker.instance().name();
	private static final com.github.javafaker.PhoneNumber phoneFaker = Faker.instance().phoneNumber();
	private static final com.github.javafaker.GameOfThrones gotFaker = Faker.instance().gameOfThrones();

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	private String forEmail(String namePart) {
		return namePart.replace(' ', '.').toLowerCase();
	}

	public void run() {
		if (!EmailHelper.isValid(loginAtDomain)) {
			ctx.error(loginAtDomain + " does not look like a valid email.");
			return;
		}
		String[] splitted = loginAtDomain.split("@");
		String localPart = splitted[0];
		String domainPart = splitted[1];

		CliUtils cu = new CliUtils(ctx);
		String dom = cu.getDomainUidByDomain(domainPart);
		if (dom == null) {
			ctx.error(domainPart + " is not a known domain or alias.");
		}
		IGroup grpApi = ctx.adminApi().instance(IGroup.class, dom);
		ItemValue<Group> userGroup = grpApi.byName("user");
		CountDownLatch cdl = new CountDownLatch(1);
		MQ.init().thenAccept(v -> {
			User u = new User();
			u.login = localPart;
			VCard card = new VCard();
			String familyName = domainPart.toUpperCase();
			String givenName = localPart;
			String extraName = null;
			String fn = givenName + " " + familyName;
			List<Email> emails = new ArrayList<>();
			Email defEmail = Email.create(loginAtDomain, true, false);
			emails.add(defEmail);
			if (Boolean.TRUE.equals(randomData)) {
				familyName = nameFaker.lastName();
				givenName = nameFaker.firstName();
				extraName = localPart;
				fn = givenName + " " + extraName + " " + familyName;
				card.organizational.role = gotFaker.house();
				card.communications.tels = Arrays.asList(Tel.create(phoneFaker.cellPhone(), Collections.emptyList()));

				String fakeLocal = forEmail(givenName) + "." + forEmail(familyName)
						+ ThreadLocalRandom.current().nextInt(100, 1000);
				defEmail.isDefault = false;
				emails.add(Email.create(fakeLocal + "@" + domainPart, true, false));
			}
			card.identification.name = Name.create(familyName, givenName, extraName, null, null, null);
			card.identification.formatedName = FormatedName.create(fn);
			u.contactInfos = card;

			u.password = Optional.ofNullable(password).orElse(localPart);
			u.accountType = AccountType.FULL;
			u.routing = Routing.internal;
			u.emails = emails;

			if (randomDatalocation == null || !randomDatalocation) {
				u.dataLocation = ctx.adminApi().instance(IServer.class, "default").allComplete().stream()
						.filter(s -> s.value.tags.contains("mail/imap")).findAny().map(s -> s.uid).orElse(null);
			}

			IUser uApi = ctx.adminApi().instance(IUser.class, dom);
			String uid = "cli-created-" + UUID.nameUUIDFromBytes(loginAtDomain.getBytes()).toString().toLowerCase();
			ctx.info("Creating " + uid + " for " + loginAtDomain);
			uApi.create(uid, u);
			if (userGroup != null) {
				grpApi.add(userGroup.uid, Arrays.asList(Member.user(uid)));
			}
			cdl.countDown();
		}).whenComplete((v, ex) -> {
			if (ex != null) {
				ctx.error(ex.getMessage());
			} else {
				ctx.info("finished for " + loginAtDomain);
			}
		}).join();
	}

}
