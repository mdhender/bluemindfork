import criteriaDefinitions from "./criteria.js";
import { CRITERIA_MATCHERS, CRITERIA_TARGETS } from "../filterRules.js";

export function resolve(criterion, vm) {
    const definition = criteriaDefinitions.sort((a, b) => b.priority - a.priority).find(item => item.match(criterion));
    if (definition) {
        return {
            ...criterion,
            viewer: definition.viewer,
            editor: definition.editor,
            text: definition.name(criterion, vm.$i18n),
            fullEditor: definition.fullEditor
        };
    }
}

export function all(vm) {
    const result = [];
    Object.values(CRITERIA_TARGETS).forEach(target => {
        Object.values(CRITERIA_MATCHERS).forEach(matcher => {
            const resolved = resolve({ target: { type: target }, matcher }, vm);
            if (resolved) {
                result.push(resolved);
            }
        });
    });
    return result.sort((a, b) => b.priority - a.priority);
}
