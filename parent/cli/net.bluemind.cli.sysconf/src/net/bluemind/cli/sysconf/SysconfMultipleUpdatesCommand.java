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
package net.bluemind.cli.sysconf;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.system.api.ISystemConfiguration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "mset", description = "Set values by using a file")
public class SysconfMultipleUpdatesCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("sysconf");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SysconfMultipleUpdatesCommand.class;
		}
	}

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Parameters(paramLabel = "<file>", description = "a Json file which contains one or multiple key-value pairs")
	public Path file = null;

	@Option(required = true, names = "--format", description = "a Json or Properties file which contains one or multiple key-value pairs. Format value : <json|properties>")
	public String format = null;

	@Override
	public void run() {
		ISystemConfiguration configurationApi = ctx.adminApi().instance(ISystemConfiguration.class);
		if (Files.isReadable(file)) {
			Map<String, String> map;
			if (format.equalsIgnoreCase("json")) {
				map = jsonFileToMap(file);
			} else if (format.equalsIgnoreCase("properties")) {
				map = propertiesFileToMap(file);
			} else {
				ctx.error(String.format("format unrecognized: %s", format));
				return;
			}
			configurationApi.updateMutableValues(map);

		} else {
			ctx.error(String.format("%s not found or is not readable", file));
		}

	}

	private Map<String, String> jsonFileToMap(Path filepath) {
		Map<String, String> map = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			String content = new String(Files.readAllBytes(filepath));
			map = mapper.readValue(content, new TypeReference<Map<String, String>>() {
			});
		} catch (Exception ex) {
			ctx.error(ex.getMessage());
		}
		return map;
	}

	private Map<String, String> propertiesFileToMap(Path filepath) {
		Map<String, String> map = Collections.emptyMap();
		Properties prop = new Properties();
		try (InputStream input = Files.newInputStream(filepath)) {
			prop.load(input);
			map = prop.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.toString()));
		} catch (Exception ex) {
			ctx.error(ex.getMessage());
		}

		return map;
	}
}