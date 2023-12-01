package net.bluemind.imap.driver.mailapi;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import net.bluemind.imap.endpoint.driver.ImapIdSet;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.lib.elasticsearch.IndexAliasMapping;

public class UidSearchAnalyzer {

	private static final Logger logger = LoggerFactory.getLogger(UidSearchAnalyzer.class);
	private static final String XBMEXTERNALID = "X-BM-ExternalID";
	private static final String INTERNAL_DATE = "internalDate";
	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
	private static final String DATE_FORMAT = "d-MMM-yyyy";
	private static final String PARENT_TYPE = "body";

	private static final Pattern RE_ALL = Pattern.compile("all ", Pattern.CASE_INSENSITIVE);
	private static final Pattern RE_SEQUENCE = Pattern.compile("(?:uid )?([\\d\\*:,]+)", Pattern.CASE_INSENSITIVE);

	private static Map<String, IUidSearchMatcher> analyzers;
	static {
		analyzers = new HashMap<>();
		analyzers.put("ALL", null);
		analyzers.put("ANSWERED", new FlagAnalyzer());
		analyzers.put("DELETED", new FlagAnalyzer());
		analyzers.put("FLAGGED", new FlagAnalyzer());
		analyzers.put("DRAFT", new FlagAnalyzer());
		analyzers.put("SEEN", new FlagAnalyzer());
		analyzers.put("UNANSWERED", new UnFlagAnalyzer());
		analyzers.put("UNDELETED", new UnFlagAnalyzer());
		analyzers.put("UNFLAGGED", new UnFlagAnalyzer());
		analyzers.put("UNDRAFT", new UnFlagAnalyzer());
		analyzers.put("UNSEEN", new UnseenAnalyzer());
		analyzers.put("BEFORE", new SmallerDateAnalyzer());
		analyzers.put("ON", new EqualsDateAnalyzer());
		analyzers.put("SINCE", new GreaterDateAnalyzer());
		analyzers.put("SENTBEFORE", new SmallerDateAnalyzer());
		analyzers.put("SENTON", new EqualsDateAnalyzer());
		analyzers.put("SENTSINCE", new GreaterDateAnalyzer());
		analyzers.put("SMALLER", new SmallerAnalyzer());
		analyzers.put("LARGER", new LargerAnalyzer());
		analyzers.put("HEADER", new HeaderAnalyzer());
		analyzers.put("KEYWORD", new KeywordAnalyzer());
		analyzers.put("UNKEYWORD", new UnKeywordAnalyzer());
		analyzers.put("UID", new SequenceAnalyzer());
		analyzers.put("BCC", new TextAnalyzer());
		analyzers.put("BODY", new TextAnalyzer());
		analyzers.put("CC", new TextAnalyzer());
		analyzers.put("FROM", new TextAnalyzer());
		analyzers.put("SUBJECT", new TextAnalyzer());
		analyzers.put("TEXT", new TextAnalyzer());
		analyzers.put("TO", new TextAnalyzer());
	}

	@SuppressWarnings("serial")
	public static class UidSearchException extends Exception {
		public UidSearchException(String message) {
			super(message);
		}
	}

	private UidSearchAnalyzer() {
	}

