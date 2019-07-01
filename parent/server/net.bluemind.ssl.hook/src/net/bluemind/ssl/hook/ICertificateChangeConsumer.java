package net.bluemind.ssl.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;

public interface ICertificateChangeConsumer {

	/**
	 * Implementations are called when BlueMind's master certificate changes.
	 * 
	 * @param ctx
	 * @throws ServerFault
	 */
	void onCertificateChange(BmContext ctx) throws ServerFault;

}
