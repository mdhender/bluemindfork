import Vue from "vue";

export function setSearchError(state, hasError) {
    Vue.set(state.search, "error", hasError);
}