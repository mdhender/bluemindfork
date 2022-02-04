package net.bluemind.core.backup.continuous.impl;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.NoopStore;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.TopicSerializer;
import net.bluemind.core.backup.continuous.api.CloneDefaults;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.api.InstallationWriteLeader;
import net.bluemind.core.backup.continuous.store.ITopicStore;
import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;
import net.bluemind.core.backup.continuous.store.TopicNames;
import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;

public class BackupStoreFactory implements IBackupStoreFactory {

	private static final Logger logger = LoggerFactory.getLogger(BackupStoreFactory.class);

	private static final File CLONE_MARKER = new File(CloneDefaults.MARKER_FILE_PATH);

	private final TopicNames names;
	private final ITopicStore topicStore;

	private final Supplier<InstallationWriteLeader> election;

	public BackupStoreFactory(ITopicStore topicStore, Supplier<InstallationWriteLeader> election) {
		String iid = InstallationId.getIdentifier();
		this.election = election;
		this.names = new TopicNames(iid);
		this.topicStore = topicStore;
	}

	private boolean disabledFromSystemPropperty() {
		return Optional.ofNullable(System.getProperty("backup.continuous.store.disabled")).map(s -> s.equals("true"))
				.orElse(false);
	}

	@Override
	public <T> IBackupStore<T> forContainer(BaseContainerDescriptor c) {
		TopicDescriptor descriptor = names.forContainer(c);
		TopicPublisher publisher = publisher(descriptor);
		TopicSerializer<RecordKey, ItemValue<T>> serializer = new ItemValueSerializer<>();
		return new BackupStore<>(publisher, descriptor, serializer);
	}

	private TopicPublisher publisher(TopicDescriptor descriptor) {
		return isNoop(descriptor) ? NoopStore.NOOP.getPublisher(descriptor) : topicStore.getPublisher(descriptor);
	}

	private boolean isNoop(TopicDescriptor descriptor) {
		boolean ret = !election.get().isLeader() || CLONE_MARKER.exists() || disabledFromSystemPropperty()
				|| "global.virt".equals(descriptor.domainUid());
		if (logger.isDebugEnabled() && ret) {
			logger.debug("noop for {}", descriptor);
		}
		return ret;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(DefaultBackupStore.class).add("topicStore", topicStore)
				.add("clone", CLONE_MARKER.exists()).toString();
	}

	@Override
	public InstallationWriteLeader leadership() {
		return election.get();
	}

}