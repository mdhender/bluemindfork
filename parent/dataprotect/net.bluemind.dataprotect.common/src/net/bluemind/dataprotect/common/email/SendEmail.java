/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.dataprotect.common.email;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.util.MimeUtil;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.container.model.ContainerUpdatesResult.InError;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.common.restore.RestoreRestorableItem;
import net.bluemind.dataprotect.service.action.EmailData;
import net.bluemind.dataprotect.service.action.IRestoreActionData;
import net.bluemind.dataprotect.service.action.RecipientHelper;
import net.bluemind.dataprotect.service.action.RestoreAction;
import net.bluemind.dataprotect.service.action.RestoreActionExecutor;

public abstract class SendEmail {

	protected RestoreRestorableItem item;

	Restorable restorable() {
		return item.item;
	}

	RestoreActionExecutor<EmailData> executor() {
		return item.executor;
	}

	public String entryUid() {
		return item.entryUid();
	}

	public String domain() {
		return item.domain();
	}

	BmContext context() {
		return executor().context;
	}

	IServerTaskMonitor monitor() {
		return item.monitor;
	}

	public SendEmail(Restorable item, RestoreActionExecutor<EmailData> executor, IServerTaskMonitor monitor) {
		this.item = new RestoreRestorableItem(item, executor, monitor);
	}

	public void sendMessage(Map<String, String> allElements, String body, String subject) throws Exception {
		List<String> emails = RecipientHelper.getRecipientList(context(), restorable());
		if (emails.isEmpty()) {
			InError error = InError.create("don't known where to send restore result", ErrorCode.NOT_FOUND,
					item.entryUid());
			item.errors.add(error);
			return;
		}

		Mailbox sender = RecipientHelper.createNotReplyMailbox(item.domain());
		emails.forEach(e -> {
			try {
				Mailbox to = SendmailHelper.formatAddress(e.split("@")[0], e);
				try (Message m = getMessage(sender, to, allElements, body, subject)) {
					RestoreAction<IRestoreActionData> restoreAction = new RestoreAction<>(RestoreAction.Type.EMAIL,
							new EmailData(sender, m));
					executor().accept(restoreAction);
				}
			} catch (Exception ex) {
				InError error = InError.create(ex.getMessage(), null, item.entryUid());
				item.errors.add(error);
			}
		});
	}

	protected abstract String getMimeType();

	protected abstract String getExtension();

	protected Message getMessage(Mailbox from, Mailbox to, Map<String, String> allElements, String body,
			String subject) {
		monitor().log("Send message to {}", to.getAddress());

		MessageImpl mi = new MessageImpl();
		MultipartImpl mp = new MultipartImpl("mixed");
		BasicBodyFactory bbf = new BasicBodyFactory();
		TextBody tb = bbf.textBody(body, StandardCharsets.UTF_8);
		BodyPart textPart = new BodyPart();
		textPart.setBody(tb, "text/plain");
		textPart.setContentTransferEncoding(MimeUtil.ENC_QUOTED_PRINTABLE);
		mp.addBodyPart(textPart);

		for (String element : allElements.keySet()) {
			BodyPart bodyPart = new BodyPart();
			BinaryBody ib = bbf.binaryBody(allElements.get(element).getBytes());
			bodyPart.setBody(ib, getMimeType());
			bodyPart.setContentDisposition("attachment", element + getExtension());
			bodyPart.setContentTransferEncoding(MimeUtil.ENC_BASE64);
			mp.addBodyPart(bodyPart);
		}

		mi.setMultipart(mp);
		mi.setSubject(subject);
		mi.setFrom(from);
		mi.setTo(to);
		mi.setDate(new Date());
		return mi;

	}

	public void endTask() {
		item.endTask();
	}

	public RestoreRestorableItem getRestorableItem() {
		return item;
	}

}
