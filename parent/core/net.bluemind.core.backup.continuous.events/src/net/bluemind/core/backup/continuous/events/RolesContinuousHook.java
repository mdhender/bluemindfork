package net.bluemind.core.backup.continuous.events;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.dto.DirEntryRole;
import net.bluemind.core.backup.continuous.dto.OrgUnitAdminRole;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.role.hook.AdminRoleEvent;
import net.bluemind.role.hook.IRoleHook;
import net.bluemind.role.hook.RoleEvent;

public class RolesContinuousHook implements IRoleHook {

	private final DirEntryRoleContinuousBackup dirEntryRoleBackup = new DirEntryRoleContinuousBackup();
	private final OrgUnitRoleContinuousBackup orgUnitRoleBackup = new OrgUnitRoleContinuousBackup();

	private class DirEntryRoleContinuousBackup implements ContinuousContenairization<DirEntryRole> {
		@Override
		public String type() {
			return "role";
		}
	}

	private class OrgUnitRoleContinuousBackup implements ContinuousContenairization<OrgUnitAdminRole> {
		@Override
		public String type() {
			return "ou-roles";
		}
	}

	@Override
	public void onRolesSet(RoleEvent event) throws ServerFault {
		DirEntryRole role = new DirEntryRole(event.kind, event.roles);
		dirEntryRoleBackup.save(event.domainUid, event.uid, event.uid, role, true);
	}

	@Override
	public void onAdministratorRolesSet(AdminRoleEvent event) throws ServerFault {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IOrgUnits orgUnitApi = prov.instance(IOrgUnits.class, event.domainUid);
		ItemValue<OrgUnit> orgUnitItem = orgUnitApi.getComplete(event.uid);
		OrgUnitAdminRole role = new OrgUnitAdminRole(event.kind, event.roles, event.dirUid, orgUnitItem.value);
		orgUnitRoleBackup.save(event.domainUid, event.dirUid, orgUnitItem.item(), role);
	}

}
