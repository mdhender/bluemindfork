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
package net.bluemind.todolist.hook.acl;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.hooks.AbstractEmailHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.todolist.api.ITodoUids;

public class TodolistEmailHook extends AbstractEmailHook {

	private static final Logger logger = LoggerFactory.getLogger(TodolistEmailHook.class);

	public TodolistEmailHook() {
		super();
	}

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		if (ITodoUids.TYPE.equals(container.type)) {
			try {
				RawField uid = new RawField("X-BM-FolderUid", container.uid);
				RawField type = new RawField("X-BM-FolderType", container.type);

				List<AccessControlEntry> added = new ArrayList<>(current);
				added.removeAll(previous);

				notify(context, container, added, uid, type);

			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	@Override
	protected String getTemplateSubject() {
		return "TodolistSubject.ftl";
	}

	@Override
	protected String getTemplateBody() {
		return "TodolistBody.ftl";
	}

}
