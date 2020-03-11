import { STATUS } from "../constants";

export function isLoading(state) {
    return state.status === STATUS.LOADING;
}
export function isResolved(state) {
    return state.status === STATUS.RESOLVED;
}
