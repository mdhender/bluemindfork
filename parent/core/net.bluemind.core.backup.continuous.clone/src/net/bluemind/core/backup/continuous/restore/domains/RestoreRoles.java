package net.bluemind.core.backup.continuous.restore.domains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.DirEntryRole;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
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

	private final RestoreLogger log;
	private final ItemValue<Domain> domain;
	private final IServiceProvider target;

	public RestoreRoles(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		this.log = log;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "role";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		ItemValue<DirEntryRole> itemValue = rolesReader.read(payload);
		DirEntryRole roleEvent = itemValue.value;
		switch (roleEvent.kind) {
		case DOMAIN:
			log.set(type(), roleEvent.kind.name(), key);
			IDomains domainApi = target.instance(IDomains.class);
			domainApi.setRoles(itemValue.uid, roleEvent.roles);
			break;
		case GROUP:
			log.set(type(), roleEvent.kind.name(), key);
			IGroup groupApi = target.instance(IGroup.class, domain.uid);
			groupApi.setRoles(itemValue.uid, roleEvent.roles);
			break;
		case USER:
			log.set(type(), roleEvent.kind.name(), key);
			IUser userApi = target.instance(IUser.class, domain.uid);
			userApi.setRoles(itemValue.uid, roleEvent.roles);
			break;
		default:
			log.skip(type(), roleEvent.kind.name(), key, payload);
		}
	}
}
