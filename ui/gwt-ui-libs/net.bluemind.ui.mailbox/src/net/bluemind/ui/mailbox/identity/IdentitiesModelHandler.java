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
package net.bluemind.ui.mailbox.identity;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.gwt.endpoint.MailboxIdentityGwtEndpoint;
import net.bluemind.mailbox.identity.api.gwt.serder.IdentityGwtSerDer;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.gwt.js.JsUserMailIdentity;

public class IdentitiesModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.mailbox.IdentitiesModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new IdentitiesModelHandler();
			}
		});
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final MailIdentitiesModel mim = model.cast();
		mim.setSupportsExternalIdentities(false);
		final MailboxIdentityGwtEndpoint endpoint = new MailboxIdentityGwtEndpoint(Ajax.TOKEN.getSessionId(),
				mim.getDomainUid(), mim.getMailboxUid());

		endpoint.getIdentities(new DefaultAsyncHandler<List<IdentityDescription>>(handler) {

			@Override
			public void success(List<IdentityDescription> value) {
				mim.setIdentities(value);
				endpoint.getPossibleIdentities(new DefaultAsyncHandler<List<IdentityDescription>>(handler) {

					@Override
					public void success(List<IdentityDescription> value) {
						mim.setIdentitiesTemplates(value);
						handler.success(null);
					}
				});
			}

		});

	}

	@Override
	public void save(JavaScriptObject model, AsyncHandler<Void> handler) {
		final MailIdentitiesModel umim = model.cast();

		MailboxIdentityGwtEndpoint endpoint = new MailboxIdentityGwtEndpoint(Ajax.TOKEN.getSessionId(),
				umim.getDomainUid(), umim.getMailboxUid());
		doSave(endpoint, umim.getCreate(), umim.getUpdate(), umim.getDelete(), handler);
	}

	private void doSave(final MailboxIdentityGwtEndpoint endpoint,
			final JsArray<JsItemValue<JsUserMailIdentity>> toCreate,
			final JsArray<JsItemValue<JsUserMailIdentity>> toUpdate, final JsArrayString toDelete,
			final AsyncHandler<Void> handler) {

		if (toCreate.length() > 0) {
			JsItemValue<JsUserMailIdentity> item = toCreate.shift();

			endpoint.create(item.getUid(), new IdentityGwtSerDer().deserialize(new JSONObject(item.getValue().cast())),
					new AsyncHandler<Void>() {

						@Override
						public void success(Void value) {
							doSave(endpoint, toCreate, toUpdate, toDelete, handler);
						}

						@Override
						public void failure(Throwable e) {
							handler.failure(e);
						}
					});

			return;
		}

		if (toUpdate.length() > 0) {
			JsItemValue<JsUserMailIdentity> value = toUpdate.shift();
			endpoint.update(value.getUid(),
					new IdentityGwtSerDer().deserialize(new JSONObject(value.getValue().cast())),
					new AsyncHandler<Void>() {

						@Override
						public void success(Void value) {
							doSave(endpoint, toCreate, toUpdate, toDelete, handler);
						}

						@Override
						public void failure(Throwable e) {
							handler.failure(e);
						}
					});
			return;
		}

		if (toDelete.length() > 0) {
			String uid = toDelete.shift();
			endpoint.delete(uid, new AsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					doSave(endpoint, toCreate, toUpdate, toDelete, handler);
				}

				@Override
				public void failure(Throwable e) {
					handler.failure(e);
				}
			});
			return;
		}
		handler.success(null);
	}
}
