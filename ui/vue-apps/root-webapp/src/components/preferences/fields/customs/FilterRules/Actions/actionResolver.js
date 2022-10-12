import actions from "./actions.js";
import { ACTIONS } from "../filterRules.js";

export function resolve(action, vm) {
    const resolved = actions.find(a => a.match(action));
    if (resolved) {
        return {
            ...action,
            ...resolved,
            text: resolved.text(action, vm.$i18n)
        };
    }
}

export function all(vm) {
    const result = [];
    Object.values(ACTIONS).forEach(action => {
        const resolved = resolve(action, vm);
        if (resolved) {
            result.push({ ...resolved });
        }
    });
    return result;
}
