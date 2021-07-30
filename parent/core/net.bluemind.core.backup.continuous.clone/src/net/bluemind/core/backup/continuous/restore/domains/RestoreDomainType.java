package net.bluemind.core.backup.continuous.restore.domains;

import net.bluemind.core.backup.continuous.DataElement;

public interface RestoreDomainType {

	String type();

	void restore(DataElement de);
}
