/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.lib.elasticsearch;

import java.util.Arrays;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class Queries {

	public static QueryBuilder and(QueryBuilder... conds) {
		return and(Arrays.asList(conds));
	}

	public static QueryBuilder and(Iterable<QueryBuilder> conds) {
		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		for (QueryBuilder cond : conds) {
			bool.must(cond);
		}
		return bool;
	}

	public static QueryBuilder or(QueryBuilder... conds) {
		return or(Arrays.asList(conds));
	}

	public static QueryBuilder or(Iterable<QueryBuilder> conds) {
		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		for (QueryBuilder cond : conds) {
			bool.should(cond);
		}
		bool.minimumShouldMatch(1);
		return bool;
	}

	public static QueryBuilder missing(String field) {
		return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(field));
	}

}
