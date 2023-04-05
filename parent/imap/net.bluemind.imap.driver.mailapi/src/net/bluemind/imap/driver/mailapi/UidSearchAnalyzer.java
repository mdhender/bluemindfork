package net.bluemind.imap.driver.mailapi;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.InternalMax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.ESearchActivator;

public class UidSearchAnalyzer {

	private static final Logger logger = LoggerFactory.getLogger(UidSearchAnalyzer.class);
	private static final String XBMEXTERNALID = "X-BM-ExternalID";
	private static final String INTERNAL_DATE = "internalDate";
	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
	private static final String DATE_FORMAT = "d-MMM-yyyy";
	private static final String PARENT_TYPE = "body";

	private static final Pattern RE_ALL = Pattern.compile("all ", Pattern.CASE_INSENSITIVE);
	private static final Pattern RE_SEQUENCE = Pattern.compile("(?:uid )?([\\d\\*:,]+)", Pattern.CASE_INSENSITIVE);

	public Map<String, IUidSearchMatcher> map = new HashMap<>();

	@SuppressWarnings("serial")
	public static class UidSearchException extends Exception {
		public UidSearchException(String message) {
			super(message);
		}
	}

	public UidSearchAnalyzer() {
		map.put("ALL", null);
		map.put("ANSWERED", new FlagAnalyzer());
		map.put("DELETED", new FlagAnalyzer());
		map.put("FLAGGED", new FlagAnalyzer());
		map.put("DRAFT", new FlagAnalyzer());
		map.put("SEEN", new FlagAnalyzer());
		map.put("UNANSWERED", new UnFlagAnalyzer());
		map.put("UNDELETED", new UnFlagAnalyzer());
		map.put("UNFLAGGED", new UnFlagAnalyzer());
		map.put("UNDRAFT", new UnFlagAnalyzer());
		map.put("UNSEEN", new UnseenAnalyzer());
		map.put("BEFORE", new SmallerDateAnalyzer());
		map.put("ON", new EqualsDateAnalyzer());
		map.put("SINCE", new GreaterDateAnalyzer());
		map.put("SENTBEFORE", new SmallerDateAnalyzer());
		map.put("SENTON", new EqualsDateAnalyzer());
		map.put("SENTSINCE", new GreaterDateAnalyzer());
		map.put("SMALLER", new SmallerAnalyzer());
		map.put("LARGER", new LargerAnalyzer());
		map.put("HEADER", new HeaderAnalyzer());
		map.put("KEYWORD", new KeywordAnalyzer());
		map.put("UNKEYWORD", new UnKeywordAnalyzer());
		map.put("UID", new SequenceAnalyzer());
		map.put("BCC", new TextAnalyzer());
		map.put("BODY", new TextAnalyzer());
		map.put("CC", new TextAnalyzer());
		map.put("FROM", new TextAnalyzer());
		map.put("SUBJECT", new TextAnalyzer());
		map.put("TEXT", new TextAnalyzer());
		map.put("TO", new TextAnalyzer());
	}

