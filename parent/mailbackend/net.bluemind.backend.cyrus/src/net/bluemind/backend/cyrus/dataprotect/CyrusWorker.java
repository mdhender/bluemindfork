package net.bluemind.backend.cyrus.dataprotect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Sets;

import net.bluemind.backend.cyrus.partitions.CyrusFileSystemPathHelper;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.worker.DefaultWorker;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class CyrusWorker extends DefaultWorker {
	private static final String CYRUS_TAG = "mail/imap";

	public CyrusWorker() {
	}

	@Override
	public boolean supportsTag(String tag) {
		return CYRUS_TAG.equals(tag);
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		super.prepareDataDirs(ctx, tag, toBackup);
	}

	@Override
	public Set<String> getDataDirs() {
		ISystemConfiguration sysApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		List<String> skipTags = new ArrayList<>(sysApi.getValues().stringList(SysConfKeys.dpBackupSkipTags.name()));

		List<ItemValue<Domain>> domains = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).all();

		Set<String> domainsPath = domains.stream()
				.map(domain -> getDomainPath(!skipTags.contains("mail/cyrus_archives"), domain))
				.flatMap(paths -> paths.stream()).collect(Collectors.toSet());
		domainsPath.add("/var/lib/cyrus");
		return domainsPath;
	}

	private Set<String> getDomainPath(boolean withArchive, ItemValue<Domain> domain) {
		List<String> servers = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, "default").byAssignment(domain.uid, CYRUS_TAG);

		return servers.stream().map(serverUid -> getServerDomainPath(withArchive, serverUid, domain.uid))
				.flatMap(path -> path.stream()).collect(Collectors.toSet());
	}

	private Set<String> getServerDomainPath(boolean withArchive, String serverUid, String domainUid) {
		CyrusPartition cyrusPartition = CyrusPartition.forServerAndDomain(serverUid, domainUid);

		Set<String> paths = Sets.newHashSet(
				CyrusFileSystemPathHelper.getDomainDataFileSystemPath(cyrusPartition, domainUid),
				CyrusFileSystemPathHelper.getDomainMetaFileSystemPath(cyrusPartition, domainUid));

		if (withArchive) {
			paths.add(CyrusFileSystemPathHelper.getDomainHSMFileSystemPath(cyrusPartition, domainUid));
		}

		return paths.stream().map(this::expandToMailboxLetterPath).flatMap(p -> p.stream()).collect(Collectors.toSet());
	}

	private Set<String> expandToMailboxLetterPath(String domainPath) {
		return IntStream.rangeClosed('a', 'z').mapToObj(c -> (char) c).map(c -> String.format("%s/%s", domainPath, c))
				.collect(Collectors.toSet());
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
