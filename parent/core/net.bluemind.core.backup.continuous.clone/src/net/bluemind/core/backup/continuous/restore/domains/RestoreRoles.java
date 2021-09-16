package net.bluemind.core.backup.continuous.restore.domains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.dto.DirEntryRole;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.group.api.IGroup;
import net.bluemind.user.api.IUser;

public class RestoreRoles implements RestoreDomainType {
	private static final Logger logger = LoggerFactory.getLogger(RestoreRoles.class);
	private final ValueReader<ItemValue<DirEntryRole>> rolesReader = JsonUtils
			.reader(new TypeReference<ItemValue<DirEntryRole>>() {
			});

	private final IServerTaskMonitor monitor;
	private final ItemValue<Domain> domain;
	private final IServiceProvider target;

	public RestoreRoles(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target) {
		this.monitor = monitor;
		this.domain = domain;
		this.target = target;
	}

	public String type() {
		return "roles";
	}

	public void restore(DataElement de) {
		try {
			monitor.log("Processing membership:\n" + de.key + "\n" + new String(de.payload));
			ItemValue<DirEntryRole> itemValue = rolesReader.read(new String(de.payload));
			DirEntryRole roleEvent = itemValue.value;
			switch (roleEvent.kind) {
			case DOMAIN:
				IDomains domainApi = target.instance(IDomains.class);
				domainApi.setRoles(itemValue.uid, roleEvent.roles);
				break;
			case GROUP:
				IGroup groupApi = target.instance(IGroup.class, domain.uid);
				groupApi.setRoles(itemValue.uid, roleEvent.roles);
				break;
			case USER:
				IUser userApi = target.instance(IUser.class, domain.uid);
				userApi.setRoles(itemValue.uid, roleEvent.roles);
				break;
			default:
				logger.warn("Receive roles for uid {} of unknown kind {}", itemValue.uid, roleEvent.kind);
			}
		} catch (Throwable t) {
			monitor.log("Failed to restore membership: " + t.getMessage());
		}
	}
}
