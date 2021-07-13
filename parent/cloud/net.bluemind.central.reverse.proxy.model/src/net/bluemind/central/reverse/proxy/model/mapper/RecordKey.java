package net.bluemind.central.reverse.proxy.model.mapper;

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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RecordKey [type=").append(type).append(", owner=").append(owner).append(", uid=").append(uid)
				.append(", id=").append(id).append(", valueClass=").append(valueClass).append("]");
		return builder.toString();
	}

}
