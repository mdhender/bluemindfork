import AlertFactory from "../AlertFactory";

export function addApplicationAlert(state, { code, props, uid }) {
    state.applicationAlerts.push(AlertFactory.create(code, props, uid));
}