	public static QueryBUilderResult buildQuery(String query, String folderUid, String meUid) throws Exception {
		query = query + " END";
		int maxUid = 0;
		boolean hasSequence = false;
		BoolQueryBuilder qb = QueryBuilders.boolQuery();
		Client client = ESearchActivator.getClient();

		qb.must(QueryBuilders.termQuery("in", folderUid));

		UidSearchAnalyzer uidSearchAnalyzer = new UidSearchAnalyzer();

		// Gets document with highest uid for keywords with sequences management
		AggregationBuilder a = AggregationBuilders.max("uid_max").field("uid");
		SearchResponse rMax = client.prepareSearch("mailspool_alias_" + meUid).setQuery(qb).addAggregation(a).setSize(0)
				.execute().actionGet();
		if (rMax.getAggregations().get("uid_max") != null) {
			InternalMax uidMax = rMax.getAggregations().get("uid_max");
			maxUid = (int) uidMax.getValue();
		}

		String subQuery = query;

		// Keyword ALL is present -> retrieve all uids
		if (UidSearchAnalyzer.hasAll(query)) {
			subQuery = "END";
		}
		// false when NOT keyword is present
		boolean positiveKeyword = true;
		// true when OR keyword is present
		boolean isCertain = true;
		// count for OR condition
		int orCount = 0;
		BoolQueryBuilder qbShould = QueryBuilders.boolQuery();
		// Analyze query for the different authorized keywords
		while (subQuery.length() != 0) {
			int len = 0;
			String keyword = subQuery.split(" ")[0].trim().toUpperCase();
			if (keyword.equals("END")) {
				break;
			}
			if (keyword.equals("UID")) {
				hasSequence = true;
			}
			if (keyword.equals("NOT")) {
				positiveKeyword = false;
				len = keyword.length() + 1;
				subQuery = subQuery.substring(len);
				continue;
			}
			if (keyword.equals("OR")) {
				isCertain = false;
				len = keyword.length() + 1;
				subQuery = subQuery.substring(len);
				continue;
			}
			IUidSearchMatcher analyzer = uidSearchAnalyzer.map.get(keyword);
			if (analyzer != null) {
				String analyzedQuery = null;
				if (isCertain) {
					analyzedQuery = analyzer.analyse(qb, subQuery, positiveKeyword, isCertain, maxUid);
				} else {
					analyzedQuery = analyzer.analyse(qbShould, subQuery, positiveKeyword, isCertain, maxUid);
				}
				if (analyzedQuery == null) {
					throw new UidSearchException("Invalid Search criteria");
				}
				len = analyzedQuery.length();
			} else {
				if (UidSearchAnalyzer.hasSequence(subQuery)) {
					analyzer = uidSearchAnalyzer.map.get("UID");
					String analyzedQuery = null;
					if (isCertain) {
						analyzedQuery = analyzer.analyse(qb, subQuery, positiveKeyword, isCertain, maxUid);
					} else {
						analyzedQuery = analyzer.analyse(qbShould, subQuery, positiveKeyword, isCertain, maxUid);
					}
					if (analyzedQuery == null) {
						throw new UidSearchException("Invalid Search criteria (qb: " + qb + " qbShould: " + qbShould
								+ " subquery: " + subQuery + ")");
					}
					len = analyzedQuery.length();
					hasSequence = true;
				} else {
					throw new UidSearchException("Invalid Search criteria (subQuery: " + subQuery + " query: " + query
							+ " keyword: " + keyword + ")");
				}
			}
			if (!isCertain) {
				orCount++;
			}
			// When 2 keywords after OR are found, they are part of the same condition, and
			// must be as "should" is the same "must".
			if (orCount > 1) {
				orCount = 0;
				isCertain = true;
				qb.must(qbShould);
				qbShould = QueryBuilders.boolQuery();
			}
			positiveKeyword = true;
			subQuery = subQuery.substring(len);
		}
		return new QueryBUilderResult(qb, hasSequence);
	}

	public record QueryBUilderResult(BoolQueryBuilder bq, boolean hasSequence) {
	}

	public static boolean hasSequence(String query) {
		Matcher matcher = RE_SEQUENCE.matcher(query);
		return matcher.find();
	}

	public static boolean hasAll(String query) {
		Matcher matcher = RE_ALL.matcher(query);
		return matcher.find();
	}

	// Not NOT, positive flag term
	class FlagAnalyzer extends UidSearchPatternBasedMatcher {
		public static final Pattern PATTERN = Pattern.compile("(answered|deleted|draft|flagged|seen) ",
				Pattern.CASE_INSENSITIVE);

		protected FlagAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String gr = matcher.group(1);
				if (!positive) {
					qb.mustNot(QueryBuilders.termQuery("is", gr.toLowerCase()));
					return matcher.group(0);
				}
				if (certain) {
					logger.info("termQuery must 'is' with '{}'", gr);
					qb.must(QueryBuilders.termQuery("is", gr.toLowerCase()));
				} else {
					logger.info("termQuery should 'is' with '{}'", gr);
					qb.should(QueryBuilders.termQuery("is", gr.toLowerCase()));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// Not NOT, negative flag term
	class UnFlagAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("un(answered|deleted|draft|flagged) ",
				Pattern.CASE_INSENSITIVE);

		protected UnFlagAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);

