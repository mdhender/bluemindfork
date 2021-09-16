package net.bluemind.core.backup.continuous.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.dto.DirEntryRole;
import net.bluemind.core.backup.continuous.dto.OrgUnitAdminRole;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.role.hook.AdminRoleEvent;
import net.bluemind.role.hook.IRoleHook;
import net.bluemind.role.hook.RoleEvent;

public class RolesContinuousHook implements IRoleHook {

	private static final Logger logger = LoggerFactory.getLogger(RolesContinuousHook.class);

	@Override
	public void onRolesSet(RoleEvent event) throws ServerFault {
		ContainerDescriptor metaDesc = ContainerDescriptor.create(event.uid + "_at_" + event.domainUid + "_role",
				event.domainUid + " role", event.uid, "roles", event.domainUid, true);
		DirEntryRole role = new DirEntryRole(event.kind, event.roles);
		ItemValue<DirEntryRole> iv = ItemValue.create(event.uid, role);
		iv.internalId = iv.uid.hashCode();
		DefaultBackupStore.store().<DirEntryRole>forContainer(metaDesc).store(iv);
		logger.info("Saved roles for {}", event.uid);
	}

	@Override
	public void onAdministratorRolesSet(AdminRoleEvent event) throws ServerFault {
		ContainerDescriptor metaDesc = ContainerDescriptor.create(event.uid + "_at_" + event.domainUid + "_ou_role",
				event.domainUid + " ou role", event.dirUid, "ou-roles", event.domainUid, true);
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IOrgUnits orgUnitApi = prov.instance(IOrgUnits.class, event.domainUid);
		ItemValue<OrgUnit> orgUnitItem = orgUnitApi.getComplete(event.uid);

		OrgUnitAdminRole role = new OrgUnitAdminRole(event.kind, event.roles, event.dirUid, orgUnitItem.value);
		ItemValue<OrgUnitAdminRole> iv = ItemValue.create(orgUnitItem.item(), role);
		DefaultBackupStore.store().<OrgUnitAdminRole>forContainer(metaDesc).store(iv);
		logger.info("Saved roles for {}", event.uid);

	}

}
