package net.bluemind.filehosting.service.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.FileHostingInfo.Type;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.network.topology.Topology;
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

		if (info.type == Type.INTERNAL && noServerAssigned(context)) {
			return removeRoles();
		}

		return Collections.emptySet();
	}

	private boolean noServerAssigned(BmContext context) {
		return !FileHostingRolesVerifier.serverPresent;
	}

	private void verifyServerPresence() throws ServerFault {
		if (null == FileHostingRolesVerifier.serverPresent) {
			FileHostingRolesVerifier.serverPresent = Topology.getIfAvailable()
					.map(t -> t.anyIfPresent("filehosting/data").isPresent()).orElse(null);
		}
	}

	private Set<String> removeRoles() {
		return new HashSet<>(Arrays.asList("canRemoteAttach", "canUseFilehosting"));
	}

	public static void reset() {
		FileHostingRolesVerifier.serverPresent = null;
	}

}
