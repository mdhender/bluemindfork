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
package net.bluemind.ui.mailbox.filter;

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.mailbox.api.IMailboxesPromise;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.gwt.endpoint.MailboxesGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.IUserSettingsPromise;
import net.bluemind.user.api.gwt.endpoint.UserSettingsGwtEndpoint;

public class MailSettingsModelHandler implements IGwtModelHandler {

	private static final String MAIL_APPLICATION = "mail-application";
	private MailboxesGwtEndpoint mailboxes;
	private String mailboxUid;
	private String userId;
	private UserSettingsGwtEndpoint userSettings;

	@Override
	public void load(final JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringString m = model.cast();
		this.mailboxUid = m.get("mailboxUid");
		this.userId = m.get("userId");
		final String domainUid = m.get("domainUid");
		final String entryUid = m.get("entryUid");
		final JsMapStringJsObject map = model.cast();

		IDirectoryPromise directoryService = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid)
				.promiseApi();
		CompletableFuture<Void> dirEntryLoad = directoryService.findByEntryUid(entryUid).thenAccept(value -> {
			map.put("datalocation", value.dataLocation);
		});

		mailboxes = new MailboxesGwtEndpoint(Ajax.TOKEN.getSessionId(), m.get("domainUid"));
		IMailboxesPromise mailboxesService = mailboxes.promiseApi();
		CompletableFuture<Void> mailboxLoad = mailboxesService.getMailboxFilter(mailboxUid).thenAccept(value -> {
			MailSettingsModel.populate(model, value);
		});

		userSettings = new UserSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		IUserSettingsPromise userSettingsService = userSettings.promiseApi();
		CompletableFuture<Void> userSettingsLoad = userSettingsService.getOne(userId, MAIL_APPLICATION)
				.thenAccept(value -> {
					MailSettingsModel.populate(model, value);
				});

		CompletableFuture.allOf(dirEntryLoad, mailboxLoad, userSettingsLoad).thenRun(() -> handler.success(null))
				.exceptionally(t -> {
					handler.failure(t);
					return null;
				});

	}

	@Override
	public void save(final JavaScriptObject model, final AsyncHandler<Void> handler) {
		if (mailboxUid.contains("global.virt")) {
			handler.success(null);
			return;
		}
		MailSettingsModel mm = MailSettingsModel.get(model);
		MailFilter mf = mm.getMailFilter();
		String mailApplication = mm.getMailApplication();
		if (mf == null || mailApplication == null) {
			handler.success(null);
		} else {
			mailboxes.setMailboxFilter(mailboxUid, mf, new DefaultAsyncHandler<Void>(handler) {
				@Override
				public void success(Void value) {
					userSettings.setOne(userId, MAIL_APPLICATION, mailApplication,
							new DefaultAsyncHandler<Void>(handler) {
								@Override
								public void success(Void value) {
									handler.success(null);
								}
							});

				}
			});

		}
	}

	public static final String TYPE = "bm.mailbox.MailSettingsModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE,
				new IGwtDelegateFactory<IGwtModelHandler, net.bluemind.gwtconsoleapp.base.editor.ModelHandler>() {

					@Override
					public IGwtModelHandler create(net.bluemind.gwtconsoleapp.base.editor.ModelHandler model) {
						return new MailSettingsModelHandler();
					}
				});
		GWT.log("bm.settings.MailSettingsModelHandler registred");
	}
}