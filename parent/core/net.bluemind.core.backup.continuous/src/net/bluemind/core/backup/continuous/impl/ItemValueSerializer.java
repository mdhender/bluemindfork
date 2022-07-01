package net.bluemind.core.backup.continuous.impl;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.TopicSerializer;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueWriter;
import net.bluemind.directory.api.ReservedIds;

public class ItemValueSerializer<T> implements TopicSerializer<RecordKey, ItemValue<T>> {

	private final ValueWriter keyWriter;
	private final ValueWriter valueWriter;

	public ItemValueSerializer() {
		keyWriter = JsonUtils.writer(RecordKey.class);
		TypeReference<VersionnedItem<T>> theRef = new TypeReference<VersionnedItem<T>>() {
		};
		valueWriter = JsonUtils.writer(theRef.getType());
	}

	@Override
	public byte[] key(RecordKey item) {
		return keyWriter.write(item);
	}

	@Override
	public byte[] value(ItemValue<T> item, ReservedIds reservedIds) {
		VersionnedItem<T> reworked = new VersionnedItem<>(item, reservedIds);
		return valueWriter.write(reworked);
	}

}
