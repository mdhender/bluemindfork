/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.calendar.service.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryHandler;

public class DomainCalendarDirEntryHandler extends DirEntryHandler {
	private static final Logger logger = LoggerFactory.getLogger(DomainCalendarDirEntryHandler.class);
	public static byte[] DEFAULT_ICON = null;

	public static void load(Bundle bundle) {
		try (InputStream in = bundle.getResource("data/calendar.png").openStream()) {
			DEFAULT_ICON = IOUtils.toByteArray(in);
		} catch (IOException e) {
			logger.error("error loading image", e);
		}
	}

	@Override
	public Kind kind() {
		return DirEntry.Kind.CALENDAR;
	}

	@Override
	public TaskRef entryDeleted(BmContext context, String domainUid, String entryUid) throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(monitor -> {
			context.provider().instance(ICalendarsMgmt.class).delete(entryUid);
		});

	}

	@Override
	public byte[] getIcon(BmContext context, String domainUid, String uid) throws ServerFault {
		return DEFAULT_ICON;
	}

}
