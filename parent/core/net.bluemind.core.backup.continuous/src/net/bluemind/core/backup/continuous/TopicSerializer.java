package net.bluemind.core.backup.continuous;

public interface TopicSerializer<T, U> {

	byte[] key(T item);

	byte[] value(U item);

}
