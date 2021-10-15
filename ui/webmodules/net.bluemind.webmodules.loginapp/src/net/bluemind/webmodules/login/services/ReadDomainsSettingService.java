/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.webmodules.login.services;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class ReadDomainsSettingService {

	private static final Logger logger = LoggerFactory.getLogger(ReadDomainsSettingService.class);

	private String filepath;
	private List<Map<String, String>> domainSettings = new ArrayList<Map<String, String>>();

	private static class InstanceLoader {
		private static final ReadDomainsSettingService INSTANCE = new ReadDomainsSettingService(null);
	}

	public static ReadDomainsSettingService getInstance() {
		return InstanceLoader.INSTANCE;
	}

	private ReadDomainsSettingService(String filepath) {
		this.filepath = filepath;
	}

	public static ReadDomainsSettingService build(String filepath) {
		if (Strings.isNullOrEmpty(getInstance().filepath)) {
			getInstance().filepath = filepath;
		}
		return getInstance();
	}

	public void sync() throws Exception {
		readDefaultDomain();
	}

	private void readDefaultDomain() {

		if (filepath == null) {
			logger.error("Cannot read 'domains-settings' file because path is null");
			return;
		}

		domainSettings.clear();

		try {
			List<String> readAllLines = Files.readAllLines(Paths.get(filepath));
			readAllLines.forEach(l -> {
				String[] elements = l.split(":");
				domainSettings.add(Stream
						.of(new AbstractMap.SimpleEntry<>("domainUid", elements[0]),
								new AbstractMap.SimpleEntry<>("externalUrl", elements[1]),
								new AbstractMap.SimpleEntry<>("defaultDomain", elements[2]))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
			});
		} catch (Exception e) {
			logger.warn("Error occurs trying to read " + filepath + " file", e);
		}
	}

	public Optional<String> getDefaultDomain(String requestHostUrl) {

		if (domainSettings == null || domainSettings.isEmpty()) {
			return Optional.empty();
		}

		Optional<String> defaultDomainFromFile = Optional.ofNullable(domainSettings.stream()
				.filter(map -> map.entrySet().stream()
						.anyMatch(e -> e.getKey().equals("externalUrl") && e.getValue().equals(requestHostUrl)))
				.map(map -> map.get("defaultDomain")).findFirst().orElse(null));

		return defaultDomainFromFile;
	}

}
