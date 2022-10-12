import { ACTIONS } from "../filterRules.js";
import PrefFilterRuleActionDeliver from "./PrefFilterRuleActionDeliver";
import PrefFilterRuleActionForward from "./PrefFilterRuleActionForward";
import PrefFilterRuleActionNoValue from "./PrefFilterRuleActionNoValue";
import PrefFilterRuleFolderActionEditor from "./PrefFilterRuleFolderActionEditor";
import PrefFilterRuleForwardActionEditor from "./PrefFilterRuleForwardActionEditor";

export default [
    {
        match: action => [ACTIONS.DISCARD.name, ACTIONS.READ.name, ACTIONS.STAR.name].includes(action.name),
        text,
        value: true,
        viewer: PrefFilterRuleActionNoValue
    },
    {
        match: action => action.name === ACTIONS.DELIVER.name,
        text,
        viewer: PrefFilterRuleActionDeliver,
        editor: PrefFilterRuleFolderActionEditor
    },
    {
        match: action => action.name === ACTIONS.FORWARD.name,
        text,
        viewer: PrefFilterRuleActionForward,
        editor: PrefFilterRuleForwardActionEditor
    }
];

function text(action, i18n) {
    return i18n.t(`preferences.mail.filters.action.${action.name}`, { value: "" });
}
