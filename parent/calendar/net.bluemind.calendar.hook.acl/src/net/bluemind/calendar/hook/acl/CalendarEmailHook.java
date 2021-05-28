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
package net.bluemind.calendar.hook.acl;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.hooks.AbstractEmailHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.videoconferencing.api.IVideoConferenceUids;

public class CalendarEmailHook extends AbstractEmailHook {

	private static final Logger logger = LoggerFactory.getLogger(CalendarEmailHook.class);

	private String templateSubject = "CalendarSubject.ftl";
	private String templateBody = "CalendarBody.ftl";

	public CalendarEmailHook() {
		super();
	}

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		if (!ICalendarUids.TYPE.equals(container.type)) {
			return;
		}

		boolean addHeaders = true;
		IDirectory dirService = context.getServiceProvider().instance(IDirectory.class, container.domainUid);
		DirEntry owner = dirService.findByEntryUid(container.owner);
		if (owner.kind == Kind.RESOURCE) {
			IResources resourceService = context.getServiceProvider().instance(IResources.class, container.domainUid);
			ResourceDescriptor res = resourceService.get(owner.entryUid);

			if (IVideoConferenceUids.RESOURCETYPE_UID.equals(res.typeIdentifier)) {
				templateSubject = "VideoConferenceSubject.ftl";
				templateBody = "VideoConferenceBody.ftl";
				addHeaders = false;
			}

		}

		try {

			List<AccessControlEntry> added = new ArrayList<>(current);
			added.removeAll(previous);

			if (addHeaders) {
				RawField uid = new RawField("X-BM-FolderUid", container.uid);
				RawField type = new RawField("X-BM-FolderType", container.type);
				notify(context, container, added, uid, type);
			} else {
				notify(context, container, added);
			}

		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	protected String getTemplateSubject() {
		return templateSubject;
	}

	@Override
	protected String getTemplateBody() {
		return templateBody;
	}

}
