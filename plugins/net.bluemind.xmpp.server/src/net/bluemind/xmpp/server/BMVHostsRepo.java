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
package net.bluemind.xmpp.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.ConcurrentHashSet;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;
import tigase.db.DBInitException;
import tigase.db.comp.ComponentRepository;
import tigase.db.comp.RepositoryChangeListenerIfc;
import tigase.util.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostRepoDefaults;

public class BMVHostsRepo implements ComponentRepository<VHostItem> {

	private static final Logger logger = LoggerFactory.getLogger(BMVHostsRepo.class);

	// private Map<String, Domain> domIdx;
	private Set<String> doms;

	public BMVHostsRepo() {
		logger.info("*********** BM VHOSTS ********");
		// domIdx = new ConcurrentHashMap<String, Domain>();
		doms = new ConcurrentHashSet<String>();

		VertxPlatform.eventBus().consumer("refreshDomains", (msg) -> refreshDomains());
		VertxPlatform.eventBus().consumer(IStateListener.STATE_BUS_ADDRESS, (Message<String> msg) -> {
			SystemState state = SystemState.valueOf(msg.body());
			if (state == SystemState.CORE_STATE_RUNNING) {
				logger.info("core is ready, load domains list");
				refreshDomains();
			} else {
				logger.info("core is not ready, domains list is now empty");
				doms = new ConcurrentHashSet<String>();
			}
		});

		refreshDomains();
	}

	private void refreshDomains() {
		try {
			IDomains service = CF.provider().instance(IDomains.class);
			List<ItemValue<Domain>> domains = service.all();
			ConcurrentHashSet<String> nDoms = new ConcurrentHashSet<String>();

			for (ItemValue<Domain> domain : domains) {
				logger.debug("add domain {}", domain.value.name);
				nDoms.add(domain.value.name);
				for (String alias : domain.value.aliases) {
					logger.debug("    **** add domain alias {}", alias);
					nDoms.add(alias);
				}
			}
			doms = nDoms;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		logger.info("** initialized with " + doms.size() + " domains.");
	}

	@Override
	public Iterator<VHostItem> iterator() {
		ArrayList<VHostItem> ret = new ArrayList<>(doms.size());
		for (String s : doms) {
			try {
				VHostItem vh = from(s);
				ret.add(vh);
			} catch (TigaseStringprepException e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.info("returning " + ret.size() + " domains.");
		return ret.iterator();
	}

	@Override
	public void addRepoChangeListener(RepositoryChangeListenerIfc<VHostItem> repoChangeListener) {
		// TODO Auto-generated method stub
		logger.info("addRepoChangeListener");

	}

	@Override
	public void removeRepoChangeListener(RepositoryChangeListenerIfc<VHostItem> repoChangeListener) {
		// TODO Auto-generated method stub
		logger.info("removeRepoChangeListener");

	}

	@Override
	public void addItem(VHostItem item) {
		// TODO Auto-generated method stub
		logger.info("addItem {}", item);

	}

	@Override
	public Collection<VHostItem> allItems() {
		Collection<VHostItem> ret = new LinkedList<VHostItem>();
		VHostItem vhi;
		for (String key : doms) {
			try {
				vhi = from(key);
				ret.add(vhi);
			} catch (TigaseStringprepException e) {
				logger.error(e.getMessage(), e);
			}
		}

		return ret;
	}

	@Override
	public boolean contains(String key) {
		boolean ret = doms.contains(key);
		logger.debug("contains key: '" + key + "' => " + ret);
		return ret;
	}

	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		logger.info("getDefaults");
		// TODO Auto-generated method stub

	}

	@Override
	public VHostItem getItem(String key) {
		logger.debug("getItem {}", key);
		try {
			if (contains(key)) {
				VHostItem ret = from(key);
				logger.debug("getItem {}", key);
				return ret;
			} else {
				return null;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private VHostItem from(String domain) throws TigaseStringprepException {
		if (domain == null || domain.isEmpty()) {
			logger.warn("from null domain");
			return null;
		}
		logger.debug("get VHost {}", domain);
		VHostItem vhi = new VHostItem(domain);
		vhi.setS2sSecret(UUID.randomUUID().toString());
		vhi.setEnabled(true);
		return vhi;
	}

	@Override
	public VHostItem getItemInstance() {
		return VHostRepoDefaults.getItemInstance();
	}

	@Override
	public void reload() {
		refreshDomains();
	}

	@Override
	public void removeItem(String key) {
		logger.info("removeItem {}", key);
	}

	@Override
	public void setProperties(Map<String, Object> properties) {
		// TODO Auto-generated method stub
		logger.info("setProperties");
		for (Entry<String, Object> s : properties.entrySet()) {
			logger.info(" * {} => {}", s.getKey(), s.getValue());
		}

	}

	@Override
	public int size() {
		logger.debug("get size()");
		return doms.size();
	}

	@Override
	public void store() {
		logger.info("store");

	}

	@Override
	public String validateItem(VHostItem item) {
		logger.info("validateItem {}", item);
		return null;
	}

	@Override
	public void setAutoloadTimer(long arg0) {
	}

	@Override
	public void initRepository(String arg0, Map<String, String> arg1) throws DBInitException {
		logger.info("init repo {} {}", arg0, arg1);
	}

	@Override
	public void addItemNoStore(VHostItem arg0) {
		logger.info("add item not store {}", arg0);
	}

	@Override
	public void destroy() {
		logger.info("destroy BMVHostsRepo");
	}

}
