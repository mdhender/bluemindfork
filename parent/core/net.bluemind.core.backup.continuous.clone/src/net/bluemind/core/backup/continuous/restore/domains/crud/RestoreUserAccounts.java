package net.bluemind.core.backup.continuous.restore.domains.crud;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.IUserExternalAccount;
import net.bluemind.user.api.UserAccount;

public class RestoreUserAccounts extends CrudRestore<UserAccount> {
	private static final ValueReader<ItemValue<UserAccount>> reader = JsonUtils
			.reader(new TypeReference<ItemValue<UserAccount>>() {
			});

	private final IServiceProvider target;

	public RestoreUserAccounts(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return "userAccounts";
	}

	@Override
	protected ValueReader<ItemValue<UserAccount>> reader() {
		return reader;
	}

	@Override
	protected IUserExternalAccount api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(IUserExternalAccount.class, domain.uid, key.owner);
	}
}
