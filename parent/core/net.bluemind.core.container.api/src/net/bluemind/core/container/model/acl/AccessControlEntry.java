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

import net.bluemind.core.api.BMApi;

/**
 * access control entry
 * 
 */
@BMApi(version = "3")
public class AccessControlEntry {

	/**
	 * subject ( user/group uri )
	 */
	public String subject;

	/**
	 * 
	 */
	public Verb verb;

	/**
	 * Creates a new access control entry. Use the domain uid as a subject for
	 * public acls.
	 * 
	 * @param subject
	 * @param verb
	 * @return
	 */
	public static AccessControlEntry create(String subject, Verb verb) {
		AccessControlEntry ret = new AccessControlEntry();
		ret.subject = subject;
		ret.verb = verb;
		return ret;

	}

	@Override
	public String toString() {
		return "AccessControlEntry: {subject:" + subject + ", verb:" + verb + "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + ((verb == null) ? 0 : verb.hashCode());
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
		AccessControlEntry other = (AccessControlEntry) obj;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (verb != other.verb)
			return false;
		return true;
	}

}
