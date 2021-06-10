package net.bluemind.core.backup.continuous.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.TopicDeserializer;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class ItemValueDeserializer implements TopicDeserializer<RecordKey, VersionnedItem<?>> {

	private final ValueReader<RecordKey> keyReader;

	public ItemValueDeserializer() {
		this.keyReader = JsonUtils.reader(RecordKey.class);
	}

	public RecordKey key(byte[] data) {
		return keyReader.read(new String(data));
	}

	public VersionnedItem<?> value(RecordKey key, byte[] data) {
		try {
			Class<?> cls = Class.forName(key.valueClass);
			ObjectMapper mapper = new ObjectMapper();
			JavaType type = mapper.getTypeFactory().constructParametricType(VersionnedItem.class, cls);
			return (VersionnedItem<?>) JsonUtils.read(new String(data), type);
		} catch (Exception e) {
			return null;
		}
	}

}
