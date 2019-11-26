/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.tag.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.persistence.ItemTagRef;
import net.bluemind.tag.persistence.TagRefStore;
import net.bluemind.tag.service.IInCoreTagRef;

public class TagRefService implements IInCoreTagRef {

	private TagRefStore tagRefStore;
	private BmContext context;

	public TagRefService(DataSource ds, Container container, BmContext context) {
		this.context = context;
		tagRefStore = new TagRefStore(ds, container);
	}

	@Override
	public void create(Item item, List<TagRef> value) throws ServerFault {
		try {
			tagRefStore.create(item, fromTagRefs(value));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void update(Item item, List<TagRef> value) throws ServerFault {
		try {
			tagRefStore.update(item, fromTagRefs(value));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void delete(Item item) throws ServerFault {
		try {
			tagRefStore.delete(item);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<TagRef> get(Item item) throws ServerFault {
		List<ItemTagRef> refs;
		try {
			refs = tagRefStore.get(item);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		Map<String, Map<String, ItemValue<Tag>>> cache = new HashMap<>();
		Map<String, Set<String>> lookupMap = new HashMap<>();

		refs.stream().forEach(ref -> {
			Set<String> val = lookupMap.computeIfAbsent(ref.containerUid, v -> new HashSet<>());
			val.add(ref.itemUid);
		});

		lookupCachedUids(cache, lookupMap);

		return refs.stream().map(ref -> {
			return decorate(cache, ref);
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	public void deleteAll() throws ServerFault {
		try {
			tagRefStore.deleteAll();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<List<TagRef>> getMultiple(List<Item> items) throws ServerFault {
		List<List<ItemTagRef>> list;
		try {
			list = tagRefStore.getMultiple(items);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		Map<String, Map<String, ItemValue<Tag>>> cache = new HashMap<>();
		Map<String, Set<String>> lookupMap = new HashMap<>();

		list.stream().forEach(l -> {
			if (l != null) {
				l.stream().forEach(ref -> {
					Set<String> val = lookupMap.computeIfAbsent(ref.containerUid, v -> new HashSet<>());
					val.add(ref.itemUid);
				});
			}
		});

		lookupCachedUids(cache, lookupMap);

		return list.stream().map(refs -> {
			if (refs != null) {
				return refs.stream().map(ref -> {
					return decorate(cache, ref);
				}).filter(Objects::nonNull).collect(Collectors.toList());
			} else {
				return Collections.<TagRef>emptyList();
			}
		}).collect(Collectors.toList());

	}

	private TagRef decorate(Map<String, Map<String, ItemValue<Tag>>> cache, ItemTagRef ref) {
		ItemValue<Tag> tag = cache.get(ref.containerUid).get(ref.itemUid);
		if (tag != null) {
			TagRef tagRef = new TagRef();
			tagRef.itemUid = ref.itemUid;
			tagRef.containerUid = ref.containerUid;
			tagRef.label = tag.value.label;
			tagRef.color = tag.value.color;
			return tagRef;
		} else {
			return null;
		}
	}

	private ItemTagRef fromTagRef(TagRef t) {
		return ItemTagRef.create(t.containerUid, t.itemUid);
	}

	private List<ItemTagRef> fromTagRefs(List<TagRef> lt) {
		return lt.stream().map(t -> this.fromTagRef(t)).collect(Collectors.toList());
	}

	private ITags getService(String tagContainerUid) {
		return context.su().provider().instance(ITags.class, tagContainerUid);
	}

	private void lookupCachedUids(Map<String, Map<String, ItemValue<Tag>>> cache, Map<String, Set<String>> lookupMap) {
		lookupMap.entrySet().forEach(entry -> {
			ITags service = getService(entry.getKey());
			List<ItemValue<Tag>> references = service.multipleGet(new ArrayList<>(entry.getValue()));
			Map<String, ItemValue<Tag>> mapping = cache.computeIfAbsent(entry.getKey(), v -> new HashMap<>());
			references.forEach(ref -> {
				mapping.put(ref.uid, ref);
			});

		});
	}
}
