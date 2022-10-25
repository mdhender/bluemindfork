package net.bluemind.core.backup.continuous.dto;

import java.util.Map;

import net.bluemind.core.utils.JsonUtils;

public class IndexedMessageBodyDTO {

	public byte[] data;

	public IndexedMessageBodyDTO() {

	}

	public IndexedMessageBodyDTO(Map<String, Object> indexedBody) {
		this.data = JsonUtils.asBytes(indexedBody);
	}

}
