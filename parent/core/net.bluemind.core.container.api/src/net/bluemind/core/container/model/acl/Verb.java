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
package net.bluemind.core.container.model.acl;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public enum Verb {
	Invitation, Freebusy, SendOnBehalf, SendAs, Read(Invitation, Freebusy), Write(Read), Manage,
	All(Write, Manage, SendAs, SendOnBehalf);

	public final Verb[] verbs;

	Verb(Verb... verbs) {
		if (verbs != null) {
			this.verbs = verbs;
		} else {
			this.verbs = new Verb[0];
		}
	}

	public boolean can(Verb v) {
		if (v.equals(this)) {
			return true;
		}

		boolean ret = false;
		for (Verb verb : verbs) {
			if (verb.can(v)) {
				ret = true;
				break;
			}
		}

		return ret;
	}

	public Set<Verb> parentHierarchy() {
		Set<Verb> expanded = EnumSet.noneOf(Verb.class);
		parentHierarchy(expanded);
		return expanded;
	}

	private void parentHierarchy(Set<Verb> expanded) {
		expanded.add(this);
		for (Verb verb : Verb.values()) {
			for (Verb v : verb.verbs) {
				if (v == this) {
					verb.parentHierarchy(expanded);
				}
			}
		}
	}


	public static Set<Verb> expand(Verb parent) {
		Set<Verb> expanded = new HashSet<>();
		expanded.add(parent);
		for(Verb verb : parent.verbs) {
			expanded.addAll(expand(verb));
		}
		return expanded;
	}

	public static Set<Verb> compact(List<Verb> verbs) {
		Predicate<Verb> hasParentInList = (verb) -> verbs.stream()
				.anyMatch(parent -> !parent.equals(verb) && parent.can(verb));
		Set<Verb> compacted = new HashSet<>();
		for (Verb verb : verbs) {
			if (!hasParentInList.test(verb)) {
				compacted.add(verb);
			}
		}
		return compacted;
	}
}
