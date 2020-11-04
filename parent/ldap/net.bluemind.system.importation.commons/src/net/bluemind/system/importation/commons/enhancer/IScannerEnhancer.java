package net.bluemind.system.importation.commons.enhancer;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.system.importation.commons.scanner.ImportLogger;

public interface IScannerEnhancer {
	void beforeImport(IImportLogger importLogger, Parameters parameter, ItemValue<Domain> domain, LdapConProxy ldapCon);

	void afterImport(IImportLogger importLogger, Parameters parameter, ItemValue<Domain> domain, LdapConProxy ldapCon);

	void groupMembershipUpdates(ImportLogger withoutStatus, Parameters parameter, ItemValue<Domain> domain,
			LdapConProxy ldapCon, GroupMembershipData groupMembershipData);
}
