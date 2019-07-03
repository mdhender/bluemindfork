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
package net.bluemind.imap;

import java.util.List;

public class NameSpaceInfo {

	private List<String> personal;
	private List<String> otherUsers;
	private List<String> mailShares;

	public List<String> getPersonal() {
		return personal;
	}

	public void setPersonal(List<String> personal) {
		this.personal = personal;
	}

	public List<String> getOtherUsers() {
		return otherUsers;
	}

	public void setOtherUsers(List<String> otherUsers) {
		this.otherUsers = otherUsers;
	}

	public List<String> getMailShares() {
		return mailShares;
	}

	public void setMailShares(List<String> mailShares) {
		this.mailShares = mailShares;
	}

}
