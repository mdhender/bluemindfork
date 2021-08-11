/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore.domains;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchy;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;

public class RestoreFlatHierarchy implements RestoreDomainType {

	private static final ValueReader<ItemValue<ContainerHierarchyNode>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<ContainerHierarchyNode>>() {
			});
	private final IServerTaskMonitor monitor;
	private ItemValue<Domain> domain;
	private IServiceProvider target;

	public RestoreFlatHierarchy(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target) {
		this.monitor = monitor;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return IFlatHierarchyUids.TYPE;
	}

	@Override
	public void restore(DataElement de) {
		ItemValue<ContainerHierarchyNode> item = mrReader.read(new String(de.payload));
		System.err.println("kafka: " + item);
		System.err.println("de: " + de);
		IInternalContainersFlatHierarchy intApi = target.instance(IInternalContainersFlatHierarchy.class, domain.uid,
				de.key.owner);
		ItemValue<ContainerHierarchyNode> existing = intApi.getComplete(item.uid);
		System.err.println("current: " + existing);
		if (existing != null && existing.internalId != item.internalId) {
			intApi.delete(item.uid);
			System.err.println("Re-create " + item);
			intApi.createWithId(item.internalId, item.uid, item.value);
		} else if (existing != null && existing.internalId == item.internalId) {
			intApi.update(item.uid, item.value);
		} else {
			intApi.createWithId(item.internalId, item.uid, item.value);
		}
	}

}
