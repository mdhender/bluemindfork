package net.bluemind.attachment.service;

import java.util.Collections;
import java.util.Set;

import net.bluemind.backend.mail.parsing.HeaderList;

public class AttachmentHeaders implements HeaderList {

	@Override
	public Set<String> getWhiteList() {
		return Collections.singleton("X-Mozilla-Cloud-Part");
	}

}
