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
package net.bluemind.ui.adminconsole.base;

import java.util.Map;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.gwt.endpoint.DomainSettingsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.SubscriptionInformations.InstallationIndicator;
import net.bluemind.system.api.SubscriptionInformations.Kind;
import net.bluemind.system.api.gwt.endpoint.InstallationGwtEndpoint;

public class SubscriptionInfoHolder {

	private static SubscriptionInfoHolder INST = new SubscriptionInfoHolder();

	public static SubscriptionInfoHolder get() {
		return INST;
	}

	private static boolean subIncludesSimpleAccounts = false;
	private static boolean domainHasSimpleAccounts = false;

	public void init() {
		new InstallationGwtEndpoint(Ajax.TOKEN.getSessionId())
				.getSubscriptionInformations(new AsyncHandler<net.bluemind.system.api.SubscriptionInformations>() {

					@Override
					public void success(SubscriptionInformations subInfo) {
						subIncludesSimpleAccounts = false;

						if (subInfo.kind == Kind.HOST) {
							subIncludesSimpleAccounts = true;
						} else {
							subInfo.indicator.forEach(installIndicator -> {
								if (installIndicator.kind == InstallationIndicator.Kind.SimpleUser && installIndicator.maxValue > 0) {
									subIncludesSimpleAccounts = true;
								}
							});
						}
					}

					@Override
					public void failure(Throwable e) {
						subIncludesSimpleAccounts = false;
					}

				});

		DomainsHolder.get().registerDomainChangedListener(new IDomainChangedListener() {

			@Override
			public void activeDomainChanged(ItemValue<Domain> newActiveDomain) {
				checkDomainHasSimpleAccount(newActiveDomain.uid);
			}

		});
	}

	private void checkDomainHasSimpleAccount(String domain) {
		new DomainSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), domain)
		.get(new AsyncHandler<Map<String, String>>() {

			@Override
			public void success(Map<String, String> values) {
						domainHasSimpleAccounts = false;
						domainHasSimpleAccounts = Integer
								.valueOf(values.get(DomainSettingsKeys.domain_max_basic_account.toString())) > 0;
			}

			@Override
			public void failure(Throwable e) {
						domainHasSimpleAccounts = false;
			}

		});
	}

	public static boolean subIncludesSimpleAccount() {
		return subIncludesSimpleAccounts;
	}

	public static boolean domainHasSimpleAccounts() {
		return domainHasSimpleAccounts;
	}

	public static boolean domainAndSubAllowSimpleAccount() {
		return domainHasSimpleAccounts && subIncludesSimpleAccounts;
	}

}
