export const CRITERIA_TARGETS = {
    BODY: { type: "BODY", pattern: "BODY" },
    FROM: { type: "FROM", pattern: "FROM" },
    SUBJECT: { type: "SUBJECT", pattern: "SUBJECT" },
    TO: { type: "TO", pattern: "TO" },
    HEADER: { type: "HEADER", pattern: "[\\w-]+" }
};

export const CRITERIA_MATCHERS = {
    CONTAINS: "CONTAINS",
    DOESNOTCONTAIN: "DOESNOTCONTAIN",
    DOESNOTEXIST: "DOESNOTEXIST",
    DOESNOTMATCH: "DOESNOTMATCH",
    EXISTS: "EXISTS",
    IS: "IS",
    ISNOT: "ISNOT",
    MATCHES: "MATCHES"
};

const CRITERIA_REVERSED_MATCHERS = {
    CONTAINS: "DOESNOTCONTAIN",
    DOESNOTCONTAIN: "CONTAINS",
    DOESNOTEXIST: "EXISTS",
    DOESNOTMATCH: "MATCHES",
    EXISTS: "DOESNOTEXIST",
    IS: "ISNOT",
    ISNOT: "IS",
    MATCHES: "DOESNOTMATCH"
};

export const ACTIONS = {
    DELETE: { type: "delete", isValid: Boolean },
    DELIVER: { type: "deliver", isValid: Boolean },
    DISCARD: { type: "discard", isValid: Boolean },
    FORWARD: { type: "forward", isValid: value => value?.emails?.length > 0 },
    READ: { type: "read", isValid: Boolean },
    STAR: { type: "star", isValid: Boolean }
};

export const MATCH_ALL = "MATCHALL";

/**
 * Criteria capturing regex.
 * @example
 *     /(?<target>FROM|SUBJECT):(?<matcher>DOESNOTMATCH|CONTAINS):(?<value>.*?)(?=(FROM|SUBJECT):|\n|$)/
 */
const targetsRegexString = Object.values(CRITERIA_TARGETS)
        .map(ct => ct.pattern)
        .join("|"),
    tRe = targetsRegexString;
const matchersRegexString = Object.values(CRITERIA_MATCHERS).join("|"),
    mRe = matchersRegexString;
const criteriaRegexString = `(?<target>${tRe}):(?<matcher>${mRe}):(?<value>.*?)(?=(${tRe}):|\n|$)`;
const criteriaRegex = new RegExp(criteriaRegexString, "gm");

function parseCriteria(rawCriteria) {
    const results = rawCriteria.matchAll(criteriaRegex);
    const criteriaTargets = Object.values(CRITERIA_TARGETS);
    return Array.from(results, r => ({
        target: {
            type: criteriaTargets.find(ct => r.groups.target.match(ct.pattern))?.type,
            name: r.groups.target
        },
        matcher: r.groups.matcher,
        value: r.groups.value.trim()
    }));
}

function stringifyCriteria(criteria) {
    return criteria.length === 0
        ? MATCH_ALL
        : criteria.reduce(
              (criteriaString, criterion) =>
                  `${criteriaString}${criterion.target.name || criterion.target.type}:${criterion.matcher}: ${
                      criterion.value || ""
                  }\n`,
              ""
          );
}

function extractActions(rawFilter) {
    return Object.values(ACTIONS)
        .map(action => extractAction(rawFilter, action))
        .filter(Boolean);
}

function extractAction(rawFilter, action) {
    const value = rawFilter[action.type];
    return action.isValid(value) ? { type: action.type, value } : undefined;
}

export function createEmpty() {
    return { criteria: [], actions: [], name: "", exceptions: [] };
}

export function read(rawFilter) {
    return {
        active: rawFilter.active,
        name: rawFilter.name,
        terminal: rawFilter.stop === undefined ? true : rawFilter.stop,
        criteria: parseCriteria(rawFilter.criteria),
        actions: extractActions(rawFilter),
        exceptions: []
    };
}

export function write(filter) {
    const filterForwardValue = filter.actions.find(a => a.type === ACTIONS.FORWARD.type)?.value;
    return {
        active: filter.active,
        criteria: stringifyCriteria(filter.criteria),
        delete: filter.actions.find(a => a.type === ACTIONS.DELETE.type)?.value || false,
        deliver: filter.actions.find(a => a.type === ACTIONS.DELIVER.type)?.value || undefined,
        discard: filter.actions.find(a => a.type === ACTIONS.DISCARD.type)?.value || false,
        forward: filterForwardValue
            ? {
                  enabled: true,
                  ...filterForwardValue
              }
            : undefined,
        name: filter.name || undefined,
        read: filter.actions.find(a => a.type === ACTIONS.READ.type)?.value || false,
        star: filter.actions.find(a => a.type === ACTIONS.STAR.type)?.value || false,
        stop: filter.terminal
    };
}

export function reverseMatcher(matcherName) {
    return CRITERIA_REVERSED_MATCHERS[matcherName];
}

export function toString(filter, i18n) {
    const and = ` ${i18n.t("common.and")} `;

    const actions = filter.actions
        .map(action =>
            i18n.t(`preferences.mail.filters.action.${action.type}`, {
                value:
                    action.type === ACTIONS.FORWARD.type ? action.value.emails.join(i18n.t("common.and")) : action.value
            })
        )
        .join(and);

    const criteria = filter.criteria
        .map(criterion => {
            const target = i18n.t(`preferences.mail.filters.target.${criterion.target.type}`, {
                name: criterion.target.name
            });
            const matcher = i18n.t(`preferences.mail.filters.matcher.${criterion.matcher}`);
            return `${target} ${matcher} ${criterion.value}`;
        })
        .join(and);

    const exceptions = filter.exceptions
        .map(exception => {
            const target = i18n.t(`preferences.mail.filters.target.${exception.target.type}`, {
                name: exception.target.name
            });
            const matcher = i18n.t(`preferences.mail.filters.matcher.${reverseMatcher(exception.matcher)}`);
            return `${target} ${matcher} ${exception.value}`;
        })
        .join(and);

    const conditions = [criteria, exceptions].filter(Boolean).join(and);

    return [actions, conditions].join(` ${i18n.t("common.if")} `);
}
