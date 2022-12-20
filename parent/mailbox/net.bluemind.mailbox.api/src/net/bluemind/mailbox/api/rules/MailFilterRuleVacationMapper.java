package net.bluemind.mailbox.api.rules;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterRange;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperatorName;

public class MailFilterRuleVacationMapper implements MailFilterRuleTypeMapper<MailFilter.Vacation> {
	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public MailFilterRuleVacationMapper() {

	}

	public Optional<MailFilterRule> map(MailFilter.Vacation vacation) {
		if (vacation == null) {
			return Optional.empty();
		}

		MailFilterRule rule = new MailFilterRule();
		rule.type = MailFilterRule.Type.VACATION;
		rule.trigger = MailFilterRule.Trigger.IN;
		rule.active = vacation.enabled;
		rule.client = "bluemind";
		rule.name = "";
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		rule.conditions = new ArrayList<>();
		if (vacation.start != null || vacation.end != null) {
			String startDate = (vacation.start != null) ? formatter.format(vacation.start) : null;
			String endDate = (vacation.end != null) ? formatter.format(vacation.end) : null;
			rule.conditions.add(MailFilterRuleCondition.between("date", startDate, endDate));
		}
		if (vacation.subject != null && (vacation.text != null || vacation.textHtml != null)) {
			rule.addReply(vacation.subject, vacation.text, vacation.textHtml);
		}
		rule.stop = false;
		return Optional.of(rule);
	}

	public MailFilter.Vacation map(MailFilterRule rule) {
		MailFilter.Vacation vacation = new MailFilter.Vacation();
		vacation.enabled = rule.active;
		rule.reply().ifPresentOrElse(reply -> {
			vacation.subject = reply.subject;
			vacation.text = reply.plainBody;
			vacation.textHtml = reply.htmlBody;
		}, () -> {
			vacation.subject = "";
			vacation.text = "";
			vacation.textHtml = "";
		});
		rule.conditions.stream()
				.filter(condition -> condition.filter != null && condition.filter.fields.contains("date")
						&& MailFilterRuleOperatorName.RANGE.equals(condition.filter.operator))
				.findFirst() //
				.map(condition -> (MailFilterRuleFilterRange) condition.filter) //
				.ifPresent(range -> {
					vacation.start = parse(range.lowerBound);
					vacation.end = parse(range.upperBound);
				});
		return vacation;
	}

	private Date parse(String parameter) {
		if (parameter == null) {
			return null;
		}
		try {
			return formatter.parse(parameter);
		} catch (ParseException e) {
			return null;
		}
	}

}
