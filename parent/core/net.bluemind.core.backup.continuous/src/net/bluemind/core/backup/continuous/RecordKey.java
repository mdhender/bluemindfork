package net.bluemind.core.backup.continuous;

import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;

public class RecordKey {

	public String type;
	public String owner;
	public String uid;
	public long id;
	public String valueClass;
	public boolean created;

	public RecordKey() {

	}

	public RecordKey(String type, String owner, String uid, long id, String valueClass, boolean created) {
		this.type = type;
		this.owner = owner;
		this.uid = uid;
		this.id = id;
		this.valueClass = valueClass;
		this.created = created;
	}

	public static <T> RecordKey forItemValue(TopicDescriptor descriptor, ItemValue<T> item) {
		String valueClass = item.value == null ? null : item.value.getClass().getCanonicalName();
		boolean created = item.created == null || item.created.equals(item.updated);
		return new RecordKey(descriptor.type(), descriptor.owner(), descriptor.id(), item.internalId, valueClass,
				created);
	}

	public boolean match(TopicDescriptor descriptor) {
		return descriptor.type().equals(type) && descriptor.owner().equals(owner) && descriptor.id().equals(id);
	}

	public byte[] serialize() {
		return JsonUtils.writer(getClass()).write(this);
	}

	public static RecordKey unserialize(byte[] data) {
		return JsonUtils.reader(RecordKey.class).read(new String(data));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RecordKey [type=").append(type).append(", owner=").append(owner).append(", uid=").append(uid)
				.append(", id=").append(id).append(", valueClass=").append(valueClass).append("]");
		return builder.toString();
	}

}
