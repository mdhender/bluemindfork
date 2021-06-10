package net.bluemind.core.backup.continuous;

import com.google.common.base.MoreObjects;

import net.bluemind.core.container.model.ItemValue;

public class DataElement {
	public RecordKey key;
	public ItemValue<?> value;
	public byte[] payload;

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(DataElement.class)//
				.add("id", key.id)//
				.add("len", payload == null ? null : payload.length)//
				.toString();
	}
}