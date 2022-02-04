import { withAlert as _withAlert } from "@bluemind/alert.store";

export default function (action, alertName) {
    const name = "preferences." + alertName;
    return _withAlert(action, name, { area: "pref-right-panel" });
}
