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
package net.bluemind.eas.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.commons.logs.MdcLogUser;

public class EasLogUser extends MdcLogUser {

	public static final String ANONYMOUS = "anonymous";
	private static final Logger logger = LoggerFactory.getLogger(EasLogUser.class);

	private EasLogUser(String user, Logger log) {
		super(user, log);
	}

	public static void logErrorAsUser(String user, Logger log, String format, Object... arguments) {
		try (EasLogUser l = new EasLogUser(user, log)) {
			l.logErrorWithMdc(null, format, arguments);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public static void logDebugAsUser(String user, Logger log, String format, Object... arguments) {
		try (EasLogUser l = new EasLogUser(user, log)) {
			l.logDebugWithMdc(format, arguments);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public static void logInfoAsUser(String user, Logger log, String format, Object... arguments) {
		try (EasLogUser l = new EasLogUser(user, log)) {
			l.logInfoWithMdc(format, arguments);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public static void logWarnAsUser(String user, Logger log, String format, Object... arguments) {
		try (EasLogUser l = new EasLogUser(user, log)) {
			l.logWarnWithMdc(format, arguments);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public static void logExceptionAsUser(String user, Throwable e, Logger log) {
		try (EasLogUser l = new EasLogUser(user, log)) {
			l.logExceptionWithMdc(e);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public static void logErrorExceptionAsUser(String user, Throwable e, Logger log, String format,
			Object... arguments) {
		try (EasLogUser l = new EasLogUser(user, log)) {
			l.logErrorExceptionWithMdc(e, format, arguments);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	private void logDebugWithMdc(String format, Object... arguments) {
		open();
		logDebug(format, arguments);
	}

	private void logWarnWithMdc(String format, Object... arguments) {
		open();
		logWarn(format, arguments);
	}

	private void logExceptionWithMdc(Throwable e) {
		open();
		logError(e, null);
	}

	private void logErrorExceptionWithMdc(Throwable e, String format, Object... arguments) {
		open();
		logError(e, format, arguments);
	}
}
