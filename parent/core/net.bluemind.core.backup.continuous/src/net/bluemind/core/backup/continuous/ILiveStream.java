package net.bluemind.core.backup.continuous;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;

public interface ILiveStream {

	String fullName();

	IResumeToken parse(JsonObject js);

	IResumeToken subscribe(Handler<DataElement> handler);

	IResumeToken subscribe(IResumeToken startOffset, Handler<DataElement> handler);

	/**
	 * Ignore owner & container uid, returns everything
	 * 
	 * @param startOffset
	 * @param handler
	 * @return
	 */
	IResumeToken subscribeAll(IResumeToken startOffset, Handler<DataElement> handler);

	String installationId();

	String domainUid();
//
}