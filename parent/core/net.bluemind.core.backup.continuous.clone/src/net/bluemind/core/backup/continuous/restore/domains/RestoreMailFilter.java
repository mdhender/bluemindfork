package net.bluemind.core.backup.continuous.restore.domains;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.MailboxMailFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;

public class RestoreMailFilter implements RestoreDomainType {
	private static final ValueReader<ItemValue<MailboxMailFilter>> mailFilterReader = JsonUtils
			.reader(new TypeReference<ItemValue<MailboxMailFilter>>() {
			});

	private final RestoreLogger log;
	private final ItemValue<Domain> domain;
	private final IServiceProvider target;

	public RestoreMailFilter(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		this.log = log;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "mailfilters";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		ItemValue<MailboxMailFilter> itemValue = mailFilterReader.read(payload);
		MailboxMailFilter mailboxFilter = itemValue.value;
		IMailboxes mailboxesApi = target.instance(IMailboxes.class, domain.uid);
		if (!mailboxFilter.isDomain) {
			log.set(type(), "mailbox", key);
			mailboxesApi.setMailboxFilter(mailboxFilter.uid, mailboxFilter.filter);
		} else {
			log.set(type(), "domain", key);
			mailboxesApi.setDomainFilter(mailboxFilter.filter);
		}
	}
}
