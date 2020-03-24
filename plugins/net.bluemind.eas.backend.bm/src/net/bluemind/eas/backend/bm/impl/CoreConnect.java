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
package net.bluemind.eas.backend.bm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.backend.bm.state.InternalState;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.vertx.common.http.BasicAuthHandler;

public class CoreConnect {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private IServiceProvider provider(BackendSession bs) {
		InternalState in = validateSid(bs);
		return ClientSideServiceProvider.getProvider(in.coreUrl, in.sid)
				.withRemoteIps(bs.getRequest().headers().getAll("X-Forwarded-For"))
				.setOrigin("bm-eas-" + bs.getDevId());
	}

	private InternalState validateSid(BackendSession bs) {
		InternalState in = bs.getInternalState();
		String auth = bs.getRequest().headers().get("Authorization");
		if (null != auth) {
			try {
				String sid = BasicAuthHandler.getSid(auth);
				in.sid = sid;
			} catch (NullPointerException e) {
				logger.warn("Session for auth key {} does not exist", auth);
			}
		}
		return in;
	}

	public IMailboxFolders getIMailboxFoldersService(BackendSession bs) {
		CyrusPartition part = CyrusPartition.forServerAndDomain(bs.getUser().getDataLocation(),
				bs.getUser().getDomain());
		return provider(bs).instance(IMailboxFolders.class, part.name,
				"user." + bs.getUser().getUid().replace('.', '^'));
	}

	public IMailboxItems getMailboxItemsService(BackendSession bs, String mailboxUid) {
		return provider(bs).instance(IMailboxItems.class, mailboxUid);
	}

	/**
	 * @param bs
	 * @param containerUid
	 * @return
	 * @throws ServerFault
	 */
	public ICalendar getCalendarService(BackendSession bs, String containerUid) throws ServerFault {
		return provider(bs).instance(ICalendar.class, containerUid);
	}

	/**
	 * @param bs
	 * @param containerUid
	 * @return
	 * @throws ServerFault
	 */
	public IAddressBook getAddressbookService(BackendSession bs, String containerUid) throws ServerFault {
		return provider(bs).instance(IAddressBook.class, containerUid);
	}

	public <T> T getService(BackendSession bs, Class<T> klass, String... params) throws ServerFault {
		return provider(bs).instance(klass, params);
	}

	/**
	 * @param coreUrl
	 * @param token
	 * @param klass
	 * @param params
	 * @return
	 * @throws ServerFault
	 */
	public <T> T getService(String coreUrl, String token, Class<T> klass, String... params) throws ServerFault {
		return ClientSideServiceProvider.getProvider(coreUrl, token).setOrigin("bm-eas").instance(klass, params);
	}

	/**
	 * @param <T>
	 * @param bs
	 * @param klass
	 * @param params
	 * @return
	 * @throws ServerFault
	 */
	public <T> T getAdmin0Service(BackendSession bs, Class<T> klass, String... params) throws ServerFault {
		InternalState in = validateSid(bs);
		return ClientSideServiceProvider.getProvider(in.coreUrl, Token.admin0()).setOrigin("bm-eas").instance(klass,
				params);
	}

	/**
	 * @param bs
	 * @param containerUid
	 * @return
	 * @throws ServerFault
	 */
	public ITodoList getTodoListService(BackendSession bs, String containerUid) throws ServerFault {
		return provider(bs).instance(ITodoList.class, containerUid);
	}

	protected ItemChangeReference getItemChange(CollectionId collectionId, String uid, ItemDataType type,
			ChangeType changeType) {
		ItemChangeReference ret = new ItemChangeReference(type);
		ret.setChangeType(changeType);
		ret.setServerId(CollectionItem.of(collectionId, uid));
		return ret;
	}

	/**
	 * returns collectionId:clientId
	 * 
	 * @param collectionId
	 * @param clientId
	 * @return
	 * @throws ActiveSyncException
	 */
	public String getServerId(long collectionId, String clientId) throws ActiveSyncException {
		StringBuilder sb = new StringBuilder(10);
		sb.append(collectionId);
		if (clientId != null) {
			sb.append(':');
			sb.append(clientId);
		}
		return sb.toString();
	}

	/**
	 * returns itemUid from colletionId:itemUid
	 * 
	 * @param serverId
	 * @return
	 */
	protected String getItemUid(String serverId) {
		if (serverId == null || serverId.isEmpty()) {
			return null;
		}
		int idx = serverId.indexOf(':');
		return serverId.substring(idx + 1);
	}

}
