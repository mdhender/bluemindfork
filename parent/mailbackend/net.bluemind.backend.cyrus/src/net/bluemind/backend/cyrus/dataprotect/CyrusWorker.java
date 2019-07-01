package net.bluemind.backend.cyrus.dataprotect;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.worker.DefaultWorker;
import net.bluemind.server.api.Server;

public class CyrusWorker extends DefaultWorker {

	public CyrusWorker() {
	}

	@Override
	public boolean supportsTag(String tag) {
		return "mail/imap".equals(tag);
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		super.prepareDataDirs(ctx, tag, toBackup);
	}

	@Override
	public Set<String> getDataDirs() {
		return Sets.newHashSet("/var/lib/cyrus", "/var/spool/cyrus");
	}

	@Override
	public void dataDirsSaved(IDPContext ctx, String tag, ItemValue<Server> backedUp) throws ServerFault {
		super.dataDirsSaved(ctx, tag, backedUp);
	}

	@Override
	public void restore(IDPContext ctx, PartGeneration part, Map<String, Object> params) throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public String getDataType() {
		return "cyrus";
	}

}
