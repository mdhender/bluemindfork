import AlertFactory from "../AlertFactory";

export function addApplicationAlert(state, { code, props, uid }) {
    console.log("HELLO addApplicationAlert mutation !!!!");
    state.applicationAlerts.push(AlertFactory.create(code, props, uid));
}
