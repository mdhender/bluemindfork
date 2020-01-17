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
package net.bluemind.mailbox.hook.acl;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.hooks.AbstractEmailHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class EmailHook extends AbstractEmailHook {

	private static final Logger logger = LoggerFactory.getLogger(EmailHook.class);
	private String subjectTpl;
	private String bodyTpl;

	public EmailHook() {
		super();
	}

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {

		if (IMailboxAclUids.TYPE.equals(container.type)) {
			try {
				String mboxUid = container.uid.substring(IMailboxAclUids.MAILBOX_ACL_PREFIX.length());
				IDomains domainApi = context.su().provider().instance(IDomains.class, InstallationId.getIdentifier());
				ItemValue<Mailbox> boxItem = null;
				for (ItemValue<Domain> d : domainApi.all()) {
					IMailboxes mboxApi = context.su().provider().instance(IMailboxes.class, d.uid);
					boxItem = mboxApi.getComplete(mboxUid);
					if (boxItem != null) {
						break;
					}
				}
				if (boxItem != null) {
					RawField rf = new RawField("X-BM-MailboxSharing", mboxUid);
					this.subjectTpl = null;
					this.bodyTpl = null;
					switch (boxItem.value.type) {
					case mailshare:
					case user:
						this.subjectTpl = "MailshareSubject.ftl";
						this.bodyTpl = "MailshareBody.ftl";
						break;
					default:
						break;

					}

					if (this.subjectTpl != null) {
						List<AccessControlEntry> listCopy = new ArrayList<>(current);
						listCopy.removeAll(previous);
						notify(context, container, listCopy, rf);
					}
				}
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	protected String getTemplateSubject() {
		return this.subjectTpl;
	}

	@Override
	protected String getTemplateBody() {
		return this.bodyTpl;
	}
}
