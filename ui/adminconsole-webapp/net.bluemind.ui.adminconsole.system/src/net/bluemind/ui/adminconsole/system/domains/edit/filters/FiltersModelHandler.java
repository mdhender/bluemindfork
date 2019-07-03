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
package net.bluemind.ui.adminconsole.system.domains.edit.filters;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.gwt.endpoint.MailboxesGwtEndpoint;
import net.bluemind.mailbox.api.gwt.serder.MailFilterGwtSerDer;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.common.client.forms.Ajax;

public class FiltersModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.FiltersModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new FiltersModelHandler();
			}
		});
		GWT.log("bm.ac.FiltersModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		loadFilters(handler, map);
	}

	private void loadFilters(final AsyncHandler<Void> handler, final JsMapStringJsObject map) {
		final String domainUid = map.getString(DomainKeys.domainUid.name());
		MailboxesGwtEndpoint mailboxService = new MailboxesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		mailboxService.getDomainFilter(new DefaultAsyncHandler<MailFilter>(handler) {

			@Override
			public void success(MailFilter value) {
				JavaScriptObject filter = new MailFilterGwtSerDer().serialize(value).isObject().getJavaScriptObject();
				map.put("mail-settings", filter);
				handler.success(null);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());
		MailFilter mailFilter = new MailFilterGwtSerDer().deserialize(new JSONObject(map.get("mail-settings")));
		MailboxesGwtEndpoint mailboxService = new MailboxesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		mailboxService.setDomainFilter(mailFilter, new DefaultAsyncHandler<Void>(handler) {

			@Override
			public void success(Void value) {
				handler.success(null);
			}
		});
	}

}
