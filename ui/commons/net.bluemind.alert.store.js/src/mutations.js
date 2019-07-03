export function addError(state, error) {
    state.errors.push(error);
}

export function addSuccess(state, success) {
    state.successes.push(success);
}