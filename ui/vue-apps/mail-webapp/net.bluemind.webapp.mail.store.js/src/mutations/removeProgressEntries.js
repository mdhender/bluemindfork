import Vue from "vue";

export function removeProgressEntries(state, ids) {
    ids.forEach(id => {
        Vue.delete(state.progressEntries, id);
    });
}
