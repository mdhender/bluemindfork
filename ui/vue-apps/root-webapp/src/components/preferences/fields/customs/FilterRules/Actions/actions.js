import { ACTIONS } from "../filterRules.js";
import PrefFilterRuleActionDeliver from "./PrefFilterRuleActionDeliver";
import PrefFilterRuleActionForward from "./PrefFilterRuleActionForward";
import PrefFilterRuleActionNoValue from "./PrefFilterRuleActionNoValue";
import PrefFilterRuleFolderActionEditor from "./PrefFilterRuleFolderActionEditor";
import PrefFilterRuleForwardActionEditor from "./PrefFilterRuleForwardActionEditor";

export default [
    {
        match: action => [ACTIONS.DELETE.type, ACTIONS.READ.type, ACTIONS.STAR.type].includes(action.type),
        name: (action, i18n) => defaultName(action, i18n),
        value: true,
        viewer: PrefFilterRuleActionNoValue
    },
    {
        match: action => action.type === ACTIONS.DELIVER.type,
        name: (action, i18n) => defaultName(action, i18n),
        viewer: PrefFilterRuleActionDeliver,
        editor: PrefFilterRuleFolderActionEditor
    },
    {
        match: action => action.type === ACTIONS.FORWARD.type,
        name: (action, i18n) => defaultName(action, i18n),
        viewer: PrefFilterRuleActionForward,
        editor: PrefFilterRuleForwardActionEditor
    }
];

function defaultName(action, i18n) {
    return i18n.t(`preferences.mail.filters.action.${action.type}`, { value: "" });
}
