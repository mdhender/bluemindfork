package net.bluemind.core.backup.continuous;

import com.google.common.base.MoreObjects;

public class DataElement {
	public RecordKey key;
	public byte[] payload;
	public int part;
	public long offset;

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(DataElement.class)//
				.add("key", key)//
				.add("len", payload == null ? null : payload.length)//
				.toString();
	}
}