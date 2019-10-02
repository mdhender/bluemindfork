export function addAlert(state, alert) {
    state.alerts.push(alert);
}

export function removeAlert(state, alertUid) {
    state.alerts.splice(state.alerts.findIndex(alert => alert.uid === alertUid), 1);
}

export function removeAllAlerts(state) {
    state.alerts = [];
}