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

	public RecordKey() {

	}

	public RecordKey(String type, String owner, String uid, long id, String valueClass) {
		this.type = type;
		this.owner = owner;
		this.uid = uid;
		this.id = id;
		this.valueClass = valueClass;
	}

	public static <T> RecordKey forItemValue(TopicDescriptor descriptor, ItemValue<T> item) {
		String valueClass = item.value == null ? null : item.value.getClass().getCanonicalName();
		return new RecordKey(descriptor.type(), descriptor.owner(), descriptor.id(), item.internalId, valueClass);
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

}