	public static QueryBuilderResult buildQuery(ElasticsearchClient esClient, String query, SelectedFolder selected,
			ImapIdSetResolver resolver, String meUid) throws UidSearchException {
		String folderUid = selected.folder.uid;
		query = query + " END";
		boolean hasSequence = false;

		SearchResponse<Void> response;
		try {
			response = esClient.search(s -> {
				String alias = IndexAliasMapping.get().getReadAliasByMailboxUid(meUid);
				return s.index(alias).size(0) //
						.query(q -> q.bool(b -> b //
								.must(m -> m.term(t -> t.field("owner").value(meUid)))
								.must(m -> m.term(t -> t.field("in").value(folderUid))))) //
						.aggregations("uid_max", a -> a.max(m -> m.field("uid")));
			}, Void.class);

		} catch (ElasticsearchException | IOException e) {
			throw new UidSearchException("Unable to search max uid in 'mailspool_alias_" + meUid + "'");
		}

		Aggregate aggregate = response.aggregations().get("uid_max");
		long maxUid = (aggregate != null) ? (long) aggregate.max().value() : 0l;

		String subQuery = query;

		// false when NOT keyword is present
		boolean positiveKeyword = true;
		// true when OR keyword is present
		boolean isCertain = true;
		// count for OR condition
		int orCount = 0;

		// Builder can only be built once
		BoolQuery.Builder qb = new BoolQuery.Builder() //
				.must(m -> m.term(t -> t.field("in").value(folderUid)));
		BoolQuery.Builder qbShould = new BoolQuery.Builder();
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
			IUidSearchMatcher analyzer = UidSearchAnalyzer.analyzers.get(keyword);
			if (analyzer != null) {
				String analyzedQuery = null;
				if (isCertain) {
					analyzedQuery = analyzer.analyse(qb, subQuery, positiveKeyword, isCertain, maxUid, selected,
							resolver);
				} else {
					analyzedQuery = analyzer.analyse(qbShould, subQuery, positiveKeyword, isCertain, maxUid, selected,
							resolver);
				}
				if (analyzedQuery == null) {
					throw new UidSearchException("Invalid Search criteria");
				}
				len = analyzedQuery.length();
			} else {
				if (UidSearchAnalyzer.hasSequence(subQuery)) {
					analyzer = UidSearchAnalyzer.analyzers.get("UID");
					String analyzedQuery = null;
					if (isCertain) {
						analyzedQuery = analyzer.analyse(qb, subQuery, positiveKeyword, isCertain, maxUid, selected,
								resolver);
					} else {
						analyzedQuery = analyzer.analyse(qbShould, subQuery, positiveKeyword, isCertain, maxUid,
								selected, resolver);
					}
					if (analyzedQuery == null) {
						throw new UidSearchException("Invalid Search criteria (qb: " + qb + " qbShould: " + qbShould
								+ " subquery: " + subQuery + ")");
					}
					len = analyzedQuery.length();
					hasSequence = true;
				} else if (UidSearchAnalyzer.hasAll(subQuery)) {
					len = 4;
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
				qb.must(qbShould.build()._toQuery());
				qbShould = new BoolQuery.Builder();
			}
			positiveKeyword = true;
			subQuery = subQuery.substring(len);
		}
		return new QueryBuilderResult(qb.build()._toQuery(), hasSequence);
	}

	public record QueryBuilderResult(Query q, boolean hasSequence) {
	}

	public static boolean hasSequence(String query) {
		return RE_SEQUENCE.matcher(query).find();
	}

	public static boolean hasAll(String query) {
		return RE_ALL.matcher(query).find();
	}

