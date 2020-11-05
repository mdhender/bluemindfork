import UUIDGenerator from "@bluemind/uuid";
import AlertTypes from "./AlertTypes";
import { ERROR, LOADING, SUCCESS } from "./types";

const OPTIONS = { root: true };

export function withAlert(action, actionName, renderer) {
    const name = actionName || action.name;
    return async (store, payload) => {
        const uid = UUIDGenerator.generate();
        const alert = { name, uid, renderer, payload };
        store.commit("alert/" + LOADING, { ...alert, type: AlertTypes.LOADING }, OPTIONS);
        try {
            const result = await action(store, payload);
            store.commit("alert/" + SUCCESS, { ...alert, result, type: AlertTypes.SUCCESS }, OPTIONS);
            return result;
        } catch (error) {
            store.commit("alert/" + ERROR, { ...alert, error, type: AlertTypes.ERROR }, OPTIONS);
            throw error;
        }
    };
}
