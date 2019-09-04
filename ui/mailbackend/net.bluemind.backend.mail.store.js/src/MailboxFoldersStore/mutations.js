import Vue from "vue";
import Folder from "./Folder";

export function all(state, folders) {
    state.folders = folders.map(f => new Folder(f));
}

export function settings(state, settings) {
    state.settings = settings;
}

export function folderSettings(state, value) {
    Vue.set(state.settings, value.uid, value.settings);
}

export function setCurrent(state, uid) {
    const settings = JSON.parse(window.localStorage.getItem("backend.mail.folders")) || {};
    settings.current = uid;
    window.localStorage.setItem("backend.mail.folders", JSON.stringify(settings));
    Vue.set(state.settings, 'current', uid);
}

export function create(state, mailboxFolder) {
    state.folders.push(new Folder(mailboxFolder));
}