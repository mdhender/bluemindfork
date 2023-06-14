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
package net.bluemind.backend.mail.replica.service.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.message.MultipartImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.utils.PartsWalker;
import net.bluemind.backend.mail.parsing.Bodies;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;

public class MailboxItemDecomposer {

	private static final Logger logger = LoggerFactory.getLogger(MailboxItemDecomposer.class);
	private final BmContext context;
	private Container container;

	public MailboxItemDecomposer(BmContext context, Container cnt) {
		this.context = context;
		this.container = cnt;
	}

	/**
	 * Parts with imap addresses will be changed to temporary parts
	 *
	 * @param bodyGuid base EML to use as source for imap addresses
	 * @param rootPart the new part tree
	 * @return
	 */
	public void decomposeToTempParts(String bodyGuid, Part rootPart) {
		logger.debug("Decomposing parts into tmp files for EML (guid={})", bodyGuid);
		String loc = DataSourceRouter.location(context, container.uid);
		MessageBodyObjectStore bodyStore = new MessageBodyObjectStore(context, loc);
		ByteBuf loadedBody = bodyStore.openMmap(bodyGuid);
		try (Message msg = Mime4JHelper.parse(new ByteBufInputStream(loadedBody, true))) {
			Body body = msg.getBody();
			List<AddressableEntity> withAddr = (body instanceof MultipartImpl mp)
					? Mime4JHelper.expandTree(mp.getBodyParts())
					: Collections.singletonList(new AddressableEntity(body.getParent(), "1"));
			Map<String, SingleBody> indexed = withAddr.stream().filter(ae -> ae.getBody() instanceof SingleBody)
					.collect(Collectors.toMap(AddressableEntity::getMimeAddress, v -> (SingleBody) v.getBody()));
			PartsWalker<Message> walker = new PartsWalker<>(msg);
			walker.visit((Message c, Part p) -> {
				if (p.address != null && isImapAddress(p.address) && !p.mime.startsWith("multipart/")
						&& indexed.containsKey(p.address)) {
					String replacedPartUid = UUID.randomUUID().toString();
					SingleBody toRead = indexed.get(p.address);
					try (InputStream in = toRead.getInputStream()) {
						Files.copy(in, partFile(replacedPartUid).toPath());
						p.address = replacedPartUid;
					} catch (IOException e) {
						throw new ServerFault(e);
					}
				}
			}, rootPart);
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private boolean isImapAddress(String address) {
		return address.equals("TEXT") || address.equals("HEADER")
				|| CharMatcher.inRange('0', '9').or(CharMatcher.is('.')).matchesAllOf(address);
	}

	private File partFile(String partId) {
		String sid = context.getSecurityContext().getSessionId();
		return new File(Bodies.getFolder(sid), partId + ".part");
	}

}
