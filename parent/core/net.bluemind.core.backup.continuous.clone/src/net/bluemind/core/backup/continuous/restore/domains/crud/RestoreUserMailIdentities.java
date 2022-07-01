package net.bluemind.core.backup.continuous.restore.domains.crud;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.api.UserMailIdentity;

public class RestoreUserMailIdentities extends CrudRestore<UserMailIdentity> {

	private final ValueReader<VersionnedItem<UserMailIdentity>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<UserMailIdentity>>() {
			});

	private final IServiceProvider target;

	public RestoreUserMailIdentities(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return "userMailIdentities";
	}

	@Override
	protected ValueReader<VersionnedItem<UserMailIdentity>> reader() {
		return reader;
	}

	@Override
	protected IUserMailIdentities api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(IUserMailIdentities.class, domain.uid, key.owner);
	}
}
