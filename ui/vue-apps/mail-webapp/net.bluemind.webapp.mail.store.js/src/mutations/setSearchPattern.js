import Vue from "vue";

export function setSearchPattern(state, pattern) {
    Vue.set(state.search, "pattern", pattern);
}
