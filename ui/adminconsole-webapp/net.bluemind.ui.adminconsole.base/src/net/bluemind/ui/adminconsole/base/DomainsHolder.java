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
package net.bluemind.ui.adminconsole.base;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;

public class DomainsHolder {
	private static DomainsHolder inst = new DomainsHolder();

	public static DomainsHolder get() {
		return inst;
	}

	private List<ItemValue<Domain>> domains;
	private ItemValue<Domain> selectedDomain;

	public List<ItemValue<Domain>> getDomains() {
		return domains;
	}

	public void setDomains(List<ItemValue<Domain>> domains) {
		this.domains = domains;
	}

	public ItemValue<Domain> getSelectedDomain() {
		return selectedDomain;
	}

	public void setSelectedDomain(ItemValue<Domain> domain) {
		this.selectedDomain = domain;
		emitDomainChangedEvent(domain);

		JSONObject jsDomainItem = new JSONObject();
		new ItemValueGwtSerDer<Domain>(new DomainGwtSerDer()).serializeTo(domain, jsDomainItem);
		setJsonDomainsHolderSelectedDomain(jsDomainItem.getJavaScriptObject());
	}

	private static native void setJsonDomainsHolderSelectedDomain(JavaScriptObject domain)

	/*-{ 
		$wnd.jsonDomainsHolderSelectedDomain = domain;
	}-*/;

	private List<IDomainChangedListener> domainChangedListeners = new LinkedList<>();

	public void registerDomainChangedListener(IDomainChangedListener dcl) {
		domainChangedListeners.add(dcl);
	}

	public void removeDomainChangedListener(IDomainChangedListener dcl) {
		boolean rm = domainChangedListeners.remove(dcl);
		GWT.log("DomainChangedListener removal: " + rm + ". Remaining: " + domainChangedListeners.size());
	}

	private void emitDomainChangedEvent(ItemValue<Domain> d) {
		for (IDomainChangedListener dcl : domainChangedListeners) {
			dcl.activeDomainChanged(d);
		}
	}

	public ItemValue<Domain> getDomainByUid(String containerUid) {
		ItemValue<Domain> ret = null;
		for (ItemValue<Domain> d : domains) {
			if (d.uid.equals(containerUid)) {
				ret = d;
				break;
			}
		}
		return ret;
	}
}
