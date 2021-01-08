import { inject } from "@bluemind/inject";

const state = {
    appState: "loading",
    showSettings: false,
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
    },
    TOGGLE_SETTINGS: state => {
        state.showSettings = !state.showSettings;
    }
};

const actions = {
    async FETCH_MY_MAILBOX_QUOTA({ commit }) {
        const userId = inject("UserSession").userId;
        const mailboxQuota = await inject("MailboxesPersistence").getMailboxQuota(userId);
        console.log("quota retrieved is : ", mailboxQuota);
        commit("SET_QUOTA", { used: mailboxQuota.used, total: mailboxQuota.quota });
    }
};

export default {
    namespaced: true,
    actions,
    mutations,
    state
};
