package net.bluemind.core.backup.continuous.restore.domains;

import net.bluemind.core.backup.continuous.RecordKey;

public interface RestoreDomainType {

	String type();

	void restore(RecordKey key, String payload);
}
