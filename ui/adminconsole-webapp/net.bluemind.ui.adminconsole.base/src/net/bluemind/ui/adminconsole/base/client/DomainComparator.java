/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.ui.adminconsole.base.client;

import java.util.Comparator;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class DomainComparator implements Comparator<ItemValue<Domain>> {

	@Override
	public int compare(ItemValue<Domain> d1, ItemValue<Domain> d2) {
		String domainName1 = d1.value.defaultAlias;
		String domainName2 = d2.value.defaultAlias;

		if (domainName1.equals("global.virt")) {
			return 1;
		}
		if (domainName2.equals("global.virt")) {
			return -1;
		}

		return domainName1.compareTo(domainName2);
	}

}
