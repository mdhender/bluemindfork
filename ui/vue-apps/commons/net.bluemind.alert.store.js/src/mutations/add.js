import AlertFactory from "../AlertFactory";

export function add(state, { code, props, uid }) {
    state.alerts.push(AlertFactory.create(code, props, uid));
}
