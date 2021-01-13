import { inject } from "@bluemind/inject";

const state = {
    appState: "loading",
    quota: {
        used: null,
        total: null
    }
};

const mutations = {
    SET_APP_STATE: (state, appState) => {
        state.appState = appState;
    },
    SET_QUOTA: (state, { used, total }) => {
        state.quota.used = used;
        state.quota.total = total;
    }
};

const actions = {
    async FETCH_MY_MAILBOX_QUOTA({ commit }) {
        const userId = inject("UserSession").userId;
        const mailboxQuota = await inject("MailboxesPersistence").getMailboxQuota(userId);
        commit("SET_QUOTA", { used: mailboxQuota.used, total: mailboxQuota.quota });
    }
};

export default {
    namespaced: true,
    actions,
    mutations,
    state
};
