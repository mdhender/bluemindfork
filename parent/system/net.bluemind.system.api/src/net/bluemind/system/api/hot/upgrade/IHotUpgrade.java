package net.bluemind.system.api.hot.upgrade;

import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@Path("/hot_upgrade")
public interface IHotUpgrade {

	@GET
	@Path("running")
	Set<HotUpgradeTask> running();

	@POST
	@Path("list")
	List<HotUpgradeTask> list(HotUpgradeTaskFilter filter);

	@GET
	@Path("progress")
	List<HotUpgradeProgress> progress();
}
