package net.bluemind.core.backup.continuous.dto;

public class IndexedMessageBodyDTO {

	public byte[] data;

	public IndexedMessageBodyDTO() {

	}

	public IndexedMessageBodyDTO(byte[] raw) {
		this.data = raw;
	}

}
