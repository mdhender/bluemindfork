package net.bluemind.delivery.rules;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.delivery.lmtp.common.IDeliveryContext;
import net.bluemind.delivery.lmtp.common.IDeliveryHook;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.rules.MailFilterRule;

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
		RuleEngine engine = new RuleEngine(ctx, new Sendmail(), content, vacationCacheFactory);
		IMailboxes mailboxesApi = provider.instance(IMailboxes.class, box.dom.uid);

		MailFilter domainFilters = mailboxesApi.getDomainFilter();
		content = engine.apply(domainFilters.rules);
		if (content.isEmpty()) {
			logger.info("[rules] message has been discarded by domain rules {}", content);
			return content;
		}

		List<MailFilterRule> rules = MailFilterRule.sort(mailboxesApi.getMailboxRules(box.mbox.uid));
		content = engine.apply(rules);

		return content;
	}
}
