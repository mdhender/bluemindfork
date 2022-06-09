package net.bluemind.webappdata.service.internal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.webappdata.api.IWebAppData;
import net.bluemind.webappdata.api.WebAppData;

public class NoOpWebAppDataService implements IWebAppData {

	private static final Logger logger = LoggerFactory.getLogger(NoOpWebAppDataService.class);

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		logger.info("NOOP operation IWebAppData#itemChangelog");
		return null;
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		logger.info("NOOP operation IWebAppData#containerChangelog");
		return null;
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		logger.info("NOOP operation IWebAppData#changeset");
		return null;
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		logger.info("NOOP operation IWebAppData#changesetById");
		return null;
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		logger.info("NOOP operation IWebAppData#filteredChangesetById");
		return null;
	}

	@Override
	public long getVersion() throws ServerFault {
		logger.info("NOOP operation IWebAppData#getVersion");
		return 0;
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {
		logger.info("NOOP operation IWebAppData#xfer");

	}

	@Override
	public List<String> allUids() {
		logger.info("NOOP operation IWebAppData#allUids");
		return null;
	}

	@Override
	public Ack update(String uid, WebAppData value) {
		logger.info("NOOP operation IWebAppData#update");
		return null;
	}

	@Override
	public Ack create(String uid, WebAppData value) {
		logger.info("NOOP operation IWebAppData#create");
		return null;
	}

	@Override
	public void delete(String uid) {
		logger.info("NOOP operation IWebAppData#delete");
	}

	@Override
	public ItemValue<WebAppData> getComplete(String uid) {
		logger.info("NOOP operation IWebAppData#getComplete");
		return null;
	}

	@Override
	public List<ItemValue<WebAppData>> multipleGet(List<String> uids) {
		logger.info("NOOP operation IWebAppData#multipleGet");
		return null;
	}

	@Override
	public WebAppData get(String uid) {
		logger.info("NOOP operation IWebAppData#get");
		return null;
	}

	@Override
	public void restore(ItemValue<WebAppData> item, boolean isCreate) {
		logger.info("NOOP operation IWebAppData#restore");
	}

	@Override
	public ItemValue<WebAppData> getCompleteById(long id) {
		logger.info("NOOP operation IWebAppData#getCompleteById");
		return null;
	}

	@Override
	public List<ItemValue<WebAppData>> multipleGetById(List<Long> ids) {
		logger.info("NOOP operation IWebAppData#multipleGetById");
		return null;
	}

	@Override
	public WebAppData getByKey(String key) {
		logger.info("NOOP operation IWebAppData#getByKey");
		return null;
	}

	@Override
	public void deleteAll() {
		logger.info("NOOP operation IWebAppData#deleteAll");
	}

}
