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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;

public class Queries {
	private static Logger logger = LoggerFactory.getLogger(Queries.class);

	private static final Pattern alreadyEscapedRegex = Pattern.compile(".*?\\\\[\\[\\]+!-&|!(){}^\"~*?].*");
	private static final Pattern escapeRegex = Pattern.compile("([+\\-!\\(\\){}\\[\\]^\"~*?\\\\]|[&\\|]{2})");

	private Queries() {

	}

	public static Query and(Query... conds) {
		return and(Arrays.asList(conds));
	}

	public static Query and(Iterable<Query> conds) {
		return QueryBuilders.bool(b -> {
			conds.forEach(b::must);
			return b;
		});
	}

	public static Query or(Query... conds) {
		return or(Arrays.asList(conds));
	}

	public static Query or(Iterable<Query> conds) {
		return QueryBuilders.bool(b -> {
			conds.forEach(b::should);
			return b.minimumShouldMatch("1");
		});
	}

	/**
	 * escape the elastic-search query string. we escape all but the ":" character,
	 * since it also serves as the field:value separator
	 * 
	 * @param query
	 * @return
	 */
	public static String escape(String query) {
		if (alreadyEscapedRegex.matcher(query).matches()) {
			logger.warn("Escaping already escaped query {}", query);
		}
		query = query.replace("\\:", "##");
		query = escapeRegex.matcher(query).replaceAll("\\\\$1");
		return query.replace("##", "\\:");
	}

	public static class BmDateRange {
		public interface Builder {
			RangeQuery.Builder apply(RangeQuery.Builder builder, BmDateTime value, boolean withTz);
		}

		public static Query gte(String field, BmDateTime value) {
			return dateRangeQuery(field, value, (rangeBuilder, date, withTz) -> rangeBuilder.gte(value(date, withTz)));
		}

		public static Query gt(String field, BmDateTime value) {
			return dateRangeQuery(field, value, (rangeBuilder, date, withTz) -> rangeBuilder.gt(value(date, withTz)));
		}

		public static Query lt(String field, BmDateTime value) {
			return dateRangeQuery(field, value, (rangeBuilder, date, withTz) -> rangeBuilder.lt(value(date, withTz)));
		}

		private static JsonData value(BmDateTime date, boolean withTz) {
			return (withTz) //
					? JsonData.of(date.iso8601) //
					: JsonData.of(new BmDateTimeWrapper(date).format("yyyy-MM-dd'T'HH:mm:ss.S"));
		}

		private static Query dateRangeQuery(String field, BmDateTime date, BmDateRange.Builder builder) {
			String dateField = field + ".iso8601";
			String tzField = field + ".timezone";
			Query rangeWithoutTz = RangeQuery.of(r -> builder.apply(r.field(dateField), date, false))._toQuery();
			Query rangeWithTz = RangeQuery.of(r -> builder.apply(r.field(dateField), date, true))._toQuery();

			return BoolQuery.of(b1 -> b1 //
					.must(m -> m.exists(e -> e.field(dateField))) //
					.must(m -> m.bool(b2 -> b2 //
							.should(s -> s.bool(b3 -> b3 //
									.mustNot(mn -> mn.exists(e -> e.field(tzField))) //
									.must(rangeWithoutTz)))
							.should(s -> s.bool(b3 -> b3 //
									.must(m2 -> m2.exists(e -> e.field(tzField))) //
									.must(rangeWithTz))))))
					._toQuery();
		}

	}
}
