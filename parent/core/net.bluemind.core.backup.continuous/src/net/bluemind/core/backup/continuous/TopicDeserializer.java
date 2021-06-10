package net.bluemind.core.backup.continuous;

public interface TopicDeserializer<T, U> {

	T key(byte[] data);

	U value(T key, byte[] data);

}
