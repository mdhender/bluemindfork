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
package net.bluemind.eas.backend;

import net.bluemind.eas.data.calendarenum.AttendeeStatus;
import net.bluemind.eas.data.calendarenum.AttendeeType;

public class MSAttendee {
	private String email;
	private String name;
	private AttendeeStatus attendeeStatus;
	private AttendeeType attendeeType;
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public AttendeeStatus getAttendeeStatus() {
		return attendeeStatus;
	}
	public void setAttendeeStatus(AttendeeStatus attendeeStatus) {
		this.attendeeStatus = attendeeStatus;
	}
	public AttendeeType getAttendeeType() {
		return attendeeType;
	}
	public void setAttendeeType(AttendeeType attendeeType) {
		this.attendeeType = attendeeType;
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
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
		MSAttendee other = (MSAttendee) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		return true;
	}
	
	
}
