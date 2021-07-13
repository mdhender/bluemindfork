package net.bluemind.central.reverse.proxy.model.mapper;

import java.util.Optional;

public interface RecordKeyMapper<T> {
	Optional<RecordKey> map(T key);
}
