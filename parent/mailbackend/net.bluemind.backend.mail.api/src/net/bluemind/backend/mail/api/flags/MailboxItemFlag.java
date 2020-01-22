/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.api.flags;

import net.bluemind.core.api.BMApi;

/**
 * {@link MailboxItem} flag
 */
@BMApi(version = "3")
public class MailboxItemFlag {
	
	/**
	 * Flag value (\Seen for example)
	 */
	public String flag;
	
	/**
	 * Only {@link SystemFlag} objects should set this variable to true.
	 */
	public boolean isSystem = false;
	
	public MailboxItemFlag() {
	}
	
	public MailboxItemFlag(String flag) {
		this.flag = flag;
	}
	
	@Override
    public final boolean equals(Object object) {
		if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        
        MailboxItemFlag other = (MailboxItemFlag) object;
        return isSystem == other.isSystem && flag.equals(other.flag); 
    }

    @Override
    public int hashCode() {
    	return flag.hashCode() + Boolean.hashCode(isSystem);
    }
    
    @Override
	public String toString() {
		return flag + " (isSystem: " + isSystem + ")";
	}
}