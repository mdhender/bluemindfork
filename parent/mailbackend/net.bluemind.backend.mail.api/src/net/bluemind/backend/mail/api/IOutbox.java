package net.bluemind.backend.mail.api;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/outbox/{domainUid}/{mailboxUid}")
public interface IOutbox {

	@POST
	@Path("flush")
	TaskRef flush();
}
