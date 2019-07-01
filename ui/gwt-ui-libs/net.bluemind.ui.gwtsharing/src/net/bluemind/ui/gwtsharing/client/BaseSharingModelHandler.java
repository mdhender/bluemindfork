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
package net.bluemind.ui.gwtsharing.client;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.api.IContainerManagementAsync;
import net.bluemind.core.container.api.IContainerManagementPromise;
import net.bluemind.core.container.api.gwt.endpoint.ContainerManagementGwtEndpoint;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public abstract class BaseSharingModelHandler implements IGwtModelHandler {

	private final String modelId;
	private List<AccessControlEntry> acl;

	public BaseSharingModelHandler(String modelId) {
		this.modelId = modelId;
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject m = model.cast();

		IContainerManagementPromise cm = new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(),
				getContainerUid(model)).promiseApi();
		cm.getDescriptor().thenCompose(container -> {
			IDirectoryPromise directoryService = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(),
					container.domainUid).promiseApi();
			warn(container.ownerDirEntryPath);
			return directoryService.getEntry(container.ownerDirEntryPath);
		}).thenCompose(owner -> {
			SharingModel.init(m, modelId);
			SharingModel.populate(m, modelId, owner);
			return cm.getAccessControlList();
		}).thenAccept(value -> {
			BaseSharingModelHandler.this.acl = value;
			SharingModel.populate(m, modelId, value);
			handler.success(null);
		}).exceptionally(t -> {
			handler.success(null);
			return null;
		});

	}

	private native void error(String message) /*-{
												window.console.error(message);
												}-*/;

	private native void warn(String message) /*-{
												window.console.warn(message);
												}-*/;

	private native void info(String message) /*-{
												window.console.info(message);
												}-*/;

	private native void log(String message) /*-{
											window.console.log(message);
											}-*/;

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		SharingModel sm = SharingModel.get(model, modelId);
		if (sm == null) {
			handler.success(null);
		} else {

			List<AccessControlEntry> acl = sm.getAcl();

			if (!AclComparator.aclEquals(this.acl, acl)) {
				IContainerManagementAsync cm = new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(),
						getContainerUid(model));

				cm.getAccessControlList(new DefaultAsyncHandler<List<AccessControlEntry>>() {

					@Override
					public void success(List<AccessControlEntry> value) {
						for (AccessControlEntry accessControlEntry : value) {
							if (accessControlEntry.subject.startsWith("x-calendar-p")) {
								acl.add(accessControlEntry);
							}
						}

						cm.setAccessControlList(acl, handler);
					}

				});

			} else {
				handler.success(null);
				return;
			}
		}

	}

	protected abstract String getContainerUid(JavaScriptObject model);
}
