package net.bluemind.central.reverse.proxy.model.common.mapper.impl;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKey;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKeyMapper;

public class ByteArrayRecordKeyMapper implements RecordKeyMapper<byte[]> {

	private final Logger logger = LoggerFactory.getLogger(ByteArrayRecordKeyMapper.class);
	private final ObjectReader reader;
	private final ObjectWriter writer;

	public ByteArrayRecordKeyMapper(ObjectMapper objectMapper) {
		this.reader = objectMapper.readerFor(RecordKey.class);
		this.writer = objectMapper.writerFor(RecordKey.class);
	}

	public Optional<RecordKey> map(byte[] keyBytes) {
		try {
			RecordKey key = reader.readValue(keyBytes);
			return Optional.of(key);
		} catch (IOException e) {
			logger.error("Unable to deserialize key {}", new String(keyBytes));
			return Optional.empty();
		}
	}

	public Optional<byte[]> map(RecordKey key) {
		try {
			return Optional.of(writer.writeValueAsBytes(key));
		} catch (JsonProcessingException jse) {
			logger.error("Unable to serialize key {}", key, jse);
			return Optional.empty();
		}
	}
}
