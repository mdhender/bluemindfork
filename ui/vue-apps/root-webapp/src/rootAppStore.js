import { inject } from "@bluemind/inject";

const state = {
    appState: "loading",
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
    }
};

const actions = {
    async FETCH_MY_MAILBOX_QUOTA({ commit }) {
        const userId = inject("UserSession").userId;
        const mailboxQuota = await inject("MailboxesPersistence").getMailboxQuota(userId);
        commit("SET_QUOTA", { used: mailboxQuota.used, total: mailboxQuota.quota });
    },
    async FETCH_IDENTITIES({ commit }) {
        const identities = await inject("UserMailIdentitiesPersistence").getIdentities();
        commit("SET_IDENTITIES", identities);
    }
};

const getters = {
    DEFAULT_IDENTITY: state => state.identities.find(identity => identity.isDefault)
};

export default {
    namespaced: true,
    actions,
    getters,
    mutations,
    state
};
