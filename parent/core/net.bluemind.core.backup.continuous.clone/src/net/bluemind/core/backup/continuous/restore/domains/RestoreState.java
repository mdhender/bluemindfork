package net.bluemind.core.backup.continuous.restore.domains;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DBMaker.Maker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.backup.continuous.restore.orphans.RestoreTopology.PromotingServer;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Server;

public class RestoreState implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(RestoreState.class);

	private final Map<String, PromotingServer> serverByDatalocation;
	private Map<String, ItemValue<Mailbox>> mboxesByUid;
	private Map<String, String> globalUidsMapStoreToDb;
	private final DB handlesBackingStore;
	private final HTreeMap<String, Integer> bodies;

	public RestoreState(String domainUid, Map<String, PromotingServer> topology) {
		this.serverByDatalocation = topology;
		this.mboxesByUid = new ConcurrentHashMap<>();
		this.globalUidsMapStoreToDb = new ConcurrentHashMap<>();
		this.handlesBackingStore = buildDb(domainUid);
		this.bodies = handlesBackingStore.hashMap("bodies-" + domainUid).keySerializer(Serializer.STRING_ASCII)
				.valueSerializer(Serializer.INTEGER).createOrOpen();
	}

	public void mapUid(String storeUid, String targetUid) {
		globalUidsMapStoreToDb.put(storeUid, targetUid);
	}

	public String uidAlias(String storeUid) {
		return globalUidsMapStoreToDb.getOrDefault(storeUid, storeUid);
	}

	public ItemValue<Server> getServer(String dataLocation) {
		return serverByDatalocation.get(dataLocation).clone;
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
