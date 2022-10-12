package net.bluemind.delivery.rules;

import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionCustom;

public interface IMailFilterRuleCustomAction {

	String kind();

	DeliveryContent applyTo(DeliveryContent nextContent, MailFilterRuleActionCustom custom);

}
