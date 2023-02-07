package net.bluemind.core.container.service.internal;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.persistence.ChangesetCleanupStore;
import net.bluemind.core.container.service.IChangesetCleanup;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.jdbc.JdbcAbstractStore.SqlOperation;

public class ChangesetCleanupService implements IChangesetCleanup {

	private static final Logger logger = LoggerFactory.getLogger(ChangesetCleanupService.class);

	private ChangesetCleanupStore changetsetCleanupStore;
	private String serverUid;

	public ChangesetCleanupService(DataSource pool, String serverUid) {
		this.changetsetCleanupStore = new ChangesetCleanupStore(pool);
		this.serverUid = serverUid;
	}

	public <W> W doOrFail(SqlOperation<W> op) {
		return JdbcAbstractStore.doOrFail(op);
	}

	@Override
	public void deleteOldDeletedChangesetItems(int days) {
		JdbcAbstractStore.doOrFail(() -> {
			logger.info("Expiring changeset old items ({} days) on server {}", days, serverUid);
			try {
				long deletedItems = changetsetCleanupStore.deleteExpiredItems(days);
				logger.info("Expired {} changeset old items", deletedItems);
			} catch (SQLException e) {
				logger.error("Error cleaning up changeset items  {}", e.getMessage());
			}
			return null;
		});
	}

}
