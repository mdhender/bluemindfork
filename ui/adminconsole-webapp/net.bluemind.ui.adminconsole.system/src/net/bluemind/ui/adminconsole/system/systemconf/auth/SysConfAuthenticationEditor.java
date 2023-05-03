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
package net.bluemind.ui.adminconsole.system.systemconf.auth;

import java.util.HashSet;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.system.systemconf.SysConfModel;

public class SysConfAuthenticationEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.SysConfAuthenticationEditor";

	@UiField
	ListBox domainList;

	private static SysConfAuthenticationUiBinder uiBinder = GWT.create(SysConfAuthenticationUiBinder.class);

	interface SysConfAuthenticationUiBinder extends UiBinder<HTMLPanel, SysConfAuthenticationEditor> {
	}

	protected SysConfAuthenticationEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);

		loadDomains();
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, we -> new SysConfAuthenticationEditor());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);

		if (map.get(SysConfKeys.default_domain.name()) != null) {
			domainList.setSelectedIndex(detectDomainIndex(domainList, map.get(SysConfKeys.default_domain.name())));
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);

		map.putString(SysConfKeys.default_domain.name(), domainList.getSelectedValue());
	}

	private void loadDomains() {
		List<ItemValue<Domain>> domains = DomainsHolder.get().getDomains();
		domainList.addItem("---", "");

		expandDomainAlias(domainList, domains);
	}

	public static void expandDomainAlias(ListBox domainList, List<ItemValue<Domain>> domains) {
		HashSet<String> domainNames = new HashSet<>();
		for (ItemValue<Domain> domain : domains) {
			if ("global.virt".equals(domain.value.name)) {
				continue;
			}
			domain.value.aliases.stream().forEach(domainNames::add);
		}
		domainNames.stream().forEach(domainList::addItem);
	}

	public static int detectDomainIndex(ListBox domainList, String domain) {
		if (null == domain || domain.isEmpty()) {
			return 0;
		}

		for (int i = 0; i < domainList.getItemCount(); i++) {
			if (domainList.getItemText(i).equals(domain)) {
				return i;
			}
		}

		return 0;
	}
}
