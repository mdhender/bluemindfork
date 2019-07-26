export function addError(state, error) {
    state.errors.splice(0, state.errors.length);
    state.errors.push(error);

}

export function removeErrorAlert(state, errorUid) {
    state.errors.splice(state.errors.findIndex(error => error.uid === errorUid), 1);
}

export function removeAllErrorAlerts(state) {
    state.errors.splice(0, state.errors.length);
}

export function addSuccess(state, success) {
    state.successes.push(success);
}
