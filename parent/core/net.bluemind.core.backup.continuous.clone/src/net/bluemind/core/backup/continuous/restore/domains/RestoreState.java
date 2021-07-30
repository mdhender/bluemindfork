package net.bluemind.core.backup.continuous.restore.domains;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DBMaker.Maker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.backup.continuous.restore.mbox.UidDatalocMapping;
import net.bluemind.core.backup.continuous.restore.mbox.UidDatalocMapping.Replica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Server;

public class RestoreState implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(RestoreMailboxRecords.class);

	private final Map<String, ItemValue<Server>> serverByDatalocation;
	private Map<String, ItemValue<Mailbox>> mboxesByUid;
	private final DB handlesBackingStore;
	private final HTreeMap<String, Integer> bodies;
	private final UidDatalocMapping locationMapping;

	public RestoreState(String domainUid, Map<String, ItemValue<Server>> topology) {
		this.serverByDatalocation = topology;
		this.locationMapping = new UidDatalocMapping();
		this.mboxesByUid = new HashMap<>();
		this.handlesBackingStore = buildDb(domainUid);
		this.bodies = handlesBackingStore.hashMap("bodies-" + domainUid).keySerializer(Serializer.STRING_ASCII)
				.valueSerializer(Serializer.INTEGER).createOrOpen();
	}

	public ItemValue<Server> getServer(String dataLocation) {
		return serverByDatalocation.get(dataLocation);
	}

	public Replica storeReplica(ItemValue<Domain> domain, ItemValue<Mailbox> mbox, ItemValue<MailboxReplica> replica,
			CyrusPartition partition) {
		return locationMapping.put(replica, mbox, domain, partition);
	}

	public Replica getReplica(String uniqueId) {
		return locationMapping.get(uniqueId);
	}

	public void storeMailbox(String userUid, ItemValue<Mailbox> mailbox) {
		this.mboxesByUid.put(userUid, mailbox);
	}

	public ItemValue<Mailbox> getMailbox(String userUid) {
		return mboxesByUid.get(userUid);
	}

	public boolean containsBody(String guid) {
		return bodies.containsKey(guid);
	}

	public void storeBodySize(String guid, int size) {
		bodies.put(guid, size);
	}

	public int getBodySize(String guid) {
		return bodies.get(guid);
	}

	private DB buildDb(String domainUid) {
		Path tmp = null;
		try {
			tmp = Files.createTempFile(domainUid, ".mapdb");
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

	@Override
	public void close() throws IOException {
		handlesBackingStore.close();
	}
}
