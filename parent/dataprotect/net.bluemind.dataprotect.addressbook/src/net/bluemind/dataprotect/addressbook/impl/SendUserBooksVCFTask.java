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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.dataprotect.addressbook.impl;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class SendUserBooksVCFTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(SendUserBooksVCFTask.class);
	private DataProtectGeneration backup;
	private Restorable item;

	public SendUserBooksVCFTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	public static final SecurityContext as(String uid, String domainContainerUid) throws ServerFault {
		SecurityContext userContext = new SecurityContext(UUID.randomUUID().toString(), uid, Arrays.<String>asList(),
				Arrays.<String>asList(), Collections.emptyMap(), domainContainerUid, "en", "RestoreCalendarsTask.as");
		Sessions.get().put(userContext.getSessionId(), userContext);
		return userContext;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(10, "starting restore for uid " + item.entryUid);

		SecurityContext userContext = as(item.entryUid, item.domainUid);
		try (BackupDataProvider bdp = new BackupDataProvider(null, userContext, monitor)) {
			IServiceProvider back = bdp.createContextWithData(backup, item).provider();
			IServiceProvider live = ServerSideServiceProvider.getProvider(userContext);

			IContainers containersService = live.instance(IContainers.class);
			ContainerQuery cq = ContainerQuery.ownerAndType(userContext.getSubject(), IAddressBookUids.TYPE);
			List<ContainerDescriptor> books = containersService.all(cq);

			Map<String, String> allVCards = new HashMap<String, String>(books.size());
			for (ContainerDescriptor b : books) {
				IVCardService service = back.instance(IVCardService.class, b.uid);
				allVCards.put(b.name, service.exportAll());
			}

			IUser userService = live.instance(IUser.class, item.domainUid);
			ItemValue<User> user = userService.getComplete(item.entryUid);

			try {
				Mailbox sender = SendmailHelper.formatAddress("no-reply@" + item.domainUid,
						"no-reply@" + item.domainUid);
				Mailbox to = SendmailHelper.formatAddress(user.value.contactInfos.identification.formatedName.value,
						user.value.defaultEmail().address);
				try (Message m = getMessage(sender, to, allVCards)) {
					Sendmail mailer = new Sendmail();
					mailer.send(sender, m);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

		}

		monitor.end(true, "finished.", "[]");
	}

	private Message getMessage(Mailbox sender, Mailbox to, Map<String, String> allVCards) {
		MessageImpl mi = new MessageImpl();
		MultipartImpl mp = new MultipartImpl("mixed");
		BasicBodyFactory bbf = new BasicBodyFactory();
		TextBody tb = bbf.textBody("Une sauvegarde de vos carnets d'adresses est attachée à ce message",
				Charset.forName("utf-8"));
		BodyPart textPart = new BodyPart();
		textPart.setBody(tb, "text/plain");
		textPart.setContentTransferEncoding(MimeUtil.ENC_QUOTED_PRINTABLE);
		mp.addBodyPart(textPart);

		for (String book : allVCards.keySet()) {
			BodyPart icsPart = new BodyPart();
			BinaryBody ib = bbf.binaryBody(allVCards.get(book).getBytes());
			icsPart.setBody(ib, "text/vcard");
			icsPart.setContentDisposition("attachment", book + ".vcf");
			icsPart.setContentTransferEncoding(MimeUtil.ENC_BASE64);
			mp.addBodyPart(icsPart);
		}

		mi.setMultipart(mp);
		mi.setSubject("Mes carnets d'adresses");
		mi.setFrom(sender);
		mi.setTo(to);
		mi.setDate(new Date());
		return mi;
	}

}
