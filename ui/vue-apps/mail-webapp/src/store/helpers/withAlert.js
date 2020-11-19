import { withAlert as _withAlert } from "@bluemind/alert.store/src/withAlert";

export function withAlert(action, actionName = "", options = {}) {
    const name = "mail." + (actionName || action.name);
    return _withAlert(action, name, options);
}
