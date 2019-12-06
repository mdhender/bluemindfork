package net.bluemind.filehosting.service.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.locator.client.LocatorClient;
import net.bluemind.role.provider.IRolesVerifier;

public class FileHostingRolesVerifier implements IRolesVerifier {

	public static Boolean serverPresent;

	@Override
	public Set<String> getDeactivatedRoles() throws ServerFault {
		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		FileHostingInfo info = context.provider().instance(IFileHosting.class, "global.virt").info();
		if (!info.present) {
			return removeRoles();
		}

		verifyServerPresence();

		if (info.info.equals("BlueMind FileHosting") && noServerAssigned(context)) {
			return removeRoles();
		}

		return Collections.emptySet();
	}

	private boolean noServerAssigned(BmContext context) {
		return !FileHostingRolesVerifier.serverPresent;
	}

	private void verifyServerPresence() throws ServerFault {
		if (null == FileHostingRolesVerifier.serverPresent) {
			String ip = new LocatorClient().locateHost("filehosting/data", "admin0@global.virt");
			FileHostingRolesVerifier.serverPresent = new Boolean(!Strings.isNullOrEmpty(ip));
		}
	}

	private Set<String> removeRoles() {
		return new HashSet<>(Arrays.asList("canRemoteAttach", "canUseFilehosting"));
	}

	public static void reset() {
		FileHostingRolesVerifier.serverPresent = null;
	}

}
