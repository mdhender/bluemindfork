import UUIDGenerator from "@bluemind/uuid";
import { ERROR, LOADING, SUCCESS } from "./types";

const OPTIONS = { root: true };

export function withAlert(action, actionName, renderer) {
    const name = actionName || action.name;
    return async (store, payload) => {
        const uid = UUIDGenerator.generate();
        store.commit("alert/" + LOADING, { name, uid, renderer, payload }, OPTIONS);
        try {
            const result = await action(store, payload);
            store.commit("alert/" + SUCCESS, { name, uid, renderer, payload, result }, OPTIONS);
            return result;
        } catch (error) {
            store.commit("alert/" + ERROR, { name, uid, renderer, payload, error }, OPTIONS);
            throw error;
        }
    };
}