			if (matcher.find()) {
				String gr = matcher.group(1);

				if (positive) {
					logger.info("termQuery must not 'is' with '{}'", gr);
					qb.mustNot(QueryBuilders.termQuery("is", gr.toLowerCase()));
					return matcher.group();
				}
				if (certain) {
					logger.info("termQuery must 'is' with '{}'", gr);
					qb.must(QueryBuilders.termQuery("is", gr.toLowerCase()));
				} else {
					logger.info("termQuery should 'is' with '{}'", gr);
					qb.should(QueryBuilders.termQuery("is", gr.toLowerCase()));
				}
				return matcher.group();
			}
			return null;
		}

	}

	// Not NOT, unseen term
	class UnseenAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(unseen) ", Pattern.CASE_INSENSITIVE);

		protected UnseenAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);

			if (matcher.find()) {
				String gr = matcher.group(1);

				if (!positive) {
					logger.info("termQuery must 'is' with '{}'", gr);
					qb.mustNot(QueryBuilders.termQuery("is", gr.toLowerCase()));
					return matcher.group(0);
				}
				if (certain) {
					logger.info("termQuery must 'is' with '{}'", gr);
					qb.must(QueryBuilders.termQuery("is", gr.toLowerCase()));
				} else {
					logger.info("termQuery should 'is' with '{}'", gr);
					qb.should(QueryBuilders.termQuery("is", gr.toLowerCase()));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	class KeywordAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("keyword (\\S+) ", Pattern.CASE_INSENSITIVE);

		protected KeywordAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);

			if (matcher.find()) {
				String flag = matcher.group(1);

				if (!positive) {
					logger.info("termQuery must not 'is' with '{}'", flag);
					qb.mustNot(QueryBuilders.termQuery("is", flag.toLowerCase()));
					return matcher.group(0);
				}

				if (certain) {
					logger.info("termQuery must 'is' with '{}'", flag);
					qb.must(QueryBuilders.termQuery("is", flag.toLowerCase()));
				} else {
					logger.info("termQuery should 'is' with '{}'", flag);
					qb.should(QueryBuilders.termQuery("is", flag.toLowerCase()));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	class UnKeywordAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("unkeyword (\\S+) ", Pattern.CASE_INSENSITIVE);

		protected UnKeywordAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);

			if (matcher.find()) {
				String flag = matcher.group(1);

				if (positive) {
					logger.info("termQuery must not 'is' with '{}'", flag);
					qb.mustNot(QueryBuilders.termQuery("is", flag.toLowerCase()));
					return matcher.group(0);
				}

				if (certain) {
					logger.info("termQuery must 'is' with '{}'", flag);
					qb.must(QueryBuilders.termQuery("is", flag.toLowerCase()));
				} else {
					logger.info("termQuery should 'is' with '{}'", flag);
					qb.should(QueryBuilders.termQuery("is", flag.toLowerCase()));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// larger than
	class LargerAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("larger (\\d+) ", Pattern.CASE_INSENSITIVE);

		protected LargerAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String size = matcher.group(1);

				if (!positive) {
					if (certain) {
						logger.info("termQuery must 'size' lt '{}'", size);
						qb.must(QueryBuilders.rangeQuery("size").lt(size));
					} else {
						logger.info("termQuery should 'size' lt '{}'", size);
						qb.should(QueryBuilders.rangeQuery("size").lt(size));
					}
					return matcher.group(0);
				}
				if (certain) {
					logger.info("rangeQuery must 'size' gt '{}'", size);
					qb.must(QueryBuilders.rangeQuery("size").gt(size));
				} else {
					logger.info("rangeQuery should 'size' gt '{}'", size);
					qb.should(QueryBuilders.rangeQuery("size").gt(size));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// Smaller
	class SmallerAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("smaller (\\d+) ", Pattern.CASE_INSENSITIVE);

		protected SmallerAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String size = matcher.group(1);
				if (!positive) {
					if (certain) {
						logger.info("termQuery must 'size' gt '{}'", size);
						qb.must(QueryBuilders.rangeQuery("size").gt(size));
					} else {
						logger.info("termQuery should 'size' gt '{}'", size);
						qb.should(QueryBuilders.rangeQuery("size").gt(size));
					}
					return matcher.group(0);
				}
				if (certain) {
					logger.info("termQuery must 'size' lt '{}'", size);
					qb.must(QueryBuilders.rangeQuery("size").lt(size));
				} else {
					logger.info("termQuery should 'size' lt '{}'", size);
					qb.should(QueryBuilders.rangeQuery("size").lt(size));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// Greater than -> rangeQuery(field).gt(dt.iso8601)
	class GreaterDateAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(?:sentsince|since) (\\S+) ", Pattern.CASE_INSENSITIVE);

		protected GreaterDateAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String dateString = matcher.group(1);
				try {
					Date date = DATE_FORMATTER.parse(dateString);

					if (!positive) {
						if (certain) {
							logger.info("termQuery must '{}' lt '{}'", INTERNAL_DATE, dateString);
							qb.must(QueryBuilders.rangeQuery(INTERNAL_DATE).lt(date.getTime()));
						} else {
							logger.info("termQuery should '{}' lt '{}'", INTERNAL_DATE, dateString);
							qb.should(QueryBuilders.rangeQuery(INTERNAL_DATE).lt(date.getTime()));
						}
						return matcher.group(0);
					}
					if (certain) {
						logger.info("termQuery must '{}' gt '{}'", INTERNAL_DATE, dateString);
						qb.must(QueryBuilders.rangeQuery(INTERNAL_DATE).gt(date.getTime()));
					} else {
						logger.info("termQuery should '{}' gt '{}'", INTERNAL_DATE, dateString);
						qb.should(QueryBuilders.rangeQuery(INTERNAL_DATE).gt(date.getTime()));
					}
				} catch (ParseException e) {
					logger.error(e.getMessage());
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// Before
	class SmallerDateAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(?:sentbefore|before) (\\S+) ",
				Pattern.CASE_INSENSITIVE);

		protected SmallerDateAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String dateString = matcher.group(1);
				try {
					Date date = DATE_FORMATTER.parse(dateString);

					if (!positive) {
						if (certain) {
							logger.info("termQuery must '{}' gt '{}'", INTERNAL_DATE, dateString);
							qb.must(QueryBuilders.rangeQuery(INTERNAL_DATE).gt(date.getTime()));
						} else {
							logger.info("termQuery should '{}' gt '{}'", INTERNAL_DATE, dateString);
							qb.should(QueryBuilders.rangeQuery(INTERNAL_DATE).gt(date.getTime()));
						}
						return matcher.group(0);
					}
					if (certain) {
						logger.info("termQuery must '{}' lt '{}'", INTERNAL_DATE, dateString);
						qb.must(QueryBuilders.rangeQuery("internalDate").lt(date.getTime()));
					} else {
						logger.info("termQuery should '{}' lt '{}'", INTERNAL_DATE, dateString);
						qb.should(QueryBuilders.rangeQuery(INTERNAL_DATE).lt(date.getTime()));
					}
				} catch (ParseException e) {
					logger.error(e.getMessage());
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	class EqualsDateAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(?:senton|on) (\\S+) ", Pattern.CASE_INSENSITIVE);

		protected EqualsDateAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String dateString = matcher.group(1);

				if (!positive) {
					logger.info("termQuery must not '{}' eq '{}'", INTERNAL_DATE, dateString);
					qb.mustNot(QueryBuilders.rangeQuery(INTERNAL_DATE).gte(dateString).lte(dateString)
							.format(DATE_FORMAT));
					return matcher.group(0);
				}
				if (certain) {
					logger.info("termQuery must '{}' eq '{}'", INTERNAL_DATE, dateString);
					qb.must(QueryBuilders.rangeQuery(INTERNAL_DATE).gte(dateString).lte(dateString)
							.format(DATE_FORMAT));
				} else {
					logger.info("termQuery should '{}' eq '{}'", INTERNAL_DATE, dateString);
					qb.should(QueryBuilders.rangeQuery(INTERNAL_DATE).gte(dateString).lte(dateString)
							.format(DATE_FORMAT));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// Header
	class HeaderAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("header (\\S+) \"([^\"]*)\" ", Pattern.CASE_INSENSITIVE);

		protected HeaderAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String field = matcher.group(1).toLowerCase();
				// Checks no ':' in header field name (according to RFC 822)
				if (field.contains(":")) {
					return matcher.group(0);
				}
				String body = matcher.group(2);
				// Checks body is an empty string -> just check field exists

				if (!positive) {
					if (body.isBlank()) {
						logger.info("must not existsQuery 'headers.{}'", field);
						qb.mustNot(QueryBuilders.existsQuery("headers." + field));
					} else {
						// Special case for X-BM-ExternalID, as the ES mapping has another name for
						// keyword type
						field = (!field.equalsIgnoreCase(XBMEXTERNALID)) ? field : field + ".keyword";
						logger.info("must not be termQuery 'headers.{}:{}'", field, body);
						qb.mustNot(QueryBuilders.termQuery("headers." + field, body));
					}
					return matcher.group(0);
				}

				if (body.isBlank()) {
					if (certain) {
						logger.info("must existsQuery 'headers.{}'", field);
						qb.must(QueryBuilders.existsQuery("headers." + field));
					} else {
						logger.info("should existsQuery 'headers.{}'", field);
						qb.should(QueryBuilders.existsQuery("headers." + field));
					}
				} else {
					// Special case for X-BM-ExternalID, as the ES mapping has another name for
					// keyword type
					field = (!field.equalsIgnoreCase(XBMEXTERNALID)) ? field : field + ".keyword";
					if (certain) {
						logger.info("must be termQuery 'headers." + field + ":" + body + "'");
						qb.must(QueryBuilders.termQuery("headers." + field, body));
					} else {
						logger.info("should be termQuery 'headers." + field + ":" + body + "'");
						qb.should(QueryBuilders.termQuery("headers." + field, body));
					}
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	class TextAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern
				.compile("(?<!not_)(to|from|cc|bcc|body|subject|text) \"([^\"]*)\" ", Pattern.CASE_INSENSITIVE);

		protected TextAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String key = matcher.group(1).toLowerCase();
				String value = matcher.group(2);
				if (key.equals("body") | key.equals("text")) {
					key = "content";
				}
				Map<String, Float> fields = new HashMap<>();
				fields.put(key, 1.0F);

				if (!positive) {
					logger.info("matchPhrase must not '{}'={}'", key, value);
					qb.mustNot(JoinQueryBuilders.hasParentQuery(PARENT_TYPE, QueryBuilders.matchPhraseQuery(key, value),
							false));
					return matcher.group(0);
				}

				if (certain) {
					logger.info("matchPhrase must '{}':{}'", key, value);
					qb.must(JoinQueryBuilders.hasParentQuery(PARENT_TYPE, QueryBuilders.matchPhraseQuery(key, value),
							false));
				} else {
					logger.info("matchPhrase should '{}':{}'", key, value);
					qb.should(JoinQueryBuilders.hasParentQuery(PARENT_TYPE, QueryBuilders.matchPhraseQuery(key, value),
							false));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	class SequenceAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(?:uid )?([\\d\\*:,]+) ", Pattern.CASE_INSENSITIVE);

		protected SequenceAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQueryBuilder qb, String query, boolean positive, boolean certain, long maxUid) {
			Set<Long> set = new HashSet<>();
			long lowerBond = 0L;
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String sequence = matcher.group(1);
				List<String> listSeq = Arrays.asList(sequence.split(","));
				for (String seq : listSeq) {
					if (!seq.contains(":")) {
						// No sequence like 4
						Long l = Long.parseLong(seq);
						set.add(l);
					} else {
						// Sequence like 4:7 or 4:*
						if (seq.contains("*")) {
							// Unbounded case like 4:* or *:4
							String lower = seq.replace("*", "").replace(":", "");
							Long l = Long.parseLong(lower);
							if (l > lowerBond) {
								lowerBond = l;
							}
						} else {
							// Bounded case like 4:7
							if (seq.split(":").length > 2) {
								continue;
							}
							Integer firstDigit = Integer.parseInt(seq.split(":")[0]);
							Integer secondDigit = Integer.parseInt(seq.split(":")[1]);
							Long urBound = (long) Math.max(firstDigit, secondDigit);
							Long lBound = (long) Math.min(firstDigit, secondDigit);
							for (long l = lBound; l <= urBound; l++) {
								set.add(l);
							}
						}
					}
				}
				if (lowerBond > 0 && lowerBond < maxUid) {
					var remainingUids = LongStream.range(lowerBond, maxUid + 1).boxed().toList();
					set.addAll(remainingUids);
				}
				if (positive) {
					logger.info("termsQuery 'uid' must be in {}", set);
					qb.must(QueryBuilders.termsQuery("uid", set));
				} else {
					logger.info("termsQuery 'uid' must not be in {}", set);
					qb.mustNot(QueryBuilders.termsQuery("uid", set));
				}
				return matcher.group(0);
			}
			return null;
		}

	}
}
