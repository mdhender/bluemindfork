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
import net.bluemind.mailbox.identity.api.IMailboxIdentity;
import net.bluemind.mailbox.identity.api.Identity;

public class RestoreMailboxIdentity extends CrudRestore<Identity> {

	private final ValueReader<VersionnedItem<Identity>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<Identity>>() {
			});

	private final IServiceProvider target;

	public RestoreMailboxIdentity(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return "mailboxIdentity";
	}

	@Override
	protected ValueReader<VersionnedItem<Identity>> reader() {
		return reader;
	}

	@Override
	protected IMailboxIdentity api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(IMailboxIdentity.class, domain.uid, key.owner);
	}
}
