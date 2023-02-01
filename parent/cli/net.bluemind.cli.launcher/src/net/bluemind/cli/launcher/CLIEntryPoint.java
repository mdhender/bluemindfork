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
package net.bluemind.cli.launcher;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.osgi.framework.Version;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class CLIEntryPoint implements IApplication {

	static {
		// HOLLOW uses JUL...
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		Integer returncode = EXIT_OK;
		try {
			AnsiConsole.systemInstall();
			Version version = context.getBrandingBundle().getVersion();

			String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
			if (isCliScript(args)) {
				Path script = Paths.get(args[0]);
				CLIManager cm = new CLIManager(version);
				try (Stream<String> linesStream = Files.lines(script)) {
					Optional<Integer> returncodes = linesStream.map(this::asArguments)
							.map(optArgs -> optArgs.map(cmdargs -> {
								System.err.println(Ansi.ansi().fgBrightBlue() // NOSONAR
										.a("Execute from " + script + ": ").reset().a(String.join(" ", cmdargs)));
								return cm.processArgs(cmdargs.toArray(new String[0]));
							}).orElse(EXIT_OK)).filter(ret -> !EXIT_OK.equals(ret)).findAny();

					if (returncodes.isPresent()) {
						returncode = returncodes.get();
					}
				}
			} else {
				returncode = new CLIManager(version).processArgs(args);
			}
		} finally {
			AnsiConsole.systemUninstall();
		}
		return returncode;
	}

	private boolean isCliScript(String[] args) {
		return args.length == 1 && new File(args[0]).exists();
	}

	// Based on:
	// http://www.java2s.com/example/java-utility-method/string-split-by-quote/quoteawaresplit-string-str-char-delim-74811.html
	private Optional<List<String>> asArguments(String str) {
		boolean inQuotes = false;
		boolean inEscape = false;

		List<String> elements = new ArrayList<>();
		StringBuilder buffer = new StringBuilder();
		for (char c : str.toCharArray()) {
			if (c == ' ' && !inQuotes) {
				elements.add(buffer.toString());
				buffer.setLength(0); // clear
				inEscape = false;
				continue;
			}

			if (c == '"') {
				if (!inEscape) {
					if (inQuotes) {
						inQuotes = false;
					} else {
						inQuotes = true;
					}
					continue;
				}

				inEscape = false;
				buffer.append(c);
				continue;
			}

			if (c == '\\') {
				if (!inEscape) {
					inEscape = true;
					buffer.append(c);
					continue;
				}
			}

			// all other characters
			inEscape = false;
			buffer.append(c);
		}

		if (inQuotes) {
			System.err.println(Ansi.ansi().fgBrightBlue().a("Invalid line from: ").reset().a(str));
			return Optional.empty();
		}

		elements.add(buffer.toString());

		return Optional.of(elements);
	}

	@Override
	public void stop() {
		System.err.println("CLI stopped."); // NOSONAR
	}
}
