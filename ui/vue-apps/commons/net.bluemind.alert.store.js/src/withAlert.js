import clone from "lodash.clone";
import UUIDGenerator from "@bluemind/uuid";
import { ERROR, LOADING, SUCCESS } from "./types";

const OPTIONS = { root: true };

export function withAlert(action, actionName = "", opt_options = {}) {
    const options = { renderer: typeof opt_options === "string" ? opt_options : "DefaultAlert", ...opt_options };
    const name = actionName || action.name;
    return async (store, _payload) => {
        const payload = clone(_payload);
        const uid = UUIDGenerator.generate();
        store.dispatch("alert/" + LOADING, { alert: { name, uid, payload }, options }, OPTIONS);
        try {
            const _result = await action(store, _payload);
            const result = clone(_result);
            store.dispatch("alert/" + SUCCESS, { alert: { name, uid, payload, result }, options }, OPTIONS);
            return _result;
        } catch (error) {
            store.dispatch("alert/" + ERROR, { alert: { name, uid, payload, error }, options }, OPTIONS);
            throw error;
        }
    };
}
