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
package net.bluemind.ui.adminconsole.directory.user;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.api.gwt.endpoint.MailboxesGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;

public class UserQuotaModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.UserQuotaModelHandler";

	protected UserQuotaModelHandler(ModelHandler modelHandler) {

	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new UserQuotaModelHandler(modelHandler);
			}
		});
		GWT.log("bm.ac.UserQuotaModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final String userUid = map.getString("userId");
		String domainUid = map.getString("domainUid");

		MailboxesGwtEndpoint service = new MailboxesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		service.getMailboxQuota(userUid, new AsyncHandler<MailboxQuota>() {

			@Override
			public void success(MailboxQuota value) {
				if (null == value.quota) {
					map.putString("quotaUsed", "0");
					map.putString("quotaUsedKb", "0");
				} else {
					double q = value.quota;
					double u = value.used;
					double perc = (u / q) * 100;
					int percInt = (int) perc;
					percInt = Math.min(100, percInt);
					map.putString("quotaUsed", String.valueOf(percInt));
					map.putString("quotaUsedKb", String.valueOf((int) u));
				}
				handler.success(null);
			}

			@Override
			public void failure(Throwable e) {
				map.putString("quotaUsed", "0");
				handler.success(null);
			}

		});

	}

	@Override
	public void save(JavaScriptObject model, AsyncHandler<Void> handler) {
		handler.success(null);
	}
}
