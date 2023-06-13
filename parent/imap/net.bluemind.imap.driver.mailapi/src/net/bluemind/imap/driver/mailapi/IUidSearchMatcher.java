package net.bluemind.imap.driver.mailapi;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

public interface IUidSearchMatcher {

	public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid);

}
