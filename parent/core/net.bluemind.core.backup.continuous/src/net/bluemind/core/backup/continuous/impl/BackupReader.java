package net.bluemind.core.backup.continuous.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.core.backup.continuous.IBackupReader;
import net.bluemind.core.backup.continuous.ILiveBackupStreams;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.TopicDeserializer;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.store.ITopicStore;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;

public class BackupReader implements IBackupReader {

	private static final Logger logger = LoggerFactory.getLogger(BackupReader.class);

	private final ITopicStore topicStore;

	public BackupReader(ITopicStore topicStore) {
		this.topicStore = topicStore;
	}

	@Override
	public Collection<String> installations() {
		return topicStore.topicNames().stream().filter(name -> name.endsWith("__orphans__"))
				.map(name -> instIdHexToIdentifier(name.substring(0, name.indexOf('-')))).collect(Collectors.toList());
	}

	private String instIdHexToIdentifier(String s) {
		try {
			ByteBuf uuidBuf = Unpooled.wrappedBuffer(dataFromHex(s));
			UUID uuid = new UUID(uuidBuf.readLong(), uuidBuf.readLong());
			return "bluemind-" + uuid.toString();
		} catch (Exception e) {
			logger.warn("installation id format problem '{}': {}", s, e.getMessage());
			return s;
		}
	}

	private static byte[] dataFromHex(String h) {
		if (h == null) {
			return null;
		}
		return parseHexBinary(h);
	}

	private static byte[] parseHexBinary(String s) {
		final int len = s.length();

		// "111" is not a valid hex encoding.
		if (len % 2 != 0) {
			throw new IllegalArgumentException("hexBinary needs to be even-length: " + s);
		}

		byte[] out = new byte[len / 2];

		for (int i = 0; i < len; i += 2) {
			int h = hexToBin(s.charAt(i));
			int l = hexToBin(s.charAt(i + 1));
			if (h == -1 || l == -1) {
				throw new IllegalArgumentException("contains illegal character for hexBinary: " + s);
			}

			out[i / 2] = (byte) (h * 16 + l);
		}

		return out;
	}

	private static int hexToBin(char ch) {
		if ('0' <= ch && ch <= '9') {
			return ch - '0';
		}
		if ('A' <= ch && ch <= 'F') {
			return ch - 'A' + 10;
		}
		if ('a' <= ch && ch <= 'f') {
			return ch - 'a' + 10;
		}
		return -1;
	}

	@Override
	public ILiveBackupStreams forInstallation(String instId) {
		String installationid = instId.replace("bluemind-", "").replace("-", "");
		Set<String> topicNames = topicStore.topicNames(installationid);
		logger.warn("[{} / {}] topicNames {}", instId, installationid, topicNames);
		TopicSubscriber orphanSubscriber = topicNames.stream().filter(name -> name.endsWith("__orphans__")).findFirst()
				.map(topicStore::getSubscriber).orElse(null);
		List<TopicSubscriber> domainSubscribers = topicNames.stream().filter(name -> !name.endsWith("__orphans__"))
				.map(topicStore::getSubscriber).collect(Collectors.toList());
		return new ILiveBackupStreams() {

			@Override
			public List<ILiveStream> listAvailable() {
				List<ILiveStream> streams = domains();
				streams.add(orphans());
				return streams;
			}

			@Override
			public ILiveStream orphans() {
				return build(installationid, "__orphans__", orphanSubscriber);
			}

			@Override
			public List<ILiveStream> domains() {
				return domainSubscribers.stream().map(subscriber -> {
					logger.info("{}:{}", installationid, subscriber.topicName());
					String[] tokens = subscriber.topicName().split("-");
					return build(tokens[0], tokens[1], subscriber);
				}).collect(Collectors.toList());
			}
		};
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(BackupReader.class).add("topicStore", topicStore).toString();
	}

	private ILiveStream build(String installationid, String domainUid, TopicSubscriber subscriber) {
		TopicDeserializer<RecordKey, VersionnedItem<?>> deserializer = new ItemValueDeserializer();
		return new LiveStream(installationid, domainUid, subscriber, deserializer);
	}

}