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
package net.bluemind.addressbook.ldap.sync;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsDecorator;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.PagedResults;
import org.apache.directory.api.ldap.model.name.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.addressbook.api.VCardChanges.ItemAdd;
import net.bluemind.addressbook.api.VCardChanges.ItemDelete;
import net.bluemind.addressbook.api.VCardChanges.ItemModify;
import net.bluemind.addressbook.ldap.adapter.InetOrgPersonAdapter;
import net.bluemind.addressbook.ldap.adapter.LdapContact;
import net.bluemind.addressbook.ldap.api.LdapParameters;
import net.bluemind.addressbook.ldap.service.internal.utils.LdapHelper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ContainerSyncStatus.Status;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.persistance.ContainerSettingsStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.sync.ISyncableContainer;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.lib.ldap.LdapConProxy;

public class LdapAddressBookContainerSync implements ISyncableContainer {

	private class SyncData {
		public long timestamp;
		public String syncToken;
		public List<LdapContact> contacts;
		public Set<String> ldapUids;
	}

	private static final Logger logger = LoggerFactory.getLogger(LdapAddressBookContainerSync.class);

	private BmContext context;
	private Container container;

	public LdapAddressBookContainerSync(BmContext context, Container container) {
		this.context = context;
		this.container = container;
	}

