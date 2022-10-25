package net.bluemind.delivery.rules;

import java.util.List;
import java.util.stream.Stream;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;

public class ParameterValueCoreProvider implements ParameterValueProvider {
	private static final String DYNAMIC_PARAMETER_PREFIX = "BM_DYNAMIC_";
	private static final String DYNAMIC_PARAMETER_ADDRESSES_ME = "ADDRESSES_ME";
	private static final String DYNAMIC_PARAMETER_DIR_ENTRY = "DIR_ENTRY_";

	private final ResolvedBox box;
	private final IServiceProvider serviceProvider;

	public ParameterValueCoreProvider(ResolvedBox box, IServiceProvider serviceProvider) {
		this.box = box;
		this.serviceProvider = serviceProvider;
	}

	@Override
	public List<String> provides(List<String> parameters) {
		return parameters.stream().flatMap(this::provideDynamicParameter).toList();
	}

	private Stream<String> provideDynamicParameter(String parameter) {
		if (parameter == null || !parameter.startsWith(DYNAMIC_PARAMETER_PREFIX)) {
			return Stream.of(parameter);
		}

		String action = parameter.replace(DYNAMIC_PARAMETER_PREFIX, "");
		if (action.equals(DYNAMIC_PARAMETER_ADDRESSES_ME)) {
			return box.mbox.value.emails.stream().map(email -> email.address);
		} else if (action.startsWith(DYNAMIC_PARAMETER_DIR_ENTRY)) {
			String dirEntryPath = action.replace(DYNAMIC_PARAMETER_DIR_ENTRY, "");
			ItemValue<Mailbox> mailboxItem = getMailbox(dirEntryPath);
			return (mailboxItem != null && mailboxItem.value != null && mailboxItem.value.emails != null)
					? mailboxItem.value.emails.stream().map(email -> email.address).distinct()
					: Stream.empty();
		}
		return Stream.of(parameter);
	}

	private ItemValue<Mailbox> getMailbox(String dirEntryPath) {
		String domainUid = dirEntryPath.substring(0, dirEntryPath.indexOf('/'));
		String entryUid = dirEntryPath.substring(dirEntryPath.lastIndexOf('/') + 1);
		return serviceProvider.instance(IMailboxes.class, domainUid).getComplete(entryUid);
	}

}
