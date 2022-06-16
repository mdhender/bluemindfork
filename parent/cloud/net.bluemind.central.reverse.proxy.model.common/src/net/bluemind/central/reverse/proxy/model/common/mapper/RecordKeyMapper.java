package net.bluemind.central.reverse.proxy.model.common.mapper;

import java.util.Optional;

import net.bluemind.central.reverse.proxy.model.common.mapper.impl.ByteArrayRecordKeyMapper;
import net.bluemind.central.reverse.proxy.model.common.mapper.impl.Mapper;

public interface RecordKeyMapper<T> {
	Optional<RecordKey> map(T key);

	static RecordKeyMapper<byte[]> byteArray() {
		return new ByteArrayRecordKeyMapper(Mapper.mapper);
	}
}
