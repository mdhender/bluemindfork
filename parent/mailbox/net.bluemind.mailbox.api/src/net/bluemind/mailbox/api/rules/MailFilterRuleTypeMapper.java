package net.bluemind.mailbox.api.rules;

import java.util.Optional;

public interface MailFilterRuleTypeMapper<T> {

	Optional<MailFilterRule> map(T type);

	T map(MailFilterRule rule);

}
