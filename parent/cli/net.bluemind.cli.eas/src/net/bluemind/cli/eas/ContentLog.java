/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.cli.eas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

public class ContentLog {

	private static final String DATA_IN_LOGS = "/etc/bm-eas/data.in.logs";

	static Path recreateFile() {
		Path file = Paths.get(DATA_IN_LOGS);
		try {
			Files.deleteIfExists(file);
			Files.createFile(file);
		} catch (IOException e) {
			throw new CliException(e.getMessage());
		}
		return file;
	}

	public static class ContentLogForUser {
		public String email;

		@Spec
		private static CommandSpec spec;

		@Option(required = true, names = { "--email" }, description = "User email")
		public void setEmail(String email) {
			if (email == null || email.isEmpty()) {
				throw new ParameterException(spec.commandLine(), "User email is required");
			}

			if (email.split("@").length != 2) {
				throw new ParameterException(spec.commandLine(), String.format("User email %s is invalid", email));
			}

			this.email = email;
		}

		public void activate(CliContext ctx, CliUtils cliUtils) {
			ctx.info("Activate data.in.logs for user {}", email);
			userToLogIn(cliUtils).ifPresentOrElse(u -> activateLogsForUser(u),
					() -> new CliException(String.format("Domain not found for user email '%s'", email)));
		}

		private static void activateLogsForUser(String user) {
			try {
				Path file = createIfNotExists();
				Files.write(file, user.concat("\r\n").getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				throw new CliException(e.getMessage());
			}
		}

		private static Path createIfNotExists() {
			Path file = Paths.get(DATA_IN_LOGS);
			try {
				if (!Files.exists(file)) {
					Files.createFile(file);
				}
			} catch (IOException e) {
				throw new CliException(e.getMessage());
			}
			return file;
		}

		public void deactivate(CliContext ctx, CliUtils cliUtils) {
			ctx.info("Deactivate data.in.logs for user {}", email);
			userToLogIn(cliUtils).ifPresentOrElse(this::deactivateLogsForUser,
					() -> new CliException(String.format("Domain not found for user email '%s'", email)));
		}

		private void deactivateLogsForUser(String user) {
			Path file = Paths.get(DATA_IN_LOGS);
			if (!Files.exists(file)) {
				throw new CliException(
						String.format("Cannot deactivate logs for user %s because data.in.logs does not exist", email));
			}

			List<String> lines;
			try {
				lines = Files.readAllLines(file);
			} catch (IOException e) {
				throw new CliException(e.getMessage());
			}

			if (lines.isEmpty()) {
				throw new CliException(
						String.format("Cannot deactivate logs for user %s because data.in.logs is empty", email));
			}

			if (!lines.contains(user)) {
				throw new CliException(String
						.format("Cannot deactivate logs for user %s because is not present in data.in.logs", email));
			}
			lines.remove(user);
			rewriteFile(file, lines);
		}

		private static void rewriteFile(Path file, List<String> lines) {
			try {
				recreateFile();
				for (String line : lines) {
					byte[] strToBytes = line.concat("\r\n").getBytes();
					Files.write(file, strToBytes, StandardOpenOption.APPEND);
				}
			} catch (IOException e) {
				throw new CliException(e.getMessage());
			}
		}

		private Optional<String> userToLogIn(CliUtils cliUtils) {
			String[] emailParts = email.split("@");
			if (emailParts.length != 2) {
				return Optional.empty();
			}
			String localPart = emailParts[0];
			String domainPart = emailParts[1];
			ItemValue<Domain> domainItem = cliUtils.getDomain(domainPart)
					.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domainPart)));

			String userLogin = cliUtils.getUserLogin(domainItem.uid, localPart);
			String user = userLogin.concat("_at_").concat(domainItem.uid);
			return Optional.of(user);
		}
	}

	public static class ContentLogForAll {
		@Option(required = true, names = { "--all" }, description = "Enable content logs for all users")
		public boolean all;

		public void activate(CliContext ctx) {
			ctx.info("Activate data.in.logs for everyone");
			activateLogsForAll();
		}

		public static void activateLogsForAll() {
			recreateFile();
		}

		public void deactivate(CliContext ctx) {
			ctx.info("Deactivate data.in.logs for everyone");
			deactivateLogsForAll();
		}

		public void deactivateLogsForAll() {
			try {
				Files.deleteIfExists(Paths.get(DATA_IN_LOGS));
			} catch (IOException e) {
				throw new CliException(e.getMessage());
			}
		}
	}

}
