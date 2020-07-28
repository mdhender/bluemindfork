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
package net.bluemind.backend.postfix;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.postfix.internal.maps.events.EventProducer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.server.hook.DefaultServerHook;

public class SmtpTagServerHook extends DefaultServerHook {
	private static final Logger logger = LoggerFactory.getLogger(SmtpTagServerHook.class);

	static final Set<String> TAGS = new HashSet<>(
			Arrays.asList(TagDescriptor.mail_smtp.getTag(), TagDescriptor.mail_smtp_edge.getTag()));

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (!TAGS.contains(tag)) {
			return;
		}

		logger.info("server {}:{} tagged as {}, initialize postfix configuration", server.uid, server.value.address(),
				tag);

		// initialize basic postfix conf
		PostfixService service = new PostfixService();
		service.initializeServer(server.uid, tag);
	}

	@Override
	public void onServerAssigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> assignedDomain,
			String tag) throws ServerFault {
		if (!TAGS.contains(tag)) {
			return;
		}

		logger.info("server {}:{} assigned to domain {} as {}", server.uid, server.value.address(), assignedDomain,
				tag);

		EventProducer.dirtyMaps();
	}

	@Override
	public void onServerUnassigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> assignedDomain,
			String tag) throws ServerFault {
		if (!TAGS.contains(tag)) {
			return;
		}

		logger.info("server {}:{} unassigned from domain {} as {}", server.uid, server.value.address(), assignedDomain,
				tag);

		EventProducer.dirtyMaps();
	}
}
