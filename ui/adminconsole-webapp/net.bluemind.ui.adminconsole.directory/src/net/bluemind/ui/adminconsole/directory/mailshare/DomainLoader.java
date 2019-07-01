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
package net.bluemind.ui.adminconsole.directory.mailshare;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.endpoint.DomainsGwtEndpoint;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class DomainLoader implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.DomainLoader";

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String domainUid = map.getString("domainUid");
		DomainsGwtEndpoint ep = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());
		ep.get(domainUid, new AsyncHandler<ItemValue<Domain>>() {

			@Override
			public void success(ItemValue<Domain> value) {
				map.put("domain", new ItemValueGwtSerDer<>(new DomainGwtSerDer()).serialize(value).isObject()
						.getJavaScriptObject());
				handler.success(null);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, AsyncHandler<Void> handler) {
		handler.success(null);
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new DomainLoader();
			}
		});
		GWT.log("bm.ac.DomainLoader registred");
	}

}
