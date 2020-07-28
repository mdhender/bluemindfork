import { STATUS } from "../modules/search";

export function isSearchMode(state) {
    return state.search.status !== STATUS.IDLE;
}
export function isFolderMode(state) {
    return state.search.status === STATUS.IDLE;
}
