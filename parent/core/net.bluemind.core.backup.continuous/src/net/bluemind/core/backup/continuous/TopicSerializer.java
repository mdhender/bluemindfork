package net.bluemind.core.backup.continuous;

import net.bluemind.directory.api.ReservedIds;

public interface TopicSerializer<T, U> {

	byte[] key(T item);

	byte[] value(U item, ReservedIds reservedIds);

}
