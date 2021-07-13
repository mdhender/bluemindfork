package net.bluemind.central.reverse.proxy.model.mapper.impl;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.central.reverse.proxy.model.mapper.RecordKey;
import net.bluemind.central.reverse.proxy.model.mapper.RecordKeyMapper;

public class ByteArrayRecordKeyMapper implements RecordKeyMapper<byte[]> {

	private final Logger logger = LoggerFactory.getLogger(ByteArrayRecordKeyMapper.class);

	private ObjectMapper objectMapper;

	public ByteArrayRecordKeyMapper() {
		this.objectMapper = new ObjectMapper();
	}

	public Optional<RecordKey> map(byte[] keyBytes) {
		try {
			RecordKey key = objectMapper.readValue(keyBytes, RecordKey.class);
			return Optional.of(key);
		} catch (IOException e) {
			logger.error("Unable to deserialize key {}", new String(keyBytes));
			return Optional.empty();
		}
	}
}
