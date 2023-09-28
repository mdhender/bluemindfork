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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(name = "subscribe", description = "Manage user subscriptions")
public class UserSubscribeCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserSubscribeCommand.class;
		}
	}

	private static class Progress {
		public final int max;
		private int current = 0;

		private Progress(int max) {
			this.max = max;
		}

		public static Progress init(int max) {
			return new Progress(max);
		}

		public String getCurrentAndInc() {
			return ++current + "/" + max;
		}
	}

	private static class Logger {
		private final CliContext ctx;

		private int ignored = 0;
		private int success = 0;
		private int failure = 0;

		public Logger(CliContext ctx) {
			this.ctx = ctx;
		}

		public void userIgnored(String user) {
			ignored++;
			ctx.warn("IGNORED:User '" + user + "' not found");
		}

		public void success(Progress progress, String userUid) {
			success++;
			ctx.info(progress.getCurrentAndInc() + ":SUCCESS:user UID " + userUid + " subscribed");
		}

		public void failure(Progress progress, String userUid, ServerFault sf) {
			failure++;
			ctx.error(progress.getCurrentAndInc() + ":FAIL:user UID " + userUid + " subscribe failure - "
					+ sf.getMessage());
		}

		public void summary(Progress progress, String subscribeTo) {
			ctx.info("---");
			ctx.info("Subscribe to '" + subscribeTo + "' ending");
			ctx.info(ignored + " subscriber(s) not found");
			ctx.info(failure + "/" + progress.max + " subscription(s) in error");
			ctx.info(success + "/" + progress.max + " subscription(s) success");
		}
	}

	@Spec
	private static CommandSpec spec;

	@Option(required = false, names = { "--offline-sync" }, description = "Enable subscription offline sync.")
	public boolean offlineSync;

	@Option(required = false, names = { "--no-automount" }, description = "Disable subscription automount in Outlook.")
	public boolean noAutomount = false;

	@Option(required = false, names = { "--dry" }, description = "Dry mode.")
	public boolean dry;

	private String domainUid;

	@Option(required = true, names = { "--domain" }, description = "BlueMind domain.")
	public void getDomainUid(String domain) {
		domainUid = cliUtils.getDomainUidByDomainIfPresent(domain).orElseThrow(
				() -> new ParameterException(spec.commandLine(), "BlueMind domain '" + domain + "' not found!"));
	}

	@Option(required = true, names = { "--subscribe-to" }, description = "Container UID to subscribe.")
	public String subscribeTo;

	@ArgGroup(exclusive = true, multiplicity = "1", heading = "Subscribers%n")
	public Subscribers subscribers;

	private static class Subscribers {
		@Option(required = false, names = { "--users-all" }, description = "All domain users.")
		private boolean usersAll;

		@Option(required = false, names = { "--users-uids",
				"-u" }, split = ",", description = "Users UIDs comma separated list.")
		private List<String> usersUids;

		@Option(required = false, names = { "--users-logins",
				"-l" }, split = ",", description = "Users logins comma separated list.")
		private List<String> usersLogins;

		public Collection<String> getUids(Logger log, CliContext ctx, String domainUid) {
			Set<String> uids = new HashSet<>();
			if (usersAll) {
				uids = new HashSet<>(ctx.adminApi().instance(IUser.class, domainUid).allUids());
			} else if (usersUids != null) {
				List<String> existingUids = ctx.adminApi().instance(IUser.class, domainUid).allUids();
				for (String uid : usersUids) {
					if (!existingUids.contains(uid)) {
						log.userIgnored(uid);
						continue;
					}

					uids.add(uid);
				}
			} else if (usersLogins != null) {
				IUser userClient = ctx.adminApi().instance(IUser.class, domainUid);
				for (String login : usersLogins) {
					ItemValue<User> user = userClient.byLogin(login);
					if (user == null) {
						log.userIgnored(login);
					} else {
						uids.add(user.uid);
					}
				}
			}

			if (uids.isEmpty()) {
				throw new CliException("No valid users found in domain '" + domainUid + "'");
			}

			return uids;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;
	private Logger log;

	@Override
	public void run() {
		List<ContainerSubscription> containerSubscriptions = Arrays
				.asList(ContainerSubscription.create(subscribeTo, offlineSync, !noAutomount));

		IUserSubscription userSubscriptionClient = ctx.adminApi().instance(IUserSubscription.class, domainUid);
		Collection<String> uids = subscribers.getUids(log, ctx, domainUid);
		Progress progress = Progress.init(uids.size());
		uids.forEach(uid -> {
			try {
				if (!dry) {
					userSubscriptionClient.subscribe(uid, containerSubscriptions);
					// automount is managed by cli only
					userSubscriptionClient.updateAutomount(uid, containerSubscriptions);
				}

				log.success(progress, uid);
			} catch (ServerFault sf) {
				log.failure(progress, uid, sf);
			}
		});

		log.summary(progress, subscribeTo);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		this.log = new Logger(ctx);
		return this;
	}
}
