/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cli.adm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.iq80.snappy.SnappyInputStream;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

@SuppressWarnings("deprecation")
@Command(name = "hsm-to-cyrus", description = "Converts HSM snappy spool to a cyrus maildir folder")
public class HsmToCyrus implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return HsmToCyrus.class;
		}
	}

	@Option(name = "--foldername", description = "Folder name wanted for the restoration. Defaults to hsm-orphaned")
	public String foldername = "hsm_orphaned";

	@Option(name = "--user", description = "User uid or email to convert", required = true)
	public String useridentifier;

	@Option(name = "--domain", description = "Domain uid of the user", required = true)
	public String domainUid;

	@Option(name = "--delete", description = "Remove successfully migrated orphan emails")
	public boolean deletesuccess = false;

	private CliContext ctx;

	@Override
	public void run() {
		List<String> topDir = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e",
				"f");
		IServiceProvider provider = ctx.adminApi();
		IUser userApi = provider.instance(IUser.class, domainUid);
		ItemValue<User> user;
		String uid;
		try {
			if (!useridentifier.contains("@")) {
				user = userApi.getComplete(useridentifier);
			} else {
				user = userApi.byEmail(useridentifier);
			}
			if (user == null) {
				ctx.error("Unable to find user " + useridentifier);
				return;
			}
			uid = user.uid;
		} catch (ServerFault e) {
			ctx.error("Unable to find user " + useridentifier + ": " + e);
			return;
		}

		String fullLogin = user.value.login + "@" + domainUid;
		LoginResponse lr = provider.instance(IAuthentication.class).su(fullLogin);
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, fullLogin, lr.authKey)) {
			if (sc.login()) {
				sc.create(foldername);
				sc.subscribe(foldername);
			} else {
				ctx.error("Unable to login as " + fullLogin);
			}

			// Move snappy to mailbox
			for (String rootLvl : topDir) {
				for (String subLvl : topDir) {
					String sourceDir = String.format("/var/spool/bm-hsm/snappy/user/%s/%s/%s/%s", domainUid, uid,
							rootLvl, subLvl);
					File srcDir = new File(sourceDir);
					if (srcDir.exists() && srcDir.isDirectory()) {
						try (Stream<Path> dirStream = Files.list(srcDir.toPath())) {
							dirStream.forEach(p -> inject(sc, p));
						} catch (IOException e) {
							ctx.error("Error streaming dir " + srcDir.getAbsolutePath());
						}
					}
				}
			}
		} catch (Exception e) {
			ctx.error("Unable to connect to IMAP server 127.0.0.1: " + e);
		}

	}

	private void inject(StoreClient sc, Path snapPath) {
		try (SnappyInputStream snap = new SnappyInputStream(Files.newInputStream(snapPath))) {
			int added = sc.append(foldername, snap, FlagsList.fromString("Seen"));
			if (added > 0 && deletesuccess) {
				try {
					Files.delete(snapPath);
				} catch (IOException e) {
					ctx.error("Unable to remove " + snapPath.toAbsolutePath());
				}
			}
		} catch (IOException e) {
			ctx.error("Unable to access " + snapPath.toAbsolutePath());
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
