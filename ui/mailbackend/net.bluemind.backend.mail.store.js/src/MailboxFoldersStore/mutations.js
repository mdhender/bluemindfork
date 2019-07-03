import Vue from "vue";

export function all(state, folders) {
    state.folders = folders;
}

export function settings(state, settings) {
    state.settings = settings;
}

export function folderSettings(state, value) {
    Vue.set(state.settings, value.uid, value.settings);
}

export function setCurrent(state, uid) {
    Vue.set(state.settings, 'current', uid);
}
