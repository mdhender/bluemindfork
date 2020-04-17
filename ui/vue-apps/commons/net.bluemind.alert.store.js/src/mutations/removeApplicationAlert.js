export function removeApplicationAlert(state, alertUid) {
    state.applicationAlerts.splice(
        state.applicationAlerts.findIndex(alert => alert.uid === alertUid),
        1
    );
}
