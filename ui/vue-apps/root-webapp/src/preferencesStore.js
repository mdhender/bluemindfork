import preferenceContainers from "./preferences/store/preferenceContainers";
import { html2text, text2html } from "@bluemind/html-utils";
import { inject } from "@bluemind/inject";
import cloneDeep from "lodash.clonedeep";

const state = {
    offset: 0,
    showPreferences: false,
    search: "",
    selectedSectionId: "",
    sectionById: {},
    status: "idle",
    userPasswordLastChange: null,
    subscriptions: [],
    mailboxFilter: { remote: {}, local: {}, loaded: false }
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
    async FETCH_MAILBOX_FILTER({ commit }, userLang) {
        const userId = inject("UserSession").userId;
        const mailboxFilter = await inject("MailboxesPersistence").getMailboxFilter(userId);
        if (!mailboxFilter.vacation.textHtml && mailboxFilter.vacation.text) {
            mailboxFilter.vacation.textHtml = text2html(mailboxFilter.vacation.text, userLang);
        }
        commit("SET_MAILBOX_FILTER", mailboxFilter);
    },
    async SAVE_MAILBOX_FILTER({ commit, state }, { vacation, forwarding, rules }) {
        if (vacation) {
            if (vacation.textHtml) {
                vacation = { ...vacation, text: html2text(vacation.textHtml) };
            }
            commit("SET_VACATION", vacation);
        }
        if (forwarding) {
            commit("SET_FORWARDING", forwarding);
        }
        if (rules) {
            commit("SET_RULES", rules);
        }
        const userId = inject("UserSession").userId;
        await inject("MailboxesPersistence").setMailboxFilter(userId, state.mailboxFilter.local);
        commit("SET_MAILBOX_FILTER", state.mailboxFilter.local);
    },
    async SAVE({ commit, dispatch }) {
        commit("SET_STATUS", "saving");
        try {
            await dispatch("fields/SAVE");
            commit("SET_STATUS", "saved");
        } catch {
            commit("SET_STATUS", "error");
        }
    },
    async AUTOSAVE({ commit, dispatch }) {
        commit("SET_STATUS", "saving");
        try {
            await dispatch("fields/AUTOSAVE");
            commit("SET_STATUS", "saved");
        } catch {
            commit("SET_STATUS", "error");
        }
    },
    CANCEL({ dispatch }) {
        return dispatch("fields/CANCEL");
    }
};

const mutations = {
    SET_STATUS: (state, status) => (state.status = status),
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

    // mailboxFilter
    SET_MAILBOX_FILTER: (state, mailboxFilter) => {
        state.mailboxFilter.remote = cloneDeep(mailboxFilter);
        state.mailboxFilter.local = cloneDeep(mailboxFilter);
        state.mailboxFilter.loaded = true;
    },
    ROLLBACK_MAILBOX_FILTER: state => {
        state.mailboxFilter.local = cloneDeep(state.mailboxFilter.remote);
    },
    SET_VACATION: (state, vacation) => {
        state.mailboxFilter.local.vacation = cloneDeep(vacation);
    },
    SET_FORWARDING: (state, forwarding) => {
        state.mailboxFilter.local.forwarding = cloneDeep(forwarding);
    },
    SET_RULES: (state, rules) => {
        state.mailboxFilter.local.rules = cloneDeep(rules);
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
    STATUS: ({ status }, getters) => {
        if (getters["fields/HAS_CHANGED"]) return "idle";
        if (status === "saved" && getters["fields/HAS_ERROR"]) return "error";
        return status;
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
        preferenceContainers,
        fields: {
            namespaced: true,
            state: {},
            actions: {
                CANCEL({ state, commit }) {
                    Object.keys(state).forEach(id => {
                        if (state[id].current && !state[id].current.options.saved) {
                            commit("PUSH_STATE", { id, ...state[id].saved });
                        }
                    });
                }
            },
            mutations: {
                PUSH_STATE(state, { id, value, options = {} }) {
                    if (options.saved) {
                        state[id].saved = { value, options };
                    }
                    state[id].current = { value, options };
                },
                NEED_RELOAD(state, { id }) {
                    if (state[id].saved) {
                        state[id].saved.options.reload = true;
                    } else {
                        state[id].saved = { value: null, options: { reload: true } };
                    }
                },
                NEED_LOGOUT(state, { id }) {
                    if (state[id].saved) {
                        state[id].saved.options.logout = true;
                    } else {
                        state[id].saved = { value: null, options: { logout: true } };
                    }
                }
            },
            getters: {
                ERRORS: state => Object.keys(state).filter(id => state[id]?.current?.options.error),
                HAS_CHANGED: state =>
                    Object.values(state).some(
                        ({ current }) => current?.options && !current.options.saved && !current.options.autosave
                    ),
                HAS_ERROR: state => Object.values(state).some(({ current }) => current?.options.error),
                HAS_NOT_VALID: state => Object.values(state).some(({ current }) => current?.options.notValid),
                IS_RELOAD_NEEDED: state => Object.values(state).some(({ saved }) => saved?.options.reload),
                IS_LOGOUT_NEEDED: state => Object.values(state).some(({ saved }) => saved?.options.logout),
                NOT_VALID_PREFERENCES: state => Object.keys(state).filter(id => state[id]?.current?.options.notValid)
            }
        }
    }
};
