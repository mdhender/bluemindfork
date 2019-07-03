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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;

@BMApi(version = "3")
public class MailFilter {
	@BMApi(version = "3")
	public static class Rule {
		// FIXME: add doc...
		public String criteria;
		public boolean star;
		public boolean read;
		public boolean delete;
		public boolean discard;
		public Forwarding forward = new Forwarding();
		public String deliver;
		public boolean active = true;

		public static Rule copy(Rule f) {
			Rule ret = new Rule();
			ret.criteria = f.criteria;
			ret.star = f.star;
			ret.read = f.read;
			ret.delete = f.delete;
			ret.discard = f.discard;
			ret.forward = f.forward;
			ret.deliver = f.deliver;
			ret.active = f.active;
			return ret;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (active ? 1231 : 1237);
			result = prime * result + ((criteria == null) ? 0 : criteria.hashCode());
			result = prime * result + (delete ? 1231 : 1237);
			result = prime * result + ((deliver == null) ? 0 : deliver.hashCode());
			result = prime * result + (discard ? 1231 : 1237);
			result = prime * result + ((forward == null) ? 0 : forward.hashCode());
			result = prime * result + (read ? 1231 : 1237);
			result = prime * result + (star ? 1231 : 1237);
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
			Rule other = (Rule) obj;
			if (active != other.active)
				return false;
			if (criteria == null) {
				if (other.criteria != null)
					return false;
			} else if (!criteria.equals(other.criteria))
				return false;
			if (delete != other.delete)
				return false;
			if (deliver == null) {
				if (other.deliver != null)
					return false;
			} else if (!deliver.equals(other.deliver))
				return false;
			if (discard != other.discard)
				return false;
			if (forward == null) {
				if (other.forward != null)
					return false;
			} else if (!forward.equals(other.forward))
				return false;
			if (read != other.read)
				return false;
			if (star != other.star)
				return false;
			return true;
		}
	}

	public List<Rule> rules = Collections.emptyList();

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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((emails == null) ? 0 : emails.hashCode());
			result = prime * result + (enabled ? 1231 : 1237);
			result = prime * result + (localCopy ? 1231 : 1237);
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
			Forwarding other = (Forwarding) obj;
			if (emails == null) {
				if (other.emails != null)
					return false;
			} else if (!emails.equals(other.emails))
				return false;
			if (enabled != other.enabled)
				return false;
			if (localCopy != other.localCopy)
				return false;
			return true;
		}
	}

	public Forwarding forwarding = new Forwarding();

	@BMApi(version = "3")
	public static class Vacation {
		public boolean enabled;
		public BmDateTime start;
		public BmDateTime end;
		public String text;
		public String subject;

		public static Vacation copy(Vacation vacation) {
			Vacation v = new Vacation();
			v.enabled = vacation.enabled;
			v.start = vacation.start;
			v.end = vacation.end;
			v.text = vacation.text;
			v.subject = vacation.subject;

			return v;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (enabled ? 1231 : 1237);
			result = prime * result + ((end == null) ? 0 : end.hashCode());
			result = prime * result + ((start == null) ? 0 : start.hashCode());
			result = prime * result + ((subject == null) ? 0 : subject.hashCode());
			result = prime * result + ((text == null) ? 0 : text.hashCode());
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
			Vacation other = (Vacation) obj;
			if (enabled != other.enabled)
				return false;
			if (end == null) {
				if (other.end != null)
					return false;
			} else if (!end.equals(other.end))
				return false;
			if (start == null) {
				if (other.start != null)
					return false;
			} else if (!start.equals(other.start))
				return false;
			if (subject == null) {
				if (other.subject != null)
					return false;
			} else if (!subject.equals(other.subject))
				return false;
			if (text == null) {
				if (other.text != null)
					return false;
			} else if (!text.equals(other.text))
				return false;
			return true;
		}
	}

	public Vacation vacation = new Vacation();

	public static MailFilter create(Rule... rules) {
		MailFilter f = new MailFilter();
		f.rules = Arrays.asList(rules);
		return f;
	}

	public static MailFilter copy(MailFilter mailFilter) {
		MailFilter mf = new MailFilter();
		mf.forwarding = Forwarding.copy(mailFilter.forwarding);
		mf.vacation = Vacation.copy(mailFilter.vacation);
		mf.rules = new ArrayList<>();
		mailFilter.rules.forEach(r -> mf.rules.add(Rule.copy(r)));

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
