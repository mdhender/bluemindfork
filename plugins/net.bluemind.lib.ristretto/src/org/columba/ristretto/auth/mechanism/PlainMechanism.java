/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Ristretto Mail API.
 *
 * The Initial Developers of the Original Code are
 * Timo Stich and Frederik Dietz.
 * Portions created by the Initial Developers are Copyright (C) 2004
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.columba.ristretto.auth.mechanism;

import java.io.IOException;

import org.columba.ristretto.auth.AuthenticationException;
import org.columba.ristretto.auth.AuthenticationMechanism;
import org.columba.ristretto.auth.AuthenticationServer;

/**
 * Implementation of the PLAIN SASL AuthenticationMechanism.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class PlainMechanism implements AuthenticationMechanism {

	/**
	 * @see org.columba.ristretto.auth.AuthenticationMechanism#authenticate(org.columba.ristretto.auth.AuthenticationServer,
	 *      java.lang.String, char[])
	 */
	public void authenticate(AuthenticationServer server, String user, char[] password)
			throws IOException, AuthenticationException {
		server.authReceive();

		byte[] userBytes = user.getBytes("UTF-8");
		byte[] passwordBytes = new String(password).getBytes("UTF-8");
		byte[] command = new byte[userBytes.length + passwordBytes.length + 2];

		command[0] = 0;
		System.arraycopy(userBytes, 0, command, 1, userBytes.length);
		command[userBytes.length + 1] = 0;
		System.arraycopy(passwordBytes, 0, command, userBytes.length + 2, passwordBytes.length);

		server.authSend(command);
	}
}
