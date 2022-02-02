package net.bluemind.core.backup.continuous;

import com.google.common.base.MoreObjects;

import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;

public class RecordKey {

	public String type;
	public String owner;
	public String uid;
	public long id;
	public String valueClass;
	public String operation;

	public RecordKey() {

	}

	public RecordKey(String type, String owner, String uid, long id, String valueClass, String operation) {
		this.type = type;
		this.owner = owner;
		this.uid = uid;
		this.id = id;
		this.valueClass = valueClass;
		this.operation = operation;
	}

	public static <T> RecordKey forItemValue(TopicDescriptor descriptor, ItemValue<T> item, boolean isDelete) {
		String valueClass = item.value == null ? null : item.value.getClass().getCanonicalName();
		String operation = Operation.of(item, isDelete).name();
		return new RecordKey(descriptor.type(), descriptor.owner(), descriptor.uid(), item.internalId, valueClass,
				operation);
	}

	public boolean match(TopicDescriptor descriptor) {
		return descriptor.type().equals(type) && descriptor.owner().equals(owner) && descriptor.uid().equals(uid);
	}

	public byte[] serialize() {
		return JsonUtils.writer(getClass()).write(this);
	}

	public static RecordKey unserialize(byte[] data) {
		return JsonUtils.reader(RecordKey.class).read(new String(data));
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(RecordKey.class) //
				.add("type", type) //
				.add("owner", owner) //
				.add("uid", uid) //
				.add("id", id) //
				.add("valueClass", valueClass) //
				.add("operation", operation) //
				.toString();
	}

	public enum Operation {
		CREATE, UPDATE, DELETE;

		public static <T> Operation of(ItemValue<T> item, boolean isDelete) {
			if (isDelete) {
				return DELETE;
			} else {
				return (item.updated != null && !item.updated.equals(item.created)) ? UPDATE : CREATE;
			}
		}

		public static boolean isDelete(RecordKey key) {
			return valueOf(key.operation).equals(DELETE);
		}

		public static Operation of(RecordKey key) {
			return valueOf(key.operation);
		}
	}
}
