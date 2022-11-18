package net.bluemind.systemcheck.collect;

import java.util.Arrays;
import java.util.Map;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;

public class UserStatsCollector implements IDataCollector {

	@Override
	public boolean collectForUpgrade() {
		return false;
	}

	public void collect(IServiceProvider provider, Map<String, String> collected) throws Exception {

		int fullArchivedCount = 0;
		int fullActiveCount = 0;
		int simpleArchivedCount = 0;
		int simpleActiveCount = 0;
		int visioArchivedCount = 0;
		int visioActiveCount = 0;

		IDomains domains = provider.instance(IDomains.class);
		for (ItemValue<Domain> domain : domains.all()) {
			IDirectory directoryService = provider.instance(IDirectory.class, domain.uid);
			DirEntryQuery query = new DirEntryQuery();
			query.kindsFilter = Arrays.asList(Kind.USER);
			for (ItemValue<DirEntry> user : directoryService.search(query).values) {
				switch (user.value.accountType) {
				case SIMPLE:
					if (user.value.archived) {
						simpleArchivedCount++;
					} else {
						simpleActiveCount++;
					}
					break;
				case FULL:
					if (user.value.archived) {
						fullArchivedCount++;
					} else {
						fullActiveCount++;
					}
					break;
				case FULL_AND_VISIO:
					if (user.value.archived) {
						visioArchivedCount++;
					} else {
						visioActiveCount++;
					}
				}
			}
		}
		collected.put("users.nb.active", String.valueOf(fullActiveCount));
		collected.put("users.nb.archived", String.valueOf(fullArchivedCount));
		collected.put("simple_users.nb.active", String.valueOf(simpleActiveCount));
		collected.put("simple_users.nb.archived", String.valueOf(simpleArchivedCount));
		collected.put("visio_users.nb.active", String.valueOf(visioActiveCount));
		collected.put("visio_users.nb.archived", String.valueOf(visioArchivedCount));
	}

}
