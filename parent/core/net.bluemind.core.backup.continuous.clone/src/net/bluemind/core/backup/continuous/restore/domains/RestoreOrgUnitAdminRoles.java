package net.bluemind.core.backup.continuous.restore.domains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.dto.OrgUnitAdminRole;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.domain.api.Domain;

public class RestoreOrgUnitAdminRoles implements RestoreDomainType {
	private static final Logger logger = LoggerFactory.getLogger(RestoreOrgUnitAdminRoles.class);
	private final ValueReader<ItemValue<OrgUnitAdminRole>> adminRolesReader = JsonUtils
			.reader(new TypeReference<ItemValue<OrgUnitAdminRole>>() {
			});

	private final IServerTaskMonitor monitor;
	private final ItemValue<Domain> domain;
	private final IServiceProvider target;

	public RestoreOrgUnitAdminRoles(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target) {
		this.monitor = monitor;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "ou-roles";
	}

	@Override
	public void restore(DataElement de) {
		try {
			monitor.log("Processing org unit administrator role:\n" + de.key + "\n" + new String(de.payload));
			ItemValue<OrgUnitAdminRole> itemValue = adminRolesReader.read(new String(de.payload));
			OrgUnitAdminRole adminRoleEvent = itemValue.value;
			Item orgUnitItem = itemValue.item();

			IOrgUnits orgUnitApi = target.instance(IOrgUnits.class, domain.uid);
			ItemValue<OrgUnit> existingOrgUnitItem = orgUnitApi.getComplete(orgUnitItem.uid);
			if (existingOrgUnitItem == null) {
				ItemValue<OrgUnit> newOrgUnitItem = ItemValue.create(orgUnitItem, adminRoleEvent.orgUnit);
				orgUnitApi.createWithItem(newOrgUnitItem);
			}

			orgUnitApi.setAdministratorRoles(orgUnitItem.uid, adminRoleEvent.dirUid, adminRoleEvent.roles);
		} catch (Throwable t) {
			monitor.log("Failed to restore org unit administrator role: " + t.getMessage());
		}
	}
}
