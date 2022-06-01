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
package net.bluemind.cli.cmd.api;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.domain.api.IDomains;

public class DomainNames implements Iterable<String> {

	private List<String> defaultNames;

	public DomainNames() {
		try {
			IDomains domApi = CliContext.get().adminApi().instance(IDomains.class);
			this.defaultNames = domApi.all().stream().map(iv -> iv.value.defaultAlias).collect(Collectors.toList());
		} catch (Exception e) {
			this.defaultNames = Collections.emptyList();
		}
	}

	@Override
	public Iterator<String> iterator() {
		return defaultNames.iterator();
	}

}
