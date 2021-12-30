package net.bluemind.core.backup.continuous.restore.domains;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.dto.MailboxMailFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;

public class RestoreMailFilter implements RestoreDomainType {
	private static final ValueReader<ItemValue<MailboxMailFilter>> mailFilterReader = JsonUtils
			.reader(new TypeReference<ItemValue<MailboxMailFilter>>() {
			});

	private final IServerTaskMonitor monitor;
	private final ItemValue<Domain> domain;
	private final IServiceProvider target;

	public RestoreMailFilter(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target) {
		this.monitor = monitor;
		this.domain = domain;
		this.target = target;
	}

	@Override
	public String type() {
		return "mailfilters";
	}

	@Override
	public void restore(DataElement de) {
		monitor.log("Processing mailfilter:\n" + de.key + "\n" + new String(de.payload));
		ItemValue<MailboxMailFilter> itemValue = mailFilterReader.read(new String(de.payload));
		MailboxMailFilter mailboxFilter = itemValue.value;
		IMailboxes mailboxesApi = target.instance(IMailboxes.class, domain.uid);
		if (!mailboxFilter.isDomain) {
			mailboxesApi.setMailboxFilter(mailboxFilter.uid, mailboxFilter.filter);
		} else {
			mailboxesApi.setDomainFilter(mailboxFilter.filter);
		}
	}
}
