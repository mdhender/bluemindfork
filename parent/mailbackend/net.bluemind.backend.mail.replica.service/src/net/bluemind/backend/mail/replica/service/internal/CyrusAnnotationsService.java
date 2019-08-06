/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotations;
import net.bluemind.backend.mail.replica.api.MailboxAnnotation;
import net.bluemind.backend.mail.replica.persistence.AnnotationStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;

public class CyrusAnnotationsService implements ICyrusReplicationAnnotations {

	private static final Logger logger = LoggerFactory.getLogger(CyrusAnnotationsService.class);
	private final AnnotationStore annoStore;
	private final BmContext context;

	public CyrusAnnotationsService(BmContext context, AnnotationStore annoStore) {
		this.annoStore = annoStore;
		this.context = context;
		logger.debug("Created in ctx {}", this.context);
	}

	@Override
	public void storeAnnotation(MailboxAnnotation ss) {
		try {
			annoStore.store(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void deleteAnnotation(MailboxAnnotation ss) {
		try {
			annoStore.delete(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public List<MailboxAnnotation> annotations(String mbox) {
		try {
			return annoStore.byMailbox(mbox);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

}
