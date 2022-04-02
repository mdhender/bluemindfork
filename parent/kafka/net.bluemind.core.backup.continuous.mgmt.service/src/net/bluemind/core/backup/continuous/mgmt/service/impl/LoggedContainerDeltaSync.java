/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.IReadByIdSupport;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;

public abstract class LoggedContainerDeltaSync<O, T> extends ContainerSync {

	protected final ItemValue<DirEntryAndValue<O>> owner;
	protected final BmContext ctx;
	protected final ItemValue<Domain> domain;
	protected final IReadByIdSupport<T> crudApi;
	protected final IChangelogSupport changelogApi;
	protected final ItemValue<ContainerHierarchyNode> node;

	public static class ReadApis<T> {
		public final IReadByIdSupport<T> crudApi;
		public final IChangelogSupport changelogApi;

		public ReadApis(IReadByIdSupport<T> c, IChangelogSupport cl) {
			this.crudApi = c;
			this.changelogApi = cl;
		}
	}

	protected LoggedContainerDeltaSync(BmContext ctx, ContainerDescriptor container,
			ItemValue<ContainerHierarchyNode> node, ItemValue<DirEntryAndValue<O>> owner, ItemValue<Domain> domain) {
		super(container);
		this.owner = owner;
		this.ctx = ctx;
		this.domain = domain;
		this.node = node;
		ReadApis<T> readApi = initReadApi();
		crudApi = readApi.crudApi;
		this.changelogApi = readApi.changelogApi;
	}

	/**
	 * Called by constructor to setup the sync
	 * 
	 * @param container
	 * @param owner2
	 * @param domain2
	 * @param ctx2
	 * 
	 * @return
	 */
	protected abstract ReadApis<T> initReadApi();

	@Override
	public final void sync(ContainerState state, IBackupStoreFactory target, IServerTaskMonitor contMon) {
		ContainerChangeset<ItemVersion> cs = changelogApi.filteredChangesetById(0L, ItemFlagFilter.all());
		Stream<ItemVersion> concat = Streams.stream(Iterables.concat(cs.created, cs.updated));
		List<ItemVersion> missing = concat.filter(iv -> !state.versions.contains(iv.version))
				.collect(Collectors.toList());
		contMon.begin(missing.size(), "sync " + missing.size() + " item(s) for " + state.containerUid());
		long time = System.currentTimeMillis();
		IBackupStore<T> sink = createTargetStore(target);
		for (List<ItemVersion> chunk : Lists.partition(missing, 200)) {
			List<ItemValue<T>> loadedItems = crudApi
					.multipleGetById(chunk.stream().map(iv -> iv.id).collect(Collectors.toList()));
			for (ItemValue<T> item : loadedItems) {
				sink.store(remap(contMon, item));
				contMon.progress(1, null);
			}
		}
		time = System.currentTimeMillis() - time;
		contMon.end(true, state.containerUid() + " done in " + time + "ms.", null);

	}

	protected IBackupStore<T> createTargetStore(IBackupStoreFactory target) {
		BaseContainerDescriptor copy = BaseContainerDescriptor.create(ContainerUidsMapping.alias(cont.uid), cont.name,
				cont.owner, cont.type, cont.domainUid, cont.defaultContainer);
		return target.forContainer(copy);
	}

	protected ItemValue<T> remap(@SuppressWarnings("unused") IServerTaskMonitor contMon, ItemValue<T> item) {
		return item;
	}

}
