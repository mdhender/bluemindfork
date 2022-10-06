import { withAlert as _withAlert } from "@bluemind/alert.store";

export function withAlert(action, actionName = "", options = {}) {
    const name = "filehosting." + (actionName || action.name);
    return _withAlert(action, name, options);
}
