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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.service.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class UpgraderList {
	private static final String FILE_PATH = "/usr/share/bm-upgraders-list/upgraders.list";

	public static Set<String> get() {
		File file = new File(FILE_PATH);
		if (file.exists()) {
			try {
				return new HashSet<>(Arrays.asList(new String(Files.readAllBytes(file.toPath())).split("\r\n")) //
						.stream() //
						.filter(s -> !s.trim().isEmpty()) //
						.collect(Collectors.toSet()));
			} catch (IOException e) {
				return new HashSet<>(Collections.emptySet());
			}
		} else {
			return new HashSet<>(Collections.emptySet());
		}
	}
}
