package net.bluemind.central.reverse.proxy.model.common.mapper;

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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RecordKey [type=").append(type).append(", owner=").append(owner).append(", uid=").append(uid)
				.append(", id=").append(id).append(", valueClass=").append(valueClass).append("]");
		return builder.toString();
	}

}
