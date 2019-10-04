import Vue from "vue";

export function setSearchLoading(state, isLoading) {
    Vue.set(state.search, "loading", isLoading);
}