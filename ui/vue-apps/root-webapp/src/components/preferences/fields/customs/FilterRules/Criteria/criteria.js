import { CRITERIA_MATCHERS, CRITERIA_TARGETS } from "../filterRules.js";
import PrefFilterRuleCriterion from "./PrefFilterRuleCriterion";
import PrefFilterRuleContactCriterion from "./PrefFilterRuleContactCriterion";
import PrefFilterRuleContactCriterionEditor from "./PrefFilterRuleContactCriterionEditor";
import PrefFilterRuleHeaderCriterionEditor from "./PrefFilterRuleHeaderCriterionEditor";
import PrefFilterRuleTextCriterion from "./PrefFilterRuleTextCriterion";
import PrefFilterRuleTextCriterionEditor from "./PrefFilterRuleTextCriterionEditor";

export default [
    {
        match: criterion =>
            [CRITERIA_TARGETS.FROM.type, CRITERIA_TARGETS.TO.type].includes(criterion.target.type) &&
            [CRITERIA_MATCHERS.IS, CRITERIA_MATCHERS.ISNOT].includes(criterion.matcher),

        name: (criterion, i18n) => defaultName(criterion, i18n),
        viewer: PrefFilterRuleContactCriterion,
        editor: PrefFilterRuleContactCriterionEditor,
        priority: 0
    },
    {
        match: criterion =>
            [
                CRITERIA_TARGETS.FROM.type,
                CRITERIA_TARGETS.TO.type,
                CRITERIA_TARGETS.BODY.type,
                CRITERIA_TARGETS.SUBJECT.type
            ].includes(criterion.target.type) &&
            [CRITERIA_MATCHERS.CONTAINS, CRITERIA_MATCHERS.DOESNOTCONTAIN].includes(criterion.matcher),
        name: (criterion, i18n) => defaultName(criterion, i18n),
        viewer: PrefFilterRuleTextCriterion,
        editor: PrefFilterRuleTextCriterionEditor,
        priority: 0
    },
    {
        match: criterion =>
            [CRITERIA_TARGETS.SUBJECT.type].includes(criterion.target.type) &&
            [CRITERIA_MATCHERS.IS, CRITERIA_MATCHERS.ISNOT].includes(criterion.matcher),
        name: (criterion, i18n) => defaultName(criterion, i18n),
        viewer: PrefFilterRuleTextCriterion,
        editor: PrefFilterRuleTextCriterionEditor,
        priority: 0
    },
    {
        match: criterion =>
            [CRITERIA_TARGETS.HEADER.type].includes(criterion.target.type) &&
            [
                CRITERIA_MATCHERS.IS,
                CRITERIA_MATCHERS.ISNOT,
                CRITERIA_MATCHERS.CONTAINS,
                CRITERIA_MATCHERS.DOESNOTCONTAIN
            ].includes(criterion.matcher),
        name: (criterion, i18n) => defaultName(criterion, i18n),
        viewer: PrefFilterRuleTextCriterion,
        editor: PrefFilterRuleHeaderCriterionEditor,
        fullEditor: true,
        priority: 0
    },
    {
        match: criterion =>
            [CRITERIA_TARGETS.HEADER.type].includes(criterion.target.type) &&
            [CRITERIA_MATCHERS.EXISTS, CRITERIA_MATCHERS.DOESNOTEXIST].includes(criterion.matcher),
        name: (criterion, i18n) => defaultName(criterion, i18n),
        viewer: PrefFilterRuleCriterion,
        editor: PrefFilterRuleHeaderCriterionEditor,
        fullEditor: true,
        priority: 0
    }
];

function defaultName(criterion, i18n) {
    const target = i18n.t(`preferences.mail.filters.target.${criterion.target.type}`, { name: criterion.target.name });
    const matcher = i18n.t(`preferences.mail.filters.matcher.${criterion.matcher}`);
    return `${target} ${matcher}`;
}
