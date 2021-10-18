/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.cli.wazo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.cti.api.IComputerTelephonyIntegration;
import net.bluemind.domain.api.IDomains;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserExternalAccount;
import net.bluemind.user.api.UserAccount;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "populate", description = "populate wazo users")
public class WazoUserPopulateCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("wazo");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return WazoUserPopulateCommand.class;
		}
	}

	@Option(names = { "--show", "-s" }, description = "only show matching users found in Wazo")
	public boolean display = false;

	@Option(names = { "--domain", "-d" }, required = true, description = "the domain uid to populate users")
	public String domainUid;

	@Option(names = { "--user",
			"-u" }, required = true, description = "the BM user login (must have existing account to connect Wazo API")
	private String userLogin;

	private static final String SYSTEM_IDENTIFIER = "Wazo";

	protected CliContext ctx;
	protected CliUtils cliUtils;
	private String userUid;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {

		checkParams();

		try {
			IComputerTelephonyIntegration telApi = ctx.adminApi().instance(IComputerTelephonyIntegration.class,
					domainUid, userUid);
			populate(telApi.getUserEmails());
		} catch (ServerFault e) {
			throw new CliException(e.getMessage());
		}
	}

	private void checkParams() {

		cliUtils.getDomain(domainUid)
				.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domainUid)));

		userUid = cliUtils.getUserUidByLogin(domainUid, userLogin);

	}

	private void populate(List<String> wazoUserEmails) {

		Map<String, Set<String>> usersEmailMap = buildUserEmailsMap();

		Map<String, String> matchingUsers = new HashMap<String, String>();

		wazoUserEmails.forEach(wazoEmail -> {
			usersEmailMap.entrySet().stream()
					.filter(ue -> ue.getValue().stream().anyMatch(email -> email.equalsIgnoreCase(wazoEmail)))
					.findFirst().ifPresent(ue -> {
						matchingUsers.put(ue.getKey(), wazoEmail);
					});
		});

		if (display) {
			display(matchingUsers);
		} else {
			createUsers(matchingUsers);
		}

	}

	private Map<String, Set<String>> buildUserEmailsMap() {

		IUser userService = ctx.adminApi().instance(IUser.class, domainUid);
		Map<String, Set<String>> usersEmailMap = new HashMap<>();

		Set<String> domainAliases = ctx.adminApi().instance(IDomains.class).get(domainUid).value.aliases;
		List<String> userUids = userService.allUids();
		userUids.forEach(uid -> {
			Collection<Email> userEmails = userService.getComplete(uid).value.emails;
			Set<String> emailList = new HashSet<>();
			userEmails.forEach(e -> {
				if (e.allAliases) {
					domainAliases.forEach(a -> emailList.add(e.localPart().concat("@").concat(a)));
				} else {
					emailList.add(e.localPart().concat("@").concat(e.domainPart()));
				}
			});

			ctx.info("BM user found : " + uid + " : " + emailList.stream().collect(Collectors.joining(",", "{", "}")));
			usersEmailMap.put(uid, emailList);
		});

		return usersEmailMap;
	}

	private void display(Map<String, String> map) {
		String[] headers = { "User UID", "Email" };
		ctx.info(cliUtils.display(map, headers));
	}

	private void createUsers(Map<String, String> matchingUsers) {

		matchingUsers.entrySet().forEach(u -> {
			UserAccount account = new UserAccount(u.getValue());
			account.credentials = Strings.nullToEmpty(account.credentials);
			IUserExternalAccount userAccountService = ctx.adminApi().instance(IUserExternalAccount.class, domainUid,
					u.getKey());
			try {
				UserAccount existingAccount = userAccountService.get(SYSTEM_IDENTIFIER);
				if (existingAccount != null && existingAccount.login.equals(account.login)) {
					ctx.warn(account.login + " account already exists, do nothing");
				} else {
					userAccountService.create(SYSTEM_IDENTIFIER, account);
					ctx.info("Account " + account.login + " created");
				}
			} catch (Exception e) {
				ctx.error(e.getMessage());
			}
		});
	}

}