	@Override
	public ContainerSyncResult sync(String syncToken, IServerTaskMonitor monitor) throws ServerFault {
		monitor.begin(2, null);
		try {
			DataSource ds = DataSourceRouter.get(context, container.uid);
			ContainerSettingsStore containerSettingsStore = new ContainerSettingsStore(ds, container);
			Map<String, String> settings = containerSettingsStore.getSettings();

			LdapParameters.DirectoryType type = null;
			if (settings != null && settings.containsKey("type")) {
				try {
					type = LdapParameters.DirectoryType.valueOf(settings.get("type"));
				} catch (IllegalArgumentException iae) {
				}
			}

			if (type != null) {
				monitor.progress(1, String.format("Fetch %s entries of container %s", type.name(), container));

				LdapParameters lp = LdapParameters.create(type, settings.get("hostname"), settings.get("protocol"),
						"true".equals(settings.get("allCertificate")), settings.get("baseDn"), settings.get("loginDn"),
						settings.get("loginPw"), settings.get("filter"), settings.get("entryUUID"));
				ContainerSyncResult ret = sync(syncToken, lp, monitor.subWork(1));
				return ret;
			}

			logger.error("Fail to fetch container settings for container id {}", container.id);
			return null;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e);
		}
	}

	private ContainerSyncResult updateAddressbook(SyncData data, IServerTaskMonitor monitor) {

		ContainerSyncResult ret = new ContainerSyncResult();
		ret.status = new ContainerSyncStatus();
		ret.status.nextSync = data.timestamp + 3600000;
		ret.status.syncToken = data.syncToken;

		VCardChanges changes = new VCardChanges();
		changes.add = new ArrayList<VCardChanges.ItemAdd>();
		changes.modify = new ArrayList<VCardChanges.ItemModify>();
		changes.delete = new ArrayList<VCardChanges.ItemDelete>();

		if (monitor != null) {
			monitor.begin(data.contacts.size(), "Going to import " + data.contacts.size() + " contacts");
		}
		IAddressBook service = context.provider().instance(IAddressBook.class, container.uid);

		List<String> uids = service.allUids();
		int i = 0;
		for (LdapContact lc : data.contacts) {
			if (monitor != null) {
				monitor.progress(i++, "Import " + i + "/" + data.contacts.size());
			}

			if (!uids.contains(lc.uid)) {
				changes.add.add(ItemAdd.create(lc.uid, lc.vcard));
			} else {
				changes.modify.add(ItemModify.create(lc.uid, lc.vcard));
			}
		}

		for (String toRemove : Sets.difference(new HashSet<String>(uids), data.ldapUids)) {
			changes.delete.add(ItemDelete.create(toRemove));
		}

		ContainerUpdatesResult res = service.updates(changes);

		// add photo
		for (LdapContact lc : data.contacts) {
			if (lc.photo != null) {
				service.setPhoto(lc.uid, lc.photo);
			}
		}

		ret.added = res.added.size();
		ret.updated = res.updated.size();
		ret.removed = res.removed.size();

		return ret;
	}

	private ContainerSyncResult sync(String syncToken, LdapParameters lp, IServerTaskMonitor monitor) {

		ContainerSyncResult ret = new ContainerSyncResult();
		ret.status = new ContainerSyncStatus();

		SyncData sd = new SyncData();
		sd.timestamp = System.currentTimeMillis();
		sd.contacts = new ArrayList<LdapContact>();
		sd.ldapUids = new HashSet<String>();
		sd.syncToken = syncToken;
		if (Strings.isNullOrEmpty(sd.syncToken)) {
			sd.syncToken = "19700101000000.0Z";
		}

		try (LdapConProxy con = LdapHelper.connectLdap(lp)) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss'.0Z'");
			long modifyTimestamp = sdf.parse(sd.syncToken).getTime();
			PagedResults pagedSearchControl = new PagedResultsDecorator(con.getCodecService());

			// Fetch all ldap uids
			SearchRequest searchRequest = new SearchRequestImpl();
			searchRequest.setBase(new Dn(lp.baseDn));
			searchRequest.setFilter(lp.filter);
			searchRequest.setScope(SearchScope.SUBTREE);
			searchRequest.addAttributes(lp.entryUUID);
			searchRequest.setSizeLimit(0);
			searchRequest.setDerefAliases(AliasDerefMode.NEVER_DEREF_ALIASES);

			SearchCursor cursor = con.search(searchRequest);
			while (cursor.next()) {
				Response response = cursor.get();
				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				Entry entry = cursor.getEntry();
				String uid = InetOrgPersonAdapter.getUid(lp.type, entry, lp.entryUUID);
				if (!Strings.isNullOrEmpty(uid)) {
					sd.ldapUids.add(uid);
				}
			}

			monitor.log("Found " + sd.ldapUids.size() + " ldap entries for entryUid " + lp.entryUUID);
			logger.info("Found {} ldap entries for entryUid {}", sd.ldapUids.size(), lp.entryUUID);

			int pages = 0;
			do {
				pages++;
				pagedSearchControl.setSize(100);

				searchRequest = new SearchRequestImpl();
				searchRequest.setBase(new Dn(lp.baseDn));
				searchRequest.setFilter("(&" + lp.filter + "(" + lp.modifyTimeStampAttr + ">=" + sd.syncToken + "))");
				searchRequest.setScope(SearchScope.SUBTREE);
				searchRequest.addAttributes("*", lp.modifyTimeStampAttr, lp.entryUUID);
				searchRequest.setDerefAliases(AliasDerefMode.NEVER_DEREF_ALIASES);
				searchRequest.addControl(pagedSearchControl);

				cursor = con.search(searchRequest);
				Status status = Status.SUCCESS;
				while (cursor.next()) {
					Response response = cursor.get();
					if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
						continue;
					}

					Entry entry = cursor.getEntry();
					LdapContact contact = InetOrgPersonAdapter.getVCard(entry, lp.type, lp.entryUUID);
					if (contact.err != null) {
						monitor.log(String.format("Unsupported %s for entry %s", contact.err.name(), contact.uid));
						status = Status.ERROR;
					}
					sd.contacts.add(contact);
					if (entry.containsAttribute(lp.modifyTimeStampAttr)) {
						String entryModifyTimestamp = entry.get(lp.modifyTimeStampAttr).getString();
						if (entryModifyTimestamp.length() >= 14) {
							// Ensure ending with '.0Z' - LDAP no, AD yes
							entryModifyTimestamp = entryModifyTimestamp.substring(0, 14) + ".0Z";
							modifyTimestamp = Math.max(modifyTimestamp,
									sdf.parse(entryModifyTimestamp).getTime() + 1000);
						}
					}
				}

				SearchResultDone result = cursor.getSearchResultDone();
				LdapResult ldapResult = result.getLdapResult();
				if (ldapResult.getResultCode() != ResultCodeEnum.SUCCESS) {
					logger.info("{} {}", ldapResult.getResultCode(), ldapResult.getDiagnosticMessage());
					break;
				}

				pagedSearchControl = (PagedResults) result.getControl(PagedResults.OID);

				ContainerSyncResult syncRes = updateAddressbook(sd, monitor);
				ret.status.syncStatus = status;
				ret.status.nextSync = sd.timestamp + 3600000;
				ret.status.syncToken = sdf.format(modifyTimestamp);
				ret.added += syncRes.added;
				ret.removed += syncRes.removed;
				ret.updated += syncRes.updated;

				logger.info("pages {}: total added {}, total removed: {}, total updated: {}", pages, ret.added,
						ret.removed, ret.updated);

				monitor.log("pages: " + pages + ", total added " + ret.added + ", total removed: " + ret.removed
						+ ", total updated: " + ret.updated);

				sd.contacts.clear();
			} while (pagedSearchControl != null && pagedSearchControl.getCookie() != null);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return ret;
	}

}
