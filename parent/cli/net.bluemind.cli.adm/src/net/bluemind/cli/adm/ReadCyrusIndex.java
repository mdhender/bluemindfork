/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.bluemind.backend.cyrus.index.CyrusIndex;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "read-cyrus-index", description = "Read a cyrus index file")
public class ReadCyrusIndex implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ReadCyrusIndex.class;
		}
	}

	private CliContext ctx;

	@Parameters(paramLabel = "/path/to/cyrus.index", description = "path to the cyrus.index to decode")
	public File indexPath;

	@Option(names = "--json", description = "dump records in json format")
	public Boolean toJson = false;

	@Override
	public void run() {
		if (!indexPath.exists()) {
			ctx.error("specified index '" + indexPath + "' does not exists");
			return;
		}
		try {
			try (InputStream in = Files.newInputStream(indexPath.toPath(), StandardOpenOption.READ)) {
				CyrusIndex index = new CyrusIndex(in);

				if (toJson) {
					ObjectMapper mapper = new ObjectMapper();
					mapper.enable(SerializationFeature.INDENT_OUTPUT);
					mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

					try (JsonGenerator json = new JsonFactory().createGenerator(System.out, JsonEncoding.UTF8)) {
						json.writeStartObject();
						json.writeStringField("index", indexPath.getAbsolutePath().toString());
						json.writeArrayFieldStart("records");
						index.readAll().stream().forEach(record -> {
							try {
								mapper.writeValue(json, record);
							} catch (IOException e) {
								e.printStackTrace();
							}
						});
						json.writeEndArray();
						json.writeEndObject();
					}
				} else {
					index.readAll().stream().forEach(System.out::println);
				}
			}
		} catch (IOException e) {
			ctx.error("Unable to open index '" + indexPath + "' : " + e);
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
