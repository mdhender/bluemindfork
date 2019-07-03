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
import com.google.gwt.json.client.JSONArray;

import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.gwt.js.JsIdentityDescription;
import net.bluemind.mailbox.identity.api.gwt.serder.IdentityDescriptionGwtSerDer;
import net.bluemind.user.api.gwt.js.JsUserMailIdentity;

public class MailIdentitiesModel extends JavaScriptObject {

	protected MailIdentitiesModel() {

	}

	public final native boolean isSupportsExternalIdentities()
	/*-{
	return this['supportsExternalIdentities'];
	}-*/;

	public final native void setSupportsExternalIdentities(boolean support)
	/*-{
	this['supportsExternalIdentities'] = support;
	}-*/;

	public final native String getMailboxUid()
	/*-{
	return this['mailboxUid'];
	}-*/;

	public final native String getMailboxName()
	/*-{
	return this['mailboxName'];
	}-*/;

	public final native String getDomainUid()
	/*-{
	return this['domainUid'];
	}-*/;

	public final native JsArray<JsIdentityDescription> getIdentities()
	/*-{
	return this['identities'];
	}-*/;

	public final List<IdentityDescription> getIdentitiesAsList() {
		return new GwtSerDerUtils.ListSerDer<>(new IdentityDescriptionGwtSerDer())
				.deserialize(new JSONArray(getIdentities().cast()));
	}

	public final void setIdentities(List<IdentityDescription> value) {
		setIdentities(new GwtSerDerUtils.ListSerDer<>(new IdentityDescriptionGwtSerDer()).serialize(value).isArray()
				.getJavaScriptObject().<JsArray<JsIdentityDescription>>cast());
	}

	public final native void setIdentities(JsArray<JsIdentityDescription> identities)
	/*-{
	this['identities'] = identities;
	}-*/;

	public final native JsArray<JsIdentityDescription> getIdentitiesTemplates()
	/*-{
	return this['identities-templates'];
	}-*/;

	public final List<IdentityDescription> getIdentitiesTemplatesAsList() {
		return new GwtSerDerUtils.ListSerDer<>(new IdentityDescriptionGwtSerDer())
				.deserialize(new JSONArray(getIdentitiesTemplates().cast()));
	}

	public final void setIdentitiesTemplates(List<IdentityDescription> value) {
		setIdentitiesTemplates(new GwtSerDerUtils.ListSerDer<>(new IdentityDescriptionGwtSerDer()).serialize(value)
				.isArray().getJavaScriptObject().<JsArray<JsIdentityDescription>>cast());
	}

	public final native void setIdentitiesTemplates(JsArray<JsIdentityDescription> identities)
	/*-{
	this['identities-templates'] = identities;
	}-*/;

	public final native JsArray<JsItemValue<JsUserMailIdentity>> getCreate()
	/*-{
	return this['identities-to-create'];
	}-*/;

	public final native void setCreate(JsArray<JsItemValue<JsUserMailIdentity>> toCreate)
	/*-{
	this['identities-to-create'] = toCreate;
	}-*/;

	public final native JsArray<JsItemValue<JsUserMailIdentity>> getUpdate()
	/*-{
	return this['identities-to-update'];
	}-*/;

	public final native void setUpdate(JsArray<JsItemValue<JsUserMailIdentity>> toUpdate)
	/*-{
	this['identities-to-update'] = toUpdate;
	}-*/;

	public final native void setDelete(JsArrayString toDelete)
	/*-{
	this['identities-to-delete'] = toDelete;
	}-*/;

	public final native JsArrayString getDelete()
	/*-{
	return this['identities-to-delete'];
	}-*/;
}
