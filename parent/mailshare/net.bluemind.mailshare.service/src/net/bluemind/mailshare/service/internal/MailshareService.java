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
package net.bluemind.mailshare.service.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.utils.ImageUtils;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.service.DirDomainValue;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirEventProducer;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.IInCoreMailboxes;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.mailshare.hook.IMailshareHook;
import net.bluemind.role.api.BasicRoles;

public class MailshareService implements IMailshare {

	private String domainUid;
	private BmContext context;

	private IInCoreMailboxes mailboxes;

	private Sanitizer extSanitizer;
	private Validator validator;
	private List<IMailshareHook> hooks;
	private RBACManager rbacManager;
	private MailshareMailboxAdapter mailboxAdapter;
	private ContainerMailshareStoreService storeService;
	private DirEventProducer dirEventProducer;

	public MailshareService(BmContext context, Container container, ItemValue<Domain> domain,
			List<IMailshareHook> hooks) throws ServerFault {
		this.context = context;
		this.domainUid = domain.uid;
		this.mailboxes = context.su().provider().instance(IInCoreMailboxes.class, domainUid);
		this.hooks = hooks;
		extSanitizer = new Sanitizer(context);
		validator = new Validator(context);
		rbacManager = new RBACManager(context).forDomain(domainUid);
		this.mailboxAdapter = new MailshareMailboxAdapter();
		this.storeService = new ContainerMailshareStoreService(context, container, domain);
		dirEventProducer = new DirEventProducer(domainUid, BaseDirEntry.Kind.MAILSHARE.name(),
				VertxPlatform.eventBus());
	}

	@Override
	public void create(String uid, Mailshare mailshare) throws ServerFault {
		ItemValue<Mailshare> itemValue = ItemValue.create(uid, mailshare);
		createWithItem(itemValue);
	}

	private void createWithItem(ItemValue<Mailshare> mailshareItem) throws ServerFault {
		String uid = mailshareItem.uid;
		Mailshare mailshare = mailshareItem.value;
		rbacManager.forOrgUnit(mailshare.orgUnitUid).check(BasicRoles.ROLE_MANAGE_MAILSHARE);

		extSanitizer.create(mailshare);
		extSanitizer.create(new DirDomainValue<>(domainUid, uid, mailshare));
		validator.create(mailshare);

		Mailbox mbox = mailboxAdapter.asMailbox(domainUid, uid, mailshare);
		mailboxes.sanitize(mbox);
		mailboxes.validate(uid, mbox);
		mailshare.quota = mbox.quota;

		storeService.create(mailshareItem, reservedIdsConsumer -> mailboxes.created(uid, mbox, reservedIdsConsumer));

		for (IMailshareHook h : hooks) {
			h.onCreate(context, uid, mailshare, domainUid);
		}
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public void update(String uid, Mailshare mailshare) throws ServerFault {
		ItemValue<Mailshare> itemValue = ItemValue.create(uid, mailshare);
		updateWithItem(itemValue);
	}

	private void updateWithItem(ItemValue<Mailshare> mailshareItem) throws ServerFault {
		String uid = mailshareItem.uid;
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_MAILSHARE);
		Mailshare mailshare = mailshareItem.value;

		ItemValue<Mailshare> previous = storeService.get(uid);
		if (previous == null) {
			throw new ServerFault("mailshare " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		if (!StringUtils.equals(mailshare.orgUnitUid, previous.value.orgUnitUid)) {
			rbacManager.forOrgUnit(mailshare.orgUnitUid).check(BasicRoles.ROLE_MANAGE_MAILSHARE);
		}
		extSanitizer.update(previous.value, mailshare);
		extSanitizer.update(new DirDomainValue<>(domainUid, uid, previous.value),
				new DirDomainValue<>(domainUid, uid, mailshare));
		validator.update(previous.value, mailshare);

		Mailbox previousMailbox = mailboxAdapter.asMailbox(domainUid, uid, previous.value);
		Mailbox currentMailbox = mailboxAdapter.asMailbox(domainUid, uid, mailshare);
		mailboxes.sanitize(currentMailbox);
		mailboxes.validate(uid, currentMailbox);
		mailshare.quota = currentMailbox.quota;

		storeService.update(mailshareItem,
				reservedIdsConsumer -> mailboxes.updated(uid, previousMailbox, currentMailbox, reservedIdsConsumer));

		for (IMailshareHook h : hooks) {
			h.onUpdate(context, uid, mailshare, domainUid);
		}
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public ItemValue<Mailshare> getComplete(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGER);
		return storeService.get(uid);
	}

	@Override
	public List<ItemValue<Mailshare>> allComplete() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILSHARE);

		List<ItemValue<DirEntryAndValue<Mailshare>>> all = storeService.all();
		List<ItemValue<Mailshare>> mailshares = new ArrayList<>(all.size());
		for (ItemValue<DirEntryAndValue<Mailshare>> mailshare : all) {
			mailshares.add(ItemValue.create(mailshare, mailshare.value.value));
		}
		return mailshares;
	}

	@Override
	public TaskRef delete(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_MAILSHARE);

		return context.provider().instance(ITasksManager.class).run(monitor -> {

			monitor.begin(2, "Deleting mailshare " + uid + "@" + domainUid);

			ItemValue<Mailshare> previous = storeService.get(uid);
			if (previous == null) {
				monitor.end(false, "mailshare " + uid + " not found", "[]");
				return;
			}

			monitor.progress(1, "Deleting mailshare mailbox ...");
			mailboxes.deleted(uid, mailboxAdapter.asMailbox(domainUid, uid, previous.value));
			monitor.progress(2, "Mailshare mailbox deleted");

			storeService.delete(uid);

			for (IMailshareHook h : hooks) {
				h.onDelete(context, uid, domainUid);
			}
			dirEventProducer.deleted(uid, storeService.getVersion());

			monitor.end(true, "Mailshare deleted", JsonUtils.asString(""));

		});

	}

	@Override
	public void setPhoto(String uid, byte[] photo) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_MAILSHARE);

		ItemValue<Mailshare> ret = storeService.get(uid);

		if (ret == null) {
			throw new ServerFault("user " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		byte[] asPng = ImageUtils.checkAndSanitize(photo);
		byte[] icon = ImageUtils.resize(asPng, 22, 22);
		storeService.setPhoto(uid, asPng, icon);
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public void deletePhoto(String uid) throws ServerFault {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_MAILSHARE);

		if (storeService.hasPhoto(uid)) {
			storeService.deletePhoto(uid);
			dirEventProducer.changed(uid, storeService.getVersion());
		}
	}

	@Override
	public byte[] getPhoto(String uid) throws ServerFault {
		return storeService.getPhoto(uid);
	}

	@Override
	public byte[] getIcon(String uid) throws ServerFault {
		return storeService.getIcon(uid);
	}

	@Override
	public Mailshare get(String uid) {
		ItemValue<Mailshare> item = getComplete(uid);
		return item != null ? item.value : null;
	}

	@Override
	public void restore(ItemValue<Mailshare> item, boolean isCreate) {
		if (isCreate) {
			createWithItem(item);
		} else {
			updateWithItem(item);
		}

	}

}
