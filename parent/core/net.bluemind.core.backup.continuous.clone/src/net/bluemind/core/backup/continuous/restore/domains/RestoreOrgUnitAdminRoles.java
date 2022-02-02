package net.bluemind.core.backup.continuous.restore.domains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.OrgUnitAdminRole;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
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

	private final RestoreLogger log;
	private final ItemValue<Domain> domain;
	private final IServiceProvider target;

	public RestoreOrgUnitAdminRoles(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		this.log = log;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "ou-roles";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		ItemValue<OrgUnitAdminRole> itemValue = adminRolesReader.read(payload);
		OrgUnitAdminRole adminRoleEvent = itemValue.value;
		Item orgUnitItem = itemValue.item();

		IOrgUnits orgUnitApi = target.instance(IOrgUnits.class, domain.uid);
		ItemValue<OrgUnit> existingOrgUnitItem = orgUnitApi.getComplete(orgUnitItem.uid);
		if (existingOrgUnitItem == null) {
			log.createParent(type(), key, orgUnitItem.uid);
			ItemValue<OrgUnit> newOrgUnitItem = ItemValue.create(orgUnitItem, adminRoleEvent.orgUnit);
			orgUnitApi.restore(newOrgUnitItem, true);
		}

		log.set(type(), key);
		orgUnitApi.setAdministratorRoles(orgUnitItem.uid, adminRoleEvent.dirUid, adminRoleEvent.roles);
	}
}
