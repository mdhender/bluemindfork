/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.commons.logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class MdcLogUser implements IMdcLogUser {

	public static final String ANONYMOUS = "anonymous";
	private static final Logger logger = LoggerFactory.getLogger(MdcLogUser.class);

	protected String user;
	protected Logger log;

	public MdcLogUser() {
	}

	protected MdcLogUser(String user, Logger log) {
		this.user = user;
		this.log = log;
	}

	@Override
	public void logInfoWithMdc(String format, Object... arguments) {
		open();
		logInfo(format, arguments);
	}

	@Override
	public void logErrorWithMdc(Throwable e, String format, Object... arguments) {
		open();
		logError(e, format, arguments);
	}

	@Override
	public void close() throws Exception {
		MDC.put("user", ANONYMOUS);
	}

	protected void open() {
		MDC.put("user", user.replace("@", "_at_"));
	}

	public static void logInfoAsUser(String user, Logger log, String format, Object... arguments) {
		try (MdcLogUser l = new MdcLogUser(user, log)) {
			l.logInfoWithMdc(format, arguments);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public static void logErrorAsUser(String user, Logger log, String format, Object... arguments) {
		try (MdcLogUser l = new MdcLogUser(user, log)) {
			l.logErrorWithMdc(null, format, arguments);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	protected void logError(Throwable e, String format, Object... arguments) {
		if (e != null && format != null) {
			log.error(format, arguments, e);
		} else if (e == null && format != null) {
			log.error(format, arguments);
		} else if (e != null && format == null) {
			log.error(e.getMessage(), e);
		}
	}

	protected void logDebug(String format, Object... arguments) {
		log.debug(format, arguments);
	}

	protected void logInfo(String format, Object... arguments) {
		log.info(format, arguments);
	}

	protected void logWarn(String format, Object... arguments) {
		log.warn(format, arguments);
	}

}
