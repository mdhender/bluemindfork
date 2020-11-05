export function removeApplicationAlert(state, alertUid) {
    state.splice(
        state.findIndex(alert => alert.uid === alertUid),
        1
    );
}
