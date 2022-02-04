import containers from "./containers";
import fields from "./fields";
import mailboxFilter from "./mailboxFilter";
import withAlert from "./helpers/withAlert";
import { inject } from "@bluemind/inject";

const state = {
    offset: 0,
    showPreferences: false,
    search: "",
    selectedSectionId: "",
    sectionById: {},
    userPasswordLastChange: null,
    subscriptions: [],
    externalAccounts: []
};

const saveAction = async function ({ dispatch }) {
    await dispatch("fields/SAVE");
};

const autoSaveAction = async function ({ dispatch }) {
    await dispatch("fields/AUTOSAVE");
};

const actions = {
    async FETCH_USER_PASSWORD_LAST_CHANGE({ commit }) {
        const userId = inject("UserSession").userId;
        const user = await inject("UserPersistence").getComplete(userId);
        commit("SET_USER_PASSWORD_LAST_CHANGE", user);
    },
    async FETCH_SUBSCRIPTIONS({ commit }) {
        const subscriptions = await inject("OwnerSubscriptionsPersistence").list();
        commit("SET_SUBSCRIPTIONS", subscriptions);
        return subscriptions;
    },
    async REMOVE_SUBSCRIPTIONS({ commit, state }, containerUids) {
        const userId = inject("UserSession").userId;
        await inject("UserSubscriptionPersistence").unsubscribe(userId, containerUids);
        const subscriptionsToRemove = state.subscriptions.filter(sub => containerUids.includes(sub.value.containerUid));
        commit("REMOVE_SUBSCRIPTIONS", subscriptionsToRemove);
    },
    SAVE: withAlert(saveAction, "SAVE"),
    AUTOSAVE: withAlert(autoSaveAction, "SAVE"),
    CANCEL({ dispatch }) {
        return dispatch("fields/CANCEL");
    }
};

const mutations = {
    SET_SEARCH: (state, search) => {
        state.search = search;
    },
    SET_OFFSET: (state, offset) => (state.offset = offset),
    TOGGLE_PREFERENCES: state => (state.showPreferences = !state.showPreferences),
    SET_SECTIONS: (state, sections = []) => {
        const sectionById = {};
        sections.forEach(s => (sectionById[s.id] = s));
        state.sectionById = sectionById;
    },
    SET_SELECTED_SECTION: (state, selectedPrefSection) => {
        state.selectedSectionId = selectedPrefSection;
    },
    SET_USER_PASSWORD_LAST_CHANGE: (state, user) => {
        state.userPasswordLastChange = user.value.passwordLastChange || user.created;
    },

    // subscriptions
    SET_SUBSCRIPTIONS: (state, subscriptions) => {
        state.subscriptions = subscriptions;
    },
    ADD_SUBSCRIPTIONS: (state, subscriptions) => {
        subscriptions.forEach(subToAdd => {
            const index = state.subscriptions.findIndex(sub => sub.uid === subToAdd.uid);
            if (index === -1) {
                state.subscriptions.push(subToAdd);
            } else {
                state.subscriptions.splice(index, 1, subToAdd);
            }
        });
    },
    REMOVE_SUBSCRIPTIONS: (state, subscriptions) => {
        subscriptions.forEach(sub => {
            const index = state.subscriptions.findIndex(subscription => subscription.uid === sub.uid);
            if (index !== -1) {
                state.subscriptions.splice(index, 1);
            }
        });
    },
    // external accounts
    SET_EXTERNAL_ACCOUNTS: (state, externalAccounts) => {
        state.externalAccounts = externalAccounts;
    }
};

const getters = {
    GET_SECTION: (state, { SECTIONS }) => groupId =>
        SECTIONS.find(section =>
            section.categories.flatMap(category => category.groups).find(group => group.id === groupId)
        ),
    GET_GROUP: (state, { SECTIONS }) => groupId =>
        SECTIONS.flatMap(section => section.categories)
            .flatMap(category => category.groups)
            .find(group => group.id === groupId),
    SECTIONS: ({ sectionById }) => Object.values(sectionById).filter(section => section.visible),
    GROUP_BY_FIELD_ID: state => fieldId => {
        const splitId = fieldId.split(".");
        const sectionId = splitId[0];
        const categoryId = `${sectionId}.${splitId[1]}`;
        const groupId = `${categoryId}.${splitId[2]}`;
        return state.sectionById[sectionId]?.categories
            .find(c => c.id === categoryId)
            ?.groups.find(g => g.id === groupId);
    },
    SEARCH_PATTERN: ({ search }) => search.trim().toLowerCase(),
    HAS_SEARCH: (state, { SEARCH_PATTERN }) => SEARCH_PATTERN !== ""
};

export default {
    namespaced: true,
    actions,
    mutations,
    state,
    getters,
    modules: {
        containers,
        fields,
        mailboxFilter
    }
};
