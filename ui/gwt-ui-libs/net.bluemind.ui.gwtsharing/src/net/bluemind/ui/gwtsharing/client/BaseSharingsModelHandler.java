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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.api.IContainerManagementAsync;
import net.bluemind.core.container.api.IContainerManagementPromise;
import net.bluemind.core.container.api.gwt.endpoint.ContainerManagementGwtEndpoint;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public abstract class BaseSharingsModelHandler implements IGwtModelHandler {

	private final String modelId;
	private Map<String, List<AccessControlEntry>> loadedAcls;

	public BaseSharingsModelHandler(String modelId) {
		this.modelId = modelId;
	}

	@Override
	public void load(final JavaScriptObject model, final AsyncHandler<Void> handler) {

		final SharingsModel sm = SharingsModel.init(model, modelId);
		loadedAcls = new HashMap<>();
		loadContainers(model, new DefaultAsyncHandler<List<ContainerDescriptor>>(handler) {

			@Override
			public void success(List<ContainerDescriptor> value) {
				loadModel(model, sm, value, handler);
			}

		});
	}

	public void reload(final JavaScriptObject model, final AsyncHandler<Void> handler) {
		loadedAcls = new HashMap<>();
		final SharingsModel sm = SharingsModel.get(model, modelId);
		final Set<String> known = new HashSet<>();
		known.addAll(Arrays.asList(sm.getContainers()));
		loadContainers(model, new DefaultAsyncHandler<List<ContainerDescriptor>>(handler) {

			@Override
			public void success(List<ContainerDescriptor> value) {
				List<ContainerDescriptor> newC = new ArrayList<>();
				Set<String> all = new HashSet<>();
				for (ContainerDescriptor c : value) {
					if (!known.contains(c.uid)) {
						newC.add(c);
						known.add(c.uid);
					}
					all.add(c.uid);
				}
				GWT.log("new container " + newC.size());

				all.removeAll(known);
				for (String uid : all) {
					sm.removeData(uid);
				}
				loadModel(model, sm, newC, handler);
			}

		});
	}

	protected abstract void loadContainers(JavaScriptObject model, AsyncHandler<List<ContainerDescriptor>> handler);

	protected void loadModel(final JavaScriptObject mainModel, final SharingsModel model,
			final List<ContainerDescriptor> containers, final AsyncHandler<Void> handler) {

		List<CompletableFuture<Void>> loadModelPromises = new ArrayList<>();

		for (ContainerDescriptor cd : containers) {

			IContainerManagementPromise cm = new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(), cd.uid)
					.promiseApi();

			loadModelPromises.add(cm.getAccessControlList().thenAccept(value -> {
				loadedAcls.put(cd.uid, value);
				model.populate(cd, value);
			}).exceptionally(fn -> {
				return null;
			}));

			IDirectoryPromise directoryService = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), cd.domainUid)
					.promiseApi();
			loadModelPromises.add(directoryService.getEntry(cd.ownerDirEntryPath).thenAccept(owner -> {
				model.populate(cd, owner);
			}).exceptionally(fn -> {
				return null;
			}));
		}

		CompletableFuture.allOf(loadModelPromises.toArray(new CompletableFuture[0])).thenApply((v) -> {
			handler.success(null);
			return null;
		}).exceptionally(t -> {
			handler.success(null);
			return null;
		});

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject m = model.cast();
		SharingsModel sharingModel = m.get(modelId).cast();
		saveAcls(sharingModel, 0, handler);
	}

	private void saveAcls(final SharingsModel sharingModel, final int pos, final AsyncHandler<Void> handler) {
		if (pos >= sharingModel.getContainers().length) {
			handler.success(null);
			return;
		}

		String uid = sharingModel.getContainers()[pos];

		IContainerManagementAsync cm = new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(), uid);

		List<AccessControlEntry> acl = sharingModel.getAcl(uid);

		if (!loadedAcls.containsKey(uid) || !AclComparator.aclEquals(loadedAcls.get(uid), acl)) {

			cm.getAccessControlList(new DefaultAsyncHandler<List<AccessControlEntry>>() {

				@Override
				public void success(List<AccessControlEntry> value) {
					for (AccessControlEntry accessControlEntry : value) {
						if (accessControlEntry.subject.startsWith("x-calendar-p")) {
							acl.add(accessControlEntry);
						}
					}

					cm.setAccessControlList(acl, new DefaultAsyncHandler<Void>(handler) {

						@Override
						public void success(Void value) {
							saveAcls(sharingModel, pos + 1, handler);
						}
					});

				}

			});

		} else {
			saveAcls(sharingModel, pos + 1, handler);
		}

	}

}