	// Not NOT, positive flag term
	public static class FlagAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(answered|deleted|draft|flagged|seen) ",
				Pattern.CASE_INSENSITIVE);

		public FlagAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String gr = matcher.group(1);
				if (!positive) {
					qb.mustNot(m -> m.term(t -> t.field("is").value(gr.toLowerCase())));
					return matcher.group(0);
				}
				if (certain) {
					logger.info("termQuery must 'is' with '{}'", gr);
					qb.must(m -> m.term(t -> t.field("is").value(gr.toLowerCase())));
				} else {
					logger.info("termQuery should 'is' with '{}'", gr);
					qb.should(m -> m.term(t -> t.field("is").value(gr.toLowerCase())));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// Not NOT, negative flag term
	public static class UnFlagAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("un(answered|deleted|draft|flagged) ",
				Pattern.CASE_INSENSITIVE);

		protected UnFlagAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);

			if (matcher.find()) {
				String gr = matcher.group(1);

				if (positive) {
					logger.info("termQuery must not 'is' with '{}'", gr);
					qb.mustNot(m -> m.term(t -> t.field("is").value(gr.toLowerCase())));
					return matcher.group();
				}
				if (certain) {
					logger.info("termQuery must 'is' with '{}'", gr);
					qb.must(m -> m.term(t -> t.field("is").value(gr.toLowerCase())));
				} else {
					logger.info("termQuery should 'is' with '{}'", gr);
					qb.should(m -> m.term(t -> t.field("is").value(gr.toLowerCase())));
				}
				return matcher.group();
			}
			return null;
		}

	}

	// Not NOT, unseen term
	public static class UnseenAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(unseen) ", Pattern.CASE_INSENSITIVE);

		protected UnseenAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);

			if (matcher.find()) {
				String gr = matcher.group(1);

				if (!positive) {
					logger.info("termQuery must 'is' with '{}'", gr);
					qb.mustNot(m -> m.term(t -> t.field("is").value(gr.toLowerCase())));
					return matcher.group(0);
				}
				if (certain) {
					logger.info("termQuery must 'is' with '{}'", gr);
					qb.must(m -> m.term(t -> t.field("is").value(gr.toLowerCase())));
				} else {
					logger.info("termQuery should 'is' with '{}'", gr);
					qb.should(m -> m.term(t -> t.field("is").value(gr.toLowerCase())));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	public static class KeywordAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("keyword (\\S+) ", Pattern.CASE_INSENSITIVE);

		protected KeywordAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);

			if (matcher.find()) {
				String flag = matcher.group(1);

				if (!positive) {
					logger.info("termQuery must not 'is' with '{}'", flag);
					qb.mustNot(m -> m.term(t -> t.field("is").value(flag.toLowerCase())));
					return matcher.group(0);
				}

				if (certain) {
					logger.info("termQuery must 'is' with '{}'", flag);
					qb.must(m -> m.term(t -> t.field("is").value(flag.toLowerCase())));
				} else {
					logger.info("termQuery should 'is' with '{}'", flag);
					qb.should(m -> m.term(t -> t.field("is").value(flag.toLowerCase())));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	public static class UnKeywordAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("unkeyword (\\S+) ", Pattern.CASE_INSENSITIVE);

		protected UnKeywordAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);

			if (matcher.find()) {
				String flag = matcher.group(1);

				if (positive) {
					logger.info("termQuery must not 'is' with '{}'", flag);
					qb.mustNot(m -> m.term(t -> t.field("is").value(flag.toLowerCase())));
					return matcher.group(0);
				}

				if (certain) {
					logger.info("termQuery must 'is' with '{}'", flag);
					qb.must(m -> m.term(t -> t.field("is").value(flag.toLowerCase())));
				} else {
					logger.info("termQuery should 'is' with '{}'", flag);
					qb.should(m -> m.term(t -> t.field("is").value(flag.toLowerCase())));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// larger than
	public static class LargerAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("larger (\\d+) ", Pattern.CASE_INSENSITIVE);

		protected LargerAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String size = matcher.group(1);

				if (!positive) {
					if (certain) {
						logger.info("termQuery must 'size' lt '{}'", size);
						qb.must(m -> m.range(r -> r.field("size").lt(JsonData.of(size))));
					} else {
						logger.info("termQuery should 'size' lt '{}'", size);
						qb.should(m -> m.range(r -> r.field("size").lt(JsonData.of(size))));
					}
					return matcher.group(0);
				}
				if (certain) {
					logger.info("rangeQuery must 'size' gt '{}'", size);
					qb.must(m -> m.range(r -> r.field("size").gt(JsonData.of(size))));
				} else {
					logger.info("rangeQuery should 'size' gt '{}'", size);
					qb.should(m -> m.range(r -> r.field("size").gt(JsonData.of(size))));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// Smaller
	public static class SmallerAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("smaller (\\d+) ", Pattern.CASE_INSENSITIVE);

		protected SmallerAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String size = matcher.group(1);
				if (!positive) {
					if (certain) {
						logger.info("termQuery must 'size' gt '{}'", size);
						qb.must(m -> m.range(r -> r.field("size").gt(JsonData.of(size))));
					} else {
						logger.info("termQuery should 'size' gt '{}'", size);
						qb.should(m -> m.range(r -> r.field("size").gt(JsonData.of(size))));
					}
					return matcher.group(0);
				}
				if (certain) {
					logger.info("termQuery must 'size' lt '{}'", size);
					qb.must(m -> m.range(r -> r.field("size").lt(JsonData.of(size))));
				} else {
					logger.info("termQuery should 'size' lt '{}'", size);
					qb.should(m -> m.range(r -> r.field("size").lt(JsonData.of(size))));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// Greater than -> rangeQuery(field).gt(dt.iso8601)
	public static class GreaterDateAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(?:sentsince|since) (\\S+) ", Pattern.CASE_INSENSITIVE);

		protected GreaterDateAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String dateString = matcher.group(1);
				try {
					Date date = DATE_FORMATTER.parse(dateString);

					if (!positive) {
						if (certain) {
							logger.info("termQuery must '{}' lt '{}'", INTERNAL_DATE, dateString);
							qb.must(m -> m.range(r -> r.field(INTERNAL_DATE).lt(JsonData.of(date.getTime()))));
						} else {
							logger.info("termQuery should '{}' lt '{}'", INTERNAL_DATE, dateString);
							qb.should(m -> m.range(r -> r.field(INTERNAL_DATE).lt(JsonData.of(date.getTime()))));
						}
						return matcher.group(0);
					}
					if (certain) {
						logger.info("termQuery must '{}' gt '{}'", INTERNAL_DATE, dateString);
						qb.must(m -> m.range(r -> r.field(INTERNAL_DATE).gt(JsonData.of(date.getTime()))));
					} else {
						logger.info("termQuery should '{}' gt '{}'", INTERNAL_DATE, dateString);
						qb.should(m -> m.range(r -> r.field(INTERNAL_DATE).gt(JsonData.of(date.getTime()))));
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
	public static class SmallerDateAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(?:sentbefore|before) (\\S+) ",
				Pattern.CASE_INSENSITIVE);

		protected SmallerDateAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String dateString = matcher.group(1);
				try {
					Date date = DATE_FORMATTER.parse(dateString);

					if (!positive) {
						if (certain) {
							logger.info("termQuery must '{}' gt '{}'", INTERNAL_DATE, dateString);
							qb.must(m -> m.range(r -> r.field(INTERNAL_DATE).gt(JsonData.of(date.getTime()))));
						} else {
							logger.info("termQuery should '{}' gt '{}'", INTERNAL_DATE, dateString);
							qb.should(m -> m.range(r -> r.field(INTERNAL_DATE).gt(JsonData.of(date.getTime()))));
						}
						return matcher.group(0);
					}
					if (certain) {
						logger.info("termQuery must '{}' lt '{}'", INTERNAL_DATE, dateString);
						qb.must(m -> m.range(r -> r.field(INTERNAL_DATE).lt(JsonData.of(date.getTime()))));
					} else {
						logger.info("termQuery should '{}' lt '{}'", INTERNAL_DATE, dateString);
						qb.should(m -> m.range(r -> r.field(INTERNAL_DATE).lt(JsonData.of(date.getTime()))));
					}
				} catch (ParseException e) {
					logger.error("date parsing error: {}", e.getMessage());
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	public static class EqualsDateAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(?:senton|on) (\\S+) ", Pattern.CASE_INSENSITIVE);

		protected EqualsDateAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				JsonData dateString = JsonData.of(matcher.group(1));

				if (!positive) {
					logger.info("termQuery must not '{}' eq '{}'", INTERNAL_DATE, dateString);
					qb.mustNot(m -> m
							.range(r -> r.field(INTERNAL_DATE).gte(dateString).lte(dateString).format(DATE_FORMAT)));
					return matcher.group(0);
				}
				if (certain) {
					logger.info("termQuery must '{}' eq '{}'", INTERNAL_DATE, dateString);
					qb.must(m -> m
							.range(r -> r.field(INTERNAL_DATE).gte(dateString).lte(dateString).format(DATE_FORMAT)));
				} else {
					logger.info("termQuery should '{}' eq '{}'", INTERNAL_DATE, dateString);
					qb.should(m -> m
							.range(r -> r.field(INTERNAL_DATE).gte(dateString).lte(dateString).format(DATE_FORMAT)));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	// Header
	public static class HeaderAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("header (\\S+) \"([^\"]*)\" ", Pattern.CASE_INSENSITIVE);

		protected HeaderAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
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
						qb.mustNot(m -> m.exists(e -> e.field("headers." + field)));
					} else {
						// Special case for X-BM-ExternalID, as the ES mapping has another name for
						// keyword type
						String fixedField = (!field.equalsIgnoreCase(XBMEXTERNALID)) ? field : field + ".keyword";
						logger.info("must not be termQuery 'headers.{}:{}'", fixedField, body);
						qb.mustNot(m -> m.term(t -> t.field("headers." + fixedField).value(body)));
					}
					return matcher.group(0);
				}

				if (body.isBlank()) {
					if (certain) {
						logger.info("must existsQuery 'headers.{}'", field);
						qb.must(m -> m.exists(e -> e.field("headers." + field)));
					} else {
						logger.info("should existsQuery 'headers.{}'", field);
						qb.should(m -> m.exists(e -> e.field("headers." + field)));
					}
				} else {
					// Special case for X-BM-ExternalID, as the ES mapping has another name for
					// keyword type
					String fixedField = (!field.equalsIgnoreCase(XBMEXTERNALID)) ? field : field + ".keyword";
					if (certain) {
						logger.info("must be termQuery 'headers." + fixedField + ":" + body + "'");
						qb.must(m -> m.term(t -> t.field("headers." + fixedField).value(body)));
					} else {
						logger.info("should be termQuery 'headers." + fixedField + ":" + body + "'");
						qb.should(m -> m.term(t -> t.field("headers." + fixedField).value(body)));
					}
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	public static class TextAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern
				.compile("(?<!not_)(to|from|cc|bcc|body|subject|text) \"([^\"]*)\" ", Pattern.CASE_INSENSITIVE);

		protected TextAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String match = matcher.group(1).toLowerCase();
				String key = (match.equals("body") || match.equals("text")) ? "content" : match;
				String value = matcher.group(2);
				Map<String, Float> fields = new HashMap<>();
				fields.put(key, 1.0F);

				if (!positive) {
					logger.info("matchPhrase must not '{}'='{}'", key, value);
					qb.mustNot(m -> m.hasParent(p -> p.parentType(PARENT_TYPE).score(false) //
							.query(q -> q.matchPhrase(ph -> ph.field(key).query(value)))));
					return matcher.group(0);
				}

				if (certain) {
					logger.info("matchPhrase must '{}':'{}'", key, value);
					qb.must(m -> m.hasParent(p -> p.parentType(PARENT_TYPE).score(false) //
							.query(q -> q.matchPhrase(ph -> ph.field(key).query(value)))));
				} else {
					logger.info("matchPhrase should '{}':'{}'", key, value);
					qb.should(m -> m.hasParent(p -> p.parentType(PARENT_TYPE).score(false) //
							.query(q -> q.matchPhrase(ph -> ph.field(key).query(value)))));
				}
				return matcher.group(0);
			}
			return null;
		}

	}

	public static class SequenceAnalyzer extends UidSearchPatternBasedMatcher {
		private static final Pattern PATTERN = Pattern.compile("(?:uid )?([\\d\\*:,]+) ", Pattern.CASE_INSENSITIVE);

		protected SequenceAnalyzer() {
			super(PATTERN);
		}

		@Override
		public String analyse(BoolQuery.Builder qb, String query, boolean positive, boolean certain, long maxUid,
				SelectedFolder selected, ImapIdSetResolver resolver) {
			Matcher matcher = compiledRE.matcher(query);
			if (matcher.find()) {
				String sequence = matcher.group(1);
				ImapIdSet asSet = query.toLowerCase().startsWith("uid ") ? ImapIdSet.uids(sequence)
						: ImapIdSet.sequences(sequence);
				List<Long> itemIds = resolver.resolveIdSet(selected, asSet);
				// FIXME 1:* produces a shitty query on big folders
				if (positive) {
					logger.info("termsQuery 'itemId' must be in {}", itemIds);
					qb.must(m -> m.terms(
							t -> t.field("itemId").terms(f -> f.value(itemIds.stream().map(FieldValue::of).toList()))));
				} else {
					logger.info("termsQuery 'itemId' must not be in {}", itemIds);
					qb.mustNot(m -> m.terms(
							t -> t.field("itemId").terms(f -> f.value(itemIds.stream().map(FieldValue::of).toList()))));
				}
				return matcher.group(0);
			}
			return null;
		}

	}
}
