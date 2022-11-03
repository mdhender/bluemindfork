export const CRITERIA_TARGETS = {
    BODY: "part.content",
    FROM: "from.email",
    SUBJECT: "subject",
    TO: "to.email",
    HEADER: "headers"
};

export const CRITERIA_MATCHERS = {
    CONTAINS: "CONTAINS",
    EXISTS: "EXISTS",
    EQUALS: "EQUALS",
    MATCHES: "MATCHES"
};

export const ACTIONS = {
    DELETE: { name: "MARK_AS_DELETED", isValid: Boolean },
    DELIVER: { name: "MOVE", isValid: Boolean },
    COPY: { name: "COPY", isValid: Boolean },
    DISCARD: { name: "DISCARD", isValid: Boolean },
    FORWARD: { name: "REDIRECT", isValid: action => action.emails?.length > 0 },
    TRANSFER: { name: "TRANSFER", isValid: action => action.emails?.length > 0 },
    READ: { name: "MARK_AS_READ", isValid: Boolean },
    STAR: { name: "MARK_AS_IMPORTANT", isValid: Boolean }
};

const ACTIONS_BY_NAME = new Map(Object.values(ACTIONS).map(action => [action.name, action]));

export function read(rules) {
    return rules.map((rule, index) => {
        const manageable = isManageable(rule);
        const internalRule = manageable ? readRule(rule) : rule;
        return {
            ...internalRule,
            index,
            terminal: rule.stop,
            editable: true,
            manageable
        };
    });
}

function isManageable(rule) {
    const hasOnlyFieldConditions = rule.conditions.every(condition => condition.filter);
    const hasOnlyAndConditions = rule.conditions.every(condition => condition.operator === "AND");
    return (
        rule.client === "bluemind" &&
        rule.trigger === "IN" &&
        !rule.deferred &&
        hasOnlyFieldConditions &&
        hasOnlyAndConditions
    );
}

export function readRule(rawFilter) {
    const conditionsAndExceptions = readCriteria(rawFilter.conditions);
    return {
        active: rawFilter.active,
        name: rawFilter.name,
        terminal: rawFilter.stop === undefined ? true : rawFilter.stop,
        criteria: conditionsAndExceptions.filter(condition => !condition.exception),
        exceptions: conditionsAndExceptions.filter(condition => condition.exception),
        actions: readActions(rawFilter.actions)
    };
}

function readCriteria(rawConditions) {
    return rawConditions.map(rawCondition => {
        const filter = rawCondition.filter;
        return {
            target: readTarget(filter.fields[0]),
            matcher: filter.operator,
            value: filter.values && filter.values.length > 0 ? filter.values[0] : undefined,
            exception: rawCondition.negate
        };
    });
}

function readTarget(field) {
    const fieldTokens = field.split(".");
    return {
        type: field.startsWith("headers") ? fieldTokens[0] : field,
        name: field.startsWith("headers") ? fieldTokens[1] : undefined
    };
}

function readActions(rawActions) {
    return rawActions
        .map(rawAction => (ACTIONS_BY_NAME.get(rawAction.name)?.isValid(rawAction) ? { ...rawAction } : undefined))
        .filter(Boolean);
}

export function createEmpty() {
    return { criteria: [], actions: [], name: "", exceptions: [], manageable: true };
}

export function write(filter) {
    if (!filter.manageable) {
        return filter;
    }
    const conditions = filter.criteria.map(writeCondition);
    const exceptions = filter.exceptions.map(writeCondition);
    return {
        client: "bluemind",
        type: "GENERIC",
        trigger: "IN",
        deferred: false,
        active: filter.active,
        name: filter.name || undefined,
        conditions: conditions.concat(exceptions),
        actions: writeActions(filter.actions),
        stop: filter.terminal
    };
}

function writeCondition(criterion) {
    const field =
        criterion.target.type === "headers"
            ? criterion.target.type + "." + criterion.target.name
            : criterion.target.type;
    const fields = field === "to.email" ? [field, "cc.email"] : [field];
    const condition = {
        negate: criterion.exception,
        operator: "AND",
        conditions: [],
        filter: {
            fields,
            operator: criterion.matcher
        }
    };
    if (criterion.matcher !== "EXISTS") {
        condition.filter.values = criterion.value ? [criterion.value] : [];
    }
    return condition;
}

function writeActions(actions) {
    return actions
        .map(action => (ACTIONS_BY_NAME.get(action.name)?.isValid(action) ? { ...action } : undefined))
        .filter(Boolean);
}

export function toString(filter, i18n) {
    const and = ` ${i18n.t("common.and")} `;

    const actions = filter.actions
        .map(action => {
            let value;
            if ([ACTIONS.FORWARD.name, ACTIONS.TRANSFER.name].includes(action.name)) {
                value = action.emails.join(and);
            } else if (ACTIONS.DELIVER.name === action.name) {
                value = action.folder;
            } else {
                value = action.value;
            }
            return i18n.t(`preferences.mail.filters.action.${action.name}`, { value });
        })
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
            const matcher = i18n.t(`preferences.mail.filters.matcher.${exception.matcher}.negate`);
            return `${target} ${matcher} ${exception.value}`;
        })
        .join(and);

    const conditions = [criteria, exceptions].filter(Boolean).join(and);

    return [actions, conditions].join(` ${i18n.t("common.if")} `);
}
