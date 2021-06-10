package net.bluemind.core.backup.continuous.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DBMaker.Maker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.IBackupStore;
import net.bluemind.core.backup.continuous.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.ILiveBackupStreams;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.NoopStore;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.TopicDeserializer;
import net.bluemind.core.backup.continuous.TopicSerializer;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.store.ITopicStore;
import net.bluemind.core.backup.continuous.store.ITopicStore.DefaultTopicDescriptor;
import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;
import net.bluemind.core.backup.continuous.store.TopicNames;
import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;

public class BackupStoreFactory implements IBackupStoreFactory {

	private static final Logger logger = LoggerFactory.getLogger(BackupStore.class);

	private static final boolean CLONE_MARKER = new File("/etc/bm/continuous.clone").exists();

	private final TopicNames names;
	private final ITopicStore topicStore;
	private final boolean disabled;
	private final DB handlesBackingStore;
	private final Map<TopicDescriptor, Map<byte[], byte[]>> waitingRecordsByTopicDescr;

	public BackupStoreFactory(ITopicStore topicStore, boolean disabled) {
		String iid = InstallationId.getIdentifier();
		this.names = new TopicNames(iid);
		this.topicStore = topicStore;
		this.disabled = disabled;
		this.handlesBackingStore = buildDb();
		this.waitingRecordsByTopicDescr = new HashMap<>();
		sendRemainingRecord();
	}

	@Override
	public <T> IBackupStore<T> forContainer(BaseContainerDescriptor c) {
		TopicDescriptor descriptor = names.forContainer(c);
		TopicPublisher publisher = publisher(descriptor);
		TopicSerializer<RecordKey, ItemValue<T>> serializer = new ItemValueSerializer<>();
		Map<byte[], byte[]> waitingRecords = waitingRecordsByTopicDescr.computeIfAbsent(descriptor,
				d -> handlesBackingStore.hashMap(descriptor.fullName()).keySerializer(Serializer.BYTE_ARRAY)
						.valueSerializer(Serializer.BYTE_ARRAY).createOrOpen());
		return new BackupStore<T>(publisher, descriptor, serializer, waitingRecords);
	}

	private TopicPublisher publisher(TopicDescriptor descriptor) {
		return CLONE_MARKER || disabled || "global.virt".equals(descriptor.domainUid())
				? NoopStore.NOOP.getPublisher(descriptor)
				: topicStore.getPublisher(descriptor);
	}

	@Override
	public Collection<String> installations() {
		return topicStore.topicNames().stream().filter(name -> name.endsWith("__orphans__"))
				.map(name -> name.substring(0, name.indexOf('-'))).collect(Collectors.toList());
	}

	@Override
	public ILiveBackupStreams forInstallation(String installationid) {
		List<String> topicNames = topicStore.topicNames().stream().filter(name -> name.startsWith(installationid))
				.collect(Collectors.toList());
		TopicSubscriber orphanSubscriber = topicNames.stream().filter(name -> name.endsWith("__orphans__")).findFirst()
				.map(topicStore::getSubscriber).orElse(null);
		List<TopicSubscriber> domainSubscribers = topicNames.stream().filter(name -> !name.endsWith("__orphans__"))
				.map(topicStore::getSubscriber).collect(Collectors.toList());
		return new ILiveBackupStreams() {

			@Override
			public ILiveStream orphans() {
				return build(installationid, "__orphans__", orphanSubscriber);
			}

			@Override
			public List<ILiveStream> listAvailable() {
				return Collections.emptyList();
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
		return MoreObjects.toStringHelper(DefaultBackupStore.class).add("topicStore", topicStore)
				.add("clone", CLONE_MARKER).toString();
	}

	private ILiveStream build(String installationid, String domainUid, TopicSubscriber subscriber) {
		TopicDeserializer<RecordKey, VersionnedItem<?>> deserializer = new ItemValueDeserializer();
		return new LiveStream(installationid, domainUid, subscriber, deserializer);
	}

	private DB buildDb() {
		Path tmp = null;
		try {
			tmp = Files.createTempFile("records-waiting-for-ack", ".mapdb");
			Files.deleteIfExists(tmp);
		} catch (IOException e) {
			logger.error("Unable to create tmp file for backingStore, using heapDB");
		}
		Maker maker = (tmp != null) ? DBMaker.fileDB(tmp.toFile().getAbsolutePath()) : DBMaker.heapDB();
		return maker.checksumHeaderBypass().fileMmapEnable()//
				.fileMmapPreclearDisable() //
				.cleanerHackEnable()//
				.fileDeleteAfterClose().make();
	}

	private void sendRemainingRecord() {
		Map<String, Object> allCollections = this.handlesBackingStore.getAll();
		allCollections.forEach((name, map) -> {
			Map<byte[], byte[]> remainingRecords = (Map<byte[], byte[]>) map;
			TopicDescriptor descriptor = DefaultTopicDescriptor.of(name);
			waitingRecordsByTopicDescr.put(descriptor, remainingRecords);
			TopicPublisher publisher = publisher(descriptor);
			remainingRecords.forEach((key, value) -> {
				publisher.store(descriptor.partitionKey(), key, value).whenComplete((v, t) -> {
					if (t == null) {
						remainingRecords.remove(key);
					}
				});
			});
		});
	}
}