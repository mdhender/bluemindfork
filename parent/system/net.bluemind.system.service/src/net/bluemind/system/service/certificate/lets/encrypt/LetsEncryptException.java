/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.system.service.certificate.lets.encrypt;

import net.bluemind.core.api.fault.ServerFault;

public class LetsEncryptException extends ServerFault {

	private static final long serialVersionUID = 1L;

	public LetsEncryptException(String message) {
		super(message);
	}

	public LetsEncryptException(String message, Exception e) {
		super(message, e);
	}
}
