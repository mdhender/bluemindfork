import AlertFactory from "../AlertFactory";

export function addApplicationAlert(state, { code, props, uid }) {
    state.push(AlertFactory.create(code, props, uid));
}
