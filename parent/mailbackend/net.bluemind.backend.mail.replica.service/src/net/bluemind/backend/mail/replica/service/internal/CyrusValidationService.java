package net.bluemind.backend.mail.replica.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.backend.mail.replica.api.ICyrusValidation;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class CyrusValidationService implements ICyrusValidation {
	private static final Logger logger = LoggerFactory.getLogger(CyrusValidationService.class);

	@Override
	public boolean prevalidate(String mailbox, String partition) {
		logger.info("Cyrus Validation Service - prevalidate {}/{}", partition, mailbox);
		if (Strings.isNullOrEmpty(mailbox)) {
			throw new ServerFault("Null or empty mailbox name", ErrorCode.INVALID_MAILBOX_NAME);
		}
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
