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
package net.bluemind.ui.adminconsole.base.ui;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.ui.adminconsole.base.DomainsHolder;

public class DomainCombo extends ListBox {

	public static interface DCConstants extends Constants {
		String allDomain();
	}

	public static final DCConstants dcc = GWT.create(DCConstants.class);

	public DomainCombo() {
	}

	public void init(ItemValue<Domain> d) {
		GWT.log("init with domain: " + d.value.name + " g: " + d.value.global);
		if (d.value.global) {
			setVisible(true);

			List<ItemValue<Domain>> domList = DomainsHolder.get().getDomains();
			clear();
			for (ItemValue<Domain> dom : domList) {
				String domainName;
				if (dom.value.global) {
					domainName = dcc.allDomain();
				} else {
					domainName = dom.value.name;
				}
				addItem(domainName, dom.uid);
			}
		} else {
			setVisible(false);
		}
	}

	public void setSelectedDomainUid(String domainUid) {
		int selected = 0;
		for (int i = 0; i < getItemCount(); i++) {
			if (getValue(i).equals(domainUid)) {
				selected = i;
				break;
			}
		}

		setSelectedIndex(selected);
	}
}
