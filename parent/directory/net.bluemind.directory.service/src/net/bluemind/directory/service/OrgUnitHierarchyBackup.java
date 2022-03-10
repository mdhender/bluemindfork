package net.bluemind.directory.service;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.Providers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.persistence.DirItemStore;
import net.bluemind.directory.persistence.OrgUnitStore;

public class OrgUnitHierarchyBackup<T> {
	private static final Logger logger = LoggerFactory.getLogger(OrgUnitHierarchyBackup.class);

	private final Container container;
	private final DirItemStore orgUnitItemStore;
	private final DirEntryAndValueStore<OrgUnit> orgUnitDirEntryStore;

	public OrgUnitHierarchyBackup(BmContext context, DataSource pool, SecurityContext securityContext,
			Container container) {
		this.container = container;
		this.orgUnitItemStore = new DirItemStore(pool, container, securityContext, Kind.ORG_UNIT);
		this.orgUnitDirEntryStore = new DirEntryAndValueStore<>(pool, container, new OrgUnitStore(pool, container));
	}

	public void process(ItemValue<DirEntryAndValue<T>> itemValue) {
		BaseContainerDescriptor descriptor = BaseContainerDescriptor.create(container.uid, container.name,
				itemValue.uid, container.type, container.domainUid, container.defaultContainer);
		IBackupStore<DirEntryAndValue<OrgUnit>> orgUnitBackupStream = Providers.get().forContainer(descriptor);
		orgUnitHierarchyOf(itemValue.value.entry).forEach(orgUnit -> orgUnitBackupStream.store(orgUnit));
	}

	private List<ItemValue<DirEntryAndValue<OrgUnit>>> orgUnitHierarchyOf(DirEntry dirEntry) {
		return orgUnitHierarchyOf(dirEntry, new ArrayList<>());
	}

	private List<ItemValue<DirEntryAndValue<OrgUnit>>> orgUnitHierarchyOf(DirEntry dirEntry,
			List<ItemValue<DirEntryAndValue<OrgUnit>>> previous) {
		if (dirEntry.orgUnitUid == null) {
			return previous;
		}

		try {
			Item item = orgUnitItemStore.get(dirEntry.orgUnitUid);
			ItemValue<DirEntryAndValue<OrgUnit>> itemValue = ItemValue.create(item, orgUnitDirEntryStore.get(item));
			previous.add(0, itemValue);
			return orgUnitHierarchyOf(itemValue.value.entry, previous);
		} catch (Exception e) {
			logger.warn("Fail to retrieve OrgUnit for dirEntry uid:{}", dirEntry.entryUid, e);
			return previous;
		}
	}

}
