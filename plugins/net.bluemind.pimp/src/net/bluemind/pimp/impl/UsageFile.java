/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.pimp.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UsageFile {

	public Map<String, Integer> usage = new HashMap<>();
	public String comment;
	public String purpose;

	@JsonAnySetter
	public void setAdditionalProperty(String name, Integer value) {
		this.usage.put(name, value);
	}

	@Override
	public String toString() {
		return "Usage{" + usage + ", c: " + comment + ", p: " + purpose + "}";
	}

	public static UsageFile of(String jsonUsageFile) throws IOException {
		var path = Paths.get(jsonUsageFile);
		if (!Files.exists(path)) {
			return new UsageFile();
		}
		try (InputStream in = Files.newInputStream(path)) {
			ObjectMapper om = new ObjectMapper();
			return om.readValue(in, UsageFile.class);
		}
	}

}
