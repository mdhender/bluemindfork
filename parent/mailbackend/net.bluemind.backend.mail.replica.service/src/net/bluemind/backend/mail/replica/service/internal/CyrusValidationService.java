package net.bluemind.backend.mail.replica.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.ICyrusValidation;

public class CyrusValidationService implements ICyrusValidation {
	private static final Logger logger = LoggerFactory.getLogger(CyrusValidationService.class);

	@Override
	public boolean prevalidate(String mailbox, String partition) {
		logger.info("Cyrus Validation Service - prevalidate {}/{}", partition, mailbox);
		boolean result = true;
		switch (mailbox) {
		case "default":
			result = false;
			break;
		default:
			result = true;
			break;
		}
		return result;
	}

}
