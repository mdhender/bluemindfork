package net.bluemind.core.backup.continuous.dto;

import java.util.HashMap;
import java.util.Map;

public class ContainerItemIdSeq {

	public Long defaultDataSourceSeq = 0L;
	public Map<String, Long> mailboxDataSourceSeq;

	public ContainerItemIdSeq() {
		this.mailboxDataSourceSeq = new HashMap<>();
	}
}
