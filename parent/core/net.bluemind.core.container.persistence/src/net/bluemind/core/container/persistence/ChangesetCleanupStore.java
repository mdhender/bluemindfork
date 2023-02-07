package net.bluemind.core.container.persistence;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.sql.DataSource;

import net.bluemind.core.jdbc.JdbcAbstractStore;

public class ChangesetCleanupStore extends JdbcAbstractStore {

	public ChangesetCleanupStore(DataSource dataSource) {
		super(dataSource);
	}

	public long deleteExpiredItems(int days) throws SQLException {
		Instant dateInSeconds = Instant.now().minus(days, ChronoUnit.DAYS);
		String deleteQuery = "DELETE FROM q_changeset_cleanup WHERE date <= ?";
		return delete(deleteQuery, new Object[] { Timestamp.from(dateInSeconds) });
	}

}
