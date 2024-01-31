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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.helpers.MessageFormatter;

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
import net.datafaker.Faker;
import net.datafaker.providers.base.PhoneNumber;
import net.datafaker.providers.entertainment.GameOfThrones;
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

	@Parameters(paramLabel = "<login@domain>", description = "login@domain of the user to create (if --count is specified, {} must be present in the login for the counter)")
	public String loginAtDomain;

	@Option(names = "--pass", description = "password to apply, otherwise localpart will be used")
	public String password;

	@Option(names = "--random", description = "Generate random infos into the VCard")
	public Boolean randomData = false;

	@Option(names = "--random-datalocation", description = "Random datalocation attribution")
	public Boolean randomDatalocation = false;

	@Option(names = "--workers", description = "Number of threads to create users")
	public int workers = 4;

	@Option(names = "--count", description = "Number of users to create (default=1)")
	public int usercount = 1;

	private CliContext ctx;

	private static final net.datafaker.providers.base.Name nameFaker = new Faker().name();
	private static final PhoneNumber phoneFaker = new Faker().phoneNumber();
	private static final GameOfThrones gotFaker = new Faker().gameOfThrones();

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
		MQ.init().join();

		String[] splitted = loginAtDomain.split("@");
		String localPart = splitted[0];
		String domainName = splitted[1];

		CliUtils cliUtils = new CliUtils(ctx);
		String domainUid = cliUtils.getDomainUidByDomain(domainName);

		IGroup groupApi = ctx.adminApi().instance(IGroup.class, domainUid);

		if (usercount == 1) {
			createUser(localPart, domainName, domainUid, groupApi);
		} else {
			if (!localPart.contains("{}")) {
				ctx.error("NO: login@domain must contain '{}' for the counter");
			} else {
				ArrayBlockingQueue<String> q = new ArrayBlockingQueue<>(workers);
				try (ExecutorService pool = Executors.newFixedThreadPool(workers)) {
					for (int i = 0; i < usercount; i++) {
						String login = MessageFormatter.format(localPart, i).getMessage();
						try {
							q.put(login);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
						}
						pool.submit(() -> {
							try {
								createUser(login, domainName, domainUid, groupApi);
							} finally {
								q.remove(login); // NOSONAR
							}
						});
					}
				}
			}
		}

	}

	private void createUser(String login, String domainName, String domainUid, IGroup groupApi) {
		String latd = login + "@" + domainName;
		User u = new User();
		u.login = login;
		VCard card = new VCard();
		String familyName = domainName.toUpperCase();
		String givenName = login;
		String extraName = null;
		String fn = givenName + " " + familyName;
		List<Email> emails = new ArrayList<>();
		Email defEmail = Email.create(latd, true, false);
		emails.add(defEmail);
		if (Boolean.TRUE.equals(randomData)) {
			familyName = nameFaker.lastName();
			givenName = nameFaker.firstName();
			extraName = login;
			fn = givenName + " " + extraName + " " + familyName;
			card.organizational.role = gotFaker.house();
			card.communications.tels = Arrays.asList(Tel.create(phoneFaker.cellPhone(), Collections.emptyList()));

			String fakeLocal = forEmail(givenName) + "." + forEmail(familyName)
					+ ThreadLocalRandom.current().nextInt(100, 1000);
			defEmail.isDefault = false;
			emails.add(Email.create(fakeLocal + "@" + domainName, true, false));
		}
		card.identification.name = Name.create(familyName, givenName, extraName, null, null, null);
		card.identification.formatedName = FormatedName.create(fn);
		u.contactInfos = card;

		u.password = Optional.ofNullable(password).orElse(login);
		u.accountType = AccountType.FULL;
		u.routing = Routing.internal;
		u.emails = emails;

		if (randomDatalocation == null || !randomDatalocation) {
			u.dataLocation = ctx.adminApi().instance(IServer.class, "default").allComplete().stream()
					.filter(s -> s.value.tags.contains("mail/imap")).findAny().map(s -> s.uid).orElse(null);
		}

		IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);
		String uid = "cli-created-" + UUID.nameUUIDFromBytes(latd.getBytes()).toString().toLowerCase();
		ctx.info("Creating {} for {}", uid, latd);
		userApi.create(uid, u);
		ItemValue<Group> userGroup = groupApi.byName("user");
		if (userGroup != null) {
			groupApi.add(userGroup.uid, Arrays.asList(Member.user(uid)));
		}
	}

}
