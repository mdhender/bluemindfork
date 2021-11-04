import criteriaDefinitions from "./criteria.js";
import { CRITERIA_MATCHERS, CRITERIA_TARGETS, reverseMatcher } from "../filterRules.js";

export function resolve(criterion, vm) {
    const definition = criteriaDefinitions.sort((a, b) => b.priority - a.priority).find(item => item.match(criterion));
    if (definition) {
        return {
            ...criterion,
            viewer: definition.viewer,
            editor: definition.editor,
            positive: !criterion.matcher.includes("NOT"),
            getName: negates => {
                if (negates) {
                    const reversedMatcher = reverseMatcher(criterion.matcher);
                    const reversedCriterion = { target: criterion.target, matcher: reversedMatcher };
                    return resolve(reversedCriterion, vm)?.getName();
                }
                return definition.name(criterion, vm.$i18n);
            },
            fullEditor: definition.fullEditor
        };
    }
}

export function all(vm) {
    const result = [];
    Object.values(CRITERIA_TARGETS).forEach(target => {
        Object.values(CRITERIA_MATCHERS).forEach(matcher => {
            const resolved = resolve({ target, matcher }, vm);
            if (resolved) {
                result.push(resolved);
            }
        });
    });
    return result.sort((a, b) => b.priority - a.priority);
}
