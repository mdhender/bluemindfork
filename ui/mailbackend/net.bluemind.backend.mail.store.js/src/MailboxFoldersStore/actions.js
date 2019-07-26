import ServiceLocator from "@bluemind/inject";

export function bootstrap({ dispatch, state, commit }) {
    return dispatch("settings")
        .then(() => dispatch("all"))
        .then(() => {
            if (state.settings.current == null) {
                commit("setCurrent", state.folders.find(item => item.displayName === "INBOX").uid);
            }
        });
}

export function all({ commit }) {
    const service = ServiceLocator.getProvider("MailboxFoldersPersistance").get();
    return service.all().then(items => {
        commit("all", items);
    });
}

export function settings({ commit }) {
    const settings = window.localStorage.getItem("backend.mail.folders") || "{}";
    commit("settings", JSON.parse(settings));
}

export function expand({ commit }, uid) {
    const settings = JSON.parse(window.localStorage.getItem("backend.mail.folders")) || {};
    const folder = settings[uid] || (settings[uid] = {});
    folder.expanded = true;
    window.localStorage.setItem("backend.mail.folders", JSON.stringify(settings));
    commit("folderSettings", { uid: uid, settings: folder });
}

export function collapse({ commit }, uid) {
    const settings = JSON.parse(window.localStorage.getItem("backend.mail.folders")) || {};
    const folder = settings[uid] || (settings[uid] = {});
    folder.expanded = false;
    window.localStorage.setItem("backend.mail.folders", JSON.stringify(settings));
    commit("folderSettings", { uid: uid, settings: folder });
}