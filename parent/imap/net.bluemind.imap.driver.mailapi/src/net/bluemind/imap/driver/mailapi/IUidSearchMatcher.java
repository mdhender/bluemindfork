package net.bluemind.imap.driver.mailapi;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import net.bluemind.imap.endpoint.driver.SelectedFolder;

public interface IUidSearchMatcher {

	public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid, SelectedFolder selected, ImapIdSetResolver resolver);

}
