package net.bluemind.backend.mail.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/outbox/{domainUid}/{mailboxUid}")
public interface IOutbox {

	@POST
	@Path("flush")
	TaskRef flush();
}
