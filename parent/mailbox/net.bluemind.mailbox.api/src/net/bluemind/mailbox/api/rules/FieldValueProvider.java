package net.bluemind.mailbox.api.rules;

import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleField;

public interface FieldValueProvider {

	<T> T provides(MailFilterRuleField<T> field);

}
