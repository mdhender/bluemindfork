export function remove(state, alertUid) {
    state.alerts.splice(state.alerts.findIndex(alert => alert.uid === alertUid), 1);
}