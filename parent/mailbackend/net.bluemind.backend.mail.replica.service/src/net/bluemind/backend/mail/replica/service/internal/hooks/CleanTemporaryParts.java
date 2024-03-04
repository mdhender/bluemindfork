package net.bluemind.backend.mail.replica.service.internal.hooks;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.parsing.Bodies;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.sessions.ISessionDeletionListener;

public class CleanTemporaryParts implements ISessionDeletionListener {

	private static final Logger logger = LoggerFactory.getLogger(CleanTemporaryParts.class);

	@Override
	public void deleted(String identity, String sid, SecurityContext securityContext) {
		File sidFolder = Bodies.getFolder(sid);

		File[] parts = sidFolder.listFiles();
		if (parts != null) {
			for (File part : parts) {
				part.delete();
			}
		}
		sidFolder.delete();

		logger.debug("Folder {} deleted.", sidFolder.getAbsolutePath());
	}
}
