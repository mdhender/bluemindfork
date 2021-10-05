/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.mail.parsing;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class HeaderWhitelist {

	private static Set<String> toDrop = Sets.newHashSet("from", "to", "cc", "date", "received", "x-received", "subject",
			"content-type", "mime-version", "dkim-signature", "x-google-dkim-signature", "arc-seal", "message-id",
			"arc-message-signature", "arc-authentication-results", "received-spf", "return-path", "x-sieve");

	public final Set<String> whitelist;

	private HeaderWhitelist() {
		this.whitelist = resolveWhitelist();
	}

	private Set<String> resolveWhitelist() {
		RunnableExtensionLoader<HeaderList> epLoader = new RunnableExtensionLoader<>();
		List<HeaderList> extensions = epLoader.loadExtensions("net.bluemind.backend.mail", "parsing", "whitelist",
				"list");

		return extensions.stream().flatMap(e -> e.getWhiteList().stream()) //
				.filter(s -> s != null && !s.isEmpty()) //
				.map(String::toLowerCase) //
				.filter(s -> !toDrop.contains(s)) //
				.collect(Collectors.toSet());
	}

	public static HeaderWhitelist getInstance() {
		return InstanceLoader.INSTANCE;
	}

	private static class InstanceLoader {
		private static final HeaderWhitelist INSTANCE = new HeaderWhitelist();
	}

}
