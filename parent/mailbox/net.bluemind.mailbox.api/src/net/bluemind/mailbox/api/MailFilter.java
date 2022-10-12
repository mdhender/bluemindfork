/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.mailbox.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.api.BMApi;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionRedirect;

@BMApi(version = "3")
public class MailFilter {

	public List<MailFilterRule> rules = Collections.emptyList();

	@BMApi(version = "3")
	public static class Forwarding {
		public boolean enabled;
		public boolean localCopy;
		public Set<String> emails = new HashSet<>();

		public static Forwarding copy(Forwarding forwarding) {
			Forwarding f = new Forwarding();
			f.enabled = forwarding.enabled;
			f.localCopy = forwarding.localCopy;
			f.emails.addAll(forwarding.emails);

			return f;
		}

		public static Forwarding fromAction(MailFilterRuleActionRedirect redirect) {
			Forwarding forwarding = new Forwarding();
			if (redirect == null) {
				return new Forwarding();
			}
			forwarding.enabled = true;
			forwarding.emails = new HashSet<>(redirect.emails());
			forwarding.localCopy = redirect.keepCopy();
			return forwarding;
		}

		@Override
		public int hashCode() {
			return Objects.hash(emails, enabled, localCopy);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Forwarding other = (Forwarding) obj;
			return Objects.equals(emails, other.emails) && enabled == other.enabled && localCopy == other.localCopy;
		}
	}

	public Forwarding forwarding = new Forwarding();

	@BMApi(version = "3")
	public static class Vacation {
		public boolean enabled;
		public Date start;
		public Date end;
		public String text;
		public String textHtml;
		public String subject;

		public static Vacation copy(Vacation vacation) {
			Vacation v = new Vacation();
			v.enabled = vacation.enabled;
			v.start = vacation.start;
			v.end = vacation.end;
			v.text = vacation.text;
			v.textHtml = vacation.textHtml;
			v.subject = vacation.subject;

			return v;
		}

		@Override
		public int hashCode() {
			return Objects.hash(enabled, end, start, subject, text, textHtml);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Vacation other = (Vacation) obj;
			return enabled == other.enabled && Objects.equals(end, other.end) && Objects.equals(start, other.start)
					&& Objects.equals(subject, other.subject) && Objects.equals(text, other.text)
					&& Objects.equals(textHtml, other.textHtml);
		}
	}

	public Vacation vacation = new Vacation();

	public static MailFilter create(MailFilterRule... rules) {
		MailFilter f = new MailFilter();
		f.rules = Arrays.asList(rules);
		return f;
	}

	public static MailFilter copy(MailFilter mailFilter) {
		MailFilter mf = new MailFilter();
		mf.forwarding = Forwarding.copy(mailFilter.forwarding);
		mf.vacation = Vacation.copy(mailFilter.vacation);
		mf.rules = mailFilter.rules.stream().map(rule -> MailFilterRule.copy(rule)).collect(Collectors.toList());
		return mf;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((forwarding == null) ? 0 : forwarding.hashCode());
		result = prime * result + ((rules == null) ? 0 : rules.hashCode());
		result = prime * result + ((vacation == null) ? 0 : vacation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailFilter other = (MailFilter) obj;
		if (forwarding == null) {
			if (other.forwarding != null)
				return false;
		} else if (!forwarding.equals(other.forwarding))
			return false;
		if (rules == null) {
			if (other.rules != null)
				return false;
		} else if (!rules.equals(other.rules))
			return false;
		if (vacation == null) {
			if (other.vacation != null)
				return false;
		} else if (!vacation.equals(other.vacation))
			return false;
		return true;
	}
}
