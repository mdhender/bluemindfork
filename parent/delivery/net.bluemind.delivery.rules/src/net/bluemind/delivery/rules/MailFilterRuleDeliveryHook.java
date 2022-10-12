package net.bluemind.delivery.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.delivery.lmtp.common.IDeliveryContext;
import net.bluemind.delivery.lmtp.common.IDeliveryHook;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.service.IInCoreMailboxes;

public class MailFilterRuleDeliveryHook implements IDeliveryHook {
	private static final Logger logger = LoggerFactory.getLogger(MailFilterRuleDeliveryHook.class);

	private static final MailboxVacationSendersCache.Factory vacationCacheFactory = MailboxVacationSendersCache.Factory
			.build("/var/spool/bm-core/rules/");

	@Override
	public DeliveryContent preDelivery(IDeliveryContext ctx, DeliveryContent content) {
		IServiceProvider provider = ctx.provider();
		if (content.box() == null || content.box().dom == null || content.isEmpty()) {
			return content;
		}

		ResolvedBox box = content.box();
		RuleEngine engine = new RuleEngine(provider, new Sendmail(), content, vacationCacheFactory);
		IInCoreMailboxes mailboxesApi = provider.instance(IInCoreMailboxes.class, box.dom.uid);

		MailFilter domainFilters = mailboxesApi.getDomainFilter();
		content = engine.apply(domainFilters.rules);
		if (content.isEmpty()) {
			logger.info("[rules] message has been discarded by domain rules {}", content);
			return content;
		}

		content = engine.apply(mailboxesApi.getMailboxRules(box.mbox.uid));

		return content;
	}
}
