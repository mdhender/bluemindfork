package net.bluemind.dataprotect.postgresql;

import net.bluemind.dataprotect.postgresql.internal.AbstractPgWorker;
import net.bluemind.server.api.TagDescriptor;

public class PgWorker extends AbstractPgWorker {
	@Override
	public boolean supportsTag(String tag) {
		return TagDescriptor.bm_pgsql.getTag().equals(tag);
	}

	@Override
	protected String getBackupDirectory() {
		return "/var/backups/bluemind/work/pgsql";
	}
}
