package net.bluemind.dataprotect.postgresql;

import net.bluemind.dataprotect.postgresql.internal.AbstractPgWorker;

public class PgWorker extends AbstractPgWorker {

	@Override
	public boolean supportsTag(String tag) {
		return "bm/pgsql".equals(tag);
	}

	@Override
	protected String getBackupDirectory() {
		return "/var/backups/bluemind/work/pgsql";
	}

}
