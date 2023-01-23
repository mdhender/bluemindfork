package net.bluemind.imap.driver.mailapi;

import org.elasticsearch.index.query.BoolQueryBuilder;

public interface IUidSearchMatcher {

	public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid);

}
