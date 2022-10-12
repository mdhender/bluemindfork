package net.bluemind.delivery.lmtp.common;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.james.mime4j.dom.Message;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.common.io.CountingOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.mime4j.common.Mime4JHelper;

public class FreezableDeliveryContent {

	public record SerializedMessage(ByteBuf buffer, String guid) {
		public long size() {
			return buffer.readableBytes();
		}
	}

	private final DeliveryContent content;
	private final long size;
	private SerializedMessage serializedMessage;

	private FreezableDeliveryContent(DeliveryContent content, long size) {
		this.content = content;
		this.size = size;
	}

	private FreezableDeliveryContent(DeliveryContent content, SerializedMessage serializedMessage) {
		this(content, serializedMessage.size());
		this.serializedMessage = serializedMessage;
	}

	public DeliveryContent content() {
		return content.withSize(size);
	}

	public long size() {
		return size();
	}

	public SerializedMessage serializedMessage() {
		return serializedMessage;
	}

	public boolean isEmpty() {
		return content.isEmpty();
	}

	public boolean isFrozen() {
		return serializedMessage != null;
	}

	public static FreezableDeliveryContent create(DeliveryContent content, long size) throws IOException {
		return new FreezableDeliveryContent(content, size);
	}

	public static FreezableDeliveryContent freeze(DeliveryContent content) throws IOException {
		return serialize(content, true);
	}

	public static FreezableDeliveryContent discard(DeliveryContent content) throws IOException {
		return new FreezableDeliveryContent(content.withMessage(null), 0);
	}

	public static FreezableDeliveryContent copy(DeliveryContent content) throws IOException {
		return serialize(content, false);
	}

	private static FreezableDeliveryContent serialize(DeliveryContent content, boolean closeMessage)
			throws IOException {
		Path tmp = Files.createTempFile("lmtp-inc-", ".eml");
		try (var fileOutput = Files.newOutputStream(tmp);
				var countedOutput = new CountingOutputStream(fileOutput);
				@SuppressWarnings("deprecation")
				var hashedOutput = new HashingOutputStream(Hashing.sha1(), countedOutput)) {
			Mime4JHelper.serialize(content.message(), hashedOutput);
			String messageGuid = hashedOutput.hash().toString();
			long messageSize = countedOutput.getCount();
			ByteBuf messageBuffer = mmapBuffer(tmp, messageSize);
			if (closeMessage) {
				closeMessage(content.message());
			}
			SerializedMessage serializedMessage = new SerializedMessage(messageBuffer, messageGuid);
			content.mailboxRecord().messageBody = messageGuid;
			return new FreezableDeliveryContent(content, serializedMessage);
		} finally {
			Files.delete(tmp);
		}
	}

	private static ByteBuf mmapBuffer(Path tmp, long size) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(tmp.toFile(), "r")) {
			MappedByteBuffer mmap = raf.getChannel().map(MapMode.READ_ONLY, 0, size);
			return Unpooled.wrappedBuffer(mmap);
		}
	}

	private static void closeMessage(Message message) {
		try {
			message.close();
		} catch (Exception e) {
			// This should not happen
		}
	}
}
