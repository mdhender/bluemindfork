export function addAlert(state, alert) {
    console.log("me there");
    state.alerts.push(alert);
}

export function removeAlert(state, alertUid) {
    state.alerts.splice(state.alerts.findIndex(alert => alert.uid === alertUid), 1);
}

export function removeAllAlerts(state) {
    state.alerts = [];
}