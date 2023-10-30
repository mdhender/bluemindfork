package net.bluemind.central.reverse.proxy.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class MemberInfo {
	public final boolean added;

	public final String groupUid;

	public final String memberType;

	public final String memberUid;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public MemberInfo(@JsonProperty("added") boolean added, @JsonProperty("domainUid") String groupUid,
			@JsonProperty("memberType") String memberType, @JsonProperty("memberUid") String memberUid) {
		this.added = added;
		this.groupUid = groupUid;
		this.memberType = memberType;
		this.memberUid = memberUid;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(MemberInfo.class).add("added", added).add("group", groupUid)
				.add("memberType", memberType).add("memberUid", memberUid).toString();
	}
}
