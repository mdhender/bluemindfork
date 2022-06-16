import { containsHtml, text2html } from "@bluemind/html-utils";
import { inject } from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";
import { adapt, toRemote } from "@bluemind/webappdata";
import Vue from "vue";

const state = {
    appState: "loading",
    appData: {},
    isOnline: true,
    quota: {
        used: null,
        total: null
    },
    identities: [] // matches IdentityDescription.java domain class
};

const mutations = {
    SET_APP_STATE: (state, appState) => {
        state.appState = appState;
    },
    SET_ALL_APP_DATA: (state, allAppData) => {
        allAppData.forEach(data => {
            Vue.set(state.appData, data.key, data);
        });
    },
    SET_APP_DATA: (state, appData) => {
        Vue.set(state.appData, appData.key, appData);
    },
    REMOVE_APP_DATA: (state, key) => {
        Vue.delete(state, key);
    },
    SET_QUOTA: (state, { used, total }) => {
        state.quota.used = used;
        state.quota.total = total;
    },
    SET_IDENTITIES: (state, identities) => {
        state.identities = identities;
    },
    REMOVE_IDENTITY: (state, id) => {
        const index = state.identities.findIndex(identity => identity.id === id);
        state.identities.splice(index, 1);
    },
    ADD_IDENTITY: (state, identity) => {
        state.identities.push(identity);
    },
    UPDATE_IDENTITY: (state, identity) => {
        const index = state.identities.findIndex(i => i.id === identity.id);
        state.identities.splice(index, 1, identity);
    },
    $_VueBus_ONLINE: (state, online) => {
        state.isOnline = online;
    }
};

const actions = {
    async FETCH_ALL_APP_DATA({ commit }) {
        const service = await inject("WebAppDataPersistence");
        const appData = await service.multipleGet(await service.allUids());
        if (appData) {
            const adapted = appData.map(adapt);
            commit("SET_ALL_APP_DATA", adapted);
        }
    },
    async SET_APP_DATA({ commit, state }, { key, value }) {
        const stored = state.appData[key];
        const uid = stored ? stored.uid : UUIDGenerator.generate();
        commit("SET_APP_DATA", { uid, key, value });
        const remote = toRemote({ key, value });
        if (stored) {
            await inject("WebAppDataPersistence").update(uid, remote);
        } else {
            await inject("WebAppDataPersistence").create(uid, remote);
        }
    },
    async FETCH_MY_MAILBOX_QUOTA({ commit }) {
        const userId = inject("UserSession").userId;
        const mailboxQuota = await inject("MailboxesPersistence").getMailboxQuota(userId);
        commit("SET_QUOTA", { used: mailboxQuota.used, total: mailboxQuota.quota });
    },
    async FETCH_IDENTITIES({ commit }, userLang) {
        const identities = await inject("UserMailIdentitiesPersistence").getIdentities();
        const adapted = convertSignaturesToHtml(identities, userLang);
        commit("SET_IDENTITIES", adapted);
    }
};

const getters = {
    DEFAULT_IDENTITY: state => {
        let identity = state.identities.find(identity => identity.isDefault);
        if (!identity) {
            // fallback to current user identity
            const userSession = inject("UserSession");
            identity = state.identities.find(identity => identity.email === userSession.defaultEmail);
            if (!identity) {
                identity = { email: userSession.defaultEmail, displayname: userSession.formatedName };
            }
        }
        return identity;
    }
};

function convertSignaturesToHtml(identities, userLang) {
    return identities.map(identity => {
        if (!containsHtml(identity.signature)) {
            identity.signature = text2html(identity.signature, userLang);
        }
        return identity;
    });
}

export default {
    namespaced: true,
    actions,
    getters,
    mutations,
    state
};
