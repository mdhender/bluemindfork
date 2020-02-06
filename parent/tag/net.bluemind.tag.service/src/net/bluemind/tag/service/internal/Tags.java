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
package net.bluemind.tag.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagChanges;
import net.bluemind.tag.persistence.TagStore;

public class Tags implements ITags {

	private static final Logger logger = LoggerFactory.getLogger(Tags.class);

	private ContainerStoreService<Tag> storeService;
	private TagValidator validator;
	private EventBus eventBus;
	protected final Container container;
	protected final RBACManager rbacManager;

	protected final BmContext context;

	public Tags(BmContext context, DataSource ds, Container container) {
		if (!isDomainContainer(container) && ds.equals(context.getDataSource())) {
			throw new ServerFault("wrong datasource");
		}
		this.context = context;
		this.container = container;
		eventBus = VertxPlatform.eventBus();
		this.storeService = new ContainerStoreService<>(ds, context.getSecurityContext(), container, ITagUids.TYPE,
				new TagStore(ds, container));
		this.rbacManager = RBACManager.forContext(context).forContainer(container);
		this.validator = new TagValidator();
	}

	private static boolean isDomainContainer(Container container) {
		return container.owner.equals(container.domainUid); // default tags of domain
	}

	@Override
	public void create(String uid, Tag tag) throws ServerFault {
		checkWrite();
		doCreate(uid, tag);
	}

	private void doCreate(String uid, Tag tag) throws ServerFault {
		validator.validate(tag);
		storeService.create(uid, getDisplayName(tag), tag);
		fireEventChanged(uid);
	}

	@Override
	public void update(String uid, Tag tag) throws ServerFault {
		checkWrite();
		doUpdate(uid, tag);
	}

	private void doUpdate(String uid, Tag tag) throws ServerFault {
		validator.validate(tag);
		storeService.update(uid, getDisplayName(tag), tag);
		fireEventChanged(uid);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		checkWrite();
		doDelete(uid);
	}

	private void doDelete(String uid) {
		storeService.delete(uid);
		fireEventChanged(uid);
	}

	@Override
	public ItemValue<Tag> getComplete(String uid) throws ServerFault {
		checkRead();
		return storeService.get(uid, null);
	}

	@Override
	public List<ItemValue<Tag>> all() throws ServerFault {
		checkRead();
		return storeService.all();
	}

	@Override
	public List<ItemValue<Tag>> multipleGet(List<String> uids) throws ServerFault {
		checkRead();
		return storeService.getMultiple(uids);
	}

	private String getDisplayName(Tag tag) {
		return tag.label;
	}

	private void fireEventChanged(String uid) {
		JsonObject body = new JsonObject();
		body.put("loginAtDomain", context.getSecurityContext().getSubject());
		eventBus.publish("tags." + container.uid, body);

		body = new JsonObject();
		body.put("containerUid", container.uid);
		body.put("itemUid", uid);
		eventBus.publish("tags.changed", body);
	}

	private void fireChanged() {
		JsonObject body = new JsonObject();
		body.put("loginAtDomain", context.getSecurityContext().getSubject());
		eventBus.publish("bm.todolist.hook." + container.uid + ".changed", body);
		eventBus.publish("bm.todolist.hook.all",
				new JsonObject().put("container", container.uid).put("type", container.type));

	}

	@Override
	public ContainerUpdatesResult updates(TagChanges changes) throws ServerFault {
		checkWrite();
		ContainerUpdatesResult ret = new ContainerUpdatesResult();
		ret.added = new ArrayList<String>();
		ret.updated = new ArrayList<String>();
		ret.removed = new ArrayList<String>();
		ret.errors = new ArrayList<>();
		if (changes.add != null && changes.add.size() > 0) {
			for (TagChanges.ItemAdd add : changes.add) {

				if (storeService.get(add.uid, null) == null) {
					try {
						doCreate(add.uid, add.value);
						ret.added.add(add.uid);
					} catch (ServerFault sf) {
						ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), add.uid));
						logger.error(sf.getMessage(), sf);
					}

				} else {
					try {
						doUpdate(add.uid, add.value);
						ret.updated.add(add.uid);
					} catch (ServerFault sf) {
						ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), add.uid));
						logger.error(sf.getMessage(), sf);
					}

				}
			}
		}

		if (changes.modify != null && changes.modify.size() > 0) {
			for (TagChanges.ItemModify update : changes.modify) {

				if (storeService.get(update.uid, null) != null) {
					try {
						doUpdate(update.uid, update.value);
						ret.updated.add(update.uid);
					} catch (ServerFault sf) {
						ret.errors
								.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), update.uid));
						logger.error(sf.getMessage(), sf);
					}

				} else {
					try {
						doCreate(update.uid, update.value);
						ret.added.add(update.uid);
					} catch (ServerFault sf) {
						ret.errors
								.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), update.uid));
						logger.error(sf.getMessage(), sf);
					}

				}
			}
		}

		if (changes.delete != null && changes.delete.size() > 0) {
			for (TagChanges.ItemDelete d : changes.delete) {

				try {
					doDelete(d.uid);
					ret.removed.add(d.uid);
				} catch (ServerFault sf) {
					if (sf.getCode() == ErrorCode.NOT_FOUND) {
						logger.warn("vcard uid {} was sent as deleted but does not exist.", d.uid);
					} else {
						ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), d.uid));
						logger.error(sf.getMessage(), sf);
					}
				}
			}

		}

		if (!(ret.added.isEmpty() && ret.removed.isEmpty() && ret.updated.isEmpty())) {
			fireChanged();
		}

		ret.version = storeService.getVersion();
		return ret;
	}

	@Override
	public ContainerChangelog changelog(Long since) throws ServerFault {
		checkRead();
		if (since == null) {
			since = 0L;
		}
		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		checkRead();
		if (since == null) {
			since = 0L;
		}
		return storeService.changeset(since, Long.MAX_VALUE);

	}

	protected void checkRead() {
		rbacManager.check(Verb.Read.name());
	}

	protected void checkWrite() {
		rbacManager.check(Verb.Write.name());
	}

	@Override
	public List<String> allUids() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.allUids();
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {

		DataSource ds = context.getMailboxDataSource(serverUid);
		ContainerStore cs = new ContainerStore(null, ds, context.getSecurityContext());
		Container c;
		try {
			c = cs.get(container.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		storeService.xfer(ds, c, new TagStore(ds, c));

	}

}
