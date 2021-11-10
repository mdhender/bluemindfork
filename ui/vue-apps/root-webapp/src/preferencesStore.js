import { ContainerType, isManaged } from "./components/preferences/fields/customs/ContainersManagement/container";
import { html2text, text2html } from "@bluemind/html-utils";
import { inject } from "@bluemind/inject";

const state = {
    offset: 0,
    showPreferences: false,
    selectedSectionId: "",
    sectionById: {},
    status: "idle",
    userPasswordLastChange: null,
    subscriptions: [],
    myCalendars: [],
    otherCalendars: [], // includes calendars I subscribe to and those I manage (excluding mines)
    myMailboxContainer: null,
    otherMailboxesContainers: [], // includes mailboxes I subscribe to and those I manage (excluding mine)
    myAddressbooks: [],
    otherAddressbooks: [], // includes addressbooks I subscribe to and those I manage (excluding mine)
    mailboxFilter: { remote: {}, local: {}, loaded: false }
};

const actions = {
    async FETCH_USER_PASSWORD_LAST_CHANGE({ commit }) {
        const userId = inject("UserSession").userId;
        const user = await inject("UserPersistence").getComplete(userId);
        commit("SET_USER_PASSWORD_LAST_CHANGE", user);
    },
    async FETCH_CONTAINERS({ commit, state }) {
        const allReadableContainers = await inject("ContainersPersistence").all({});
        commit("SET_CALENDARS", getContainers(ContainerType.CALENDAR, allReadableContainers, state.subscriptions));
        commit(
            "SET_MAILBOX_CONTAINERS",
            getContainers(ContainerType.MAILBOX, allReadableContainers, state.subscriptions)
        );
        commit(
            "SET_ADDRESSBOOKS",
            getContainers(ContainerType.ADDRESSBOOK, allReadableContainers, state.subscriptions)
        );
    },
    async FETCH_SUBSCRIPTIONS({ commit }) {
        const subscriptions = await inject("OwnerSubscriptionsPersistence").list();
        commit("SET_SUBSCRIPTIONS", subscriptions);
    },
    async SET_SUBSCRIPTIONS({ commit }, newSubscriptions) {
        const userId = inject("UserSession").userId;
        await inject("UserSubscriptionPersistence").subscribe(
            userId,
            newSubscriptions.map(sub => ({
                containerUid: sub.value.containerUid,
                offlineSync: sub.value.offlineSync
            }))
        );
        commit("ADD_SUBSCRIPTIONS", newSubscriptions);
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
    async SAVE_MAILBOX_FILTER({ commit, state }, { vacation, forwarding }) {
        if (vacation) {
            if (vacation.textHtml) {
                vacation = { ...vacation, text: html2text(vacation.textHtml) };
            }
            commit("SET_VACATION", vacation);
        }
        if (forwarding) {
            commit("SET_FORWARDING", forwarding);
        }
        const userId = inject("UserSession").userId;
        await inject("MailboxesPersistence").setMailboxFilter(userId, state.mailboxFilter.local);
        commit("SET_MAILBOX_FILTER", state.mailboxFilter.local);
    },
    async SAVE({ commit, dispatch }) {
        commit("SET_STATUS", "saving");
        await dispatch("fields/SAVE");
        commit("SET_STATUS", "saved");
    },
    async AUTOSAVE({ commit, dispatch }) {
        commit("SET_STATUS", "saving");
        await dispatch("fields/AUTOSAVE");
        commit("SET_STATUS", "saved");
    },
    CANCEL({ dispatch }) {
        return dispatch("fields/CANCEL");
    }
};

const mutations = {
    SET_STATUS: (state, status) => (state.status = status),
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
        state.mailboxFilter.remote = JSON.parse(JSON.stringify(mailboxFilter));
        state.mailboxFilter.local = JSON.parse(JSON.stringify(mailboxFilter));
        state.mailboxFilter.loaded = true;
    },
    ROLLBACK_MAILBOX_FILTER: state => {
        state.mailboxFilter.local = JSON.parse(JSON.stringify(state.mailboxFilter.remote));
    },
    SET_VACATION: (state, vacation) => {
        state.mailboxFilter.local.vacation = JSON.parse(JSON.stringify(vacation));
    },
    SET_FORWARDING: (state, forwarding) => {
        state.mailboxFilter.local.forwarding = JSON.parse(JSON.stringify(forwarding));
    },

    // calendars
    SET_CALENDARS: (state, { myContainers, otherContainers }) => {
        state.myCalendars = myContainers;
        state.otherCalendars = otherContainers;
    },
    ADD_PERSONAL_CALENDAR: (state, myCalendar) => {
        state.myCalendars.push(myCalendar);
    },
    REMOVE_PERSONAL_CALENDAR: (state, calendarUid) => {
        const index = state.myCalendars.findIndex(myCal => myCal.uid === calendarUid);
        if (index !== -1) {
            state.myCalendars.splice(index, 1);
        }
    },
    UPDATE_PERSONAL_CALENDAR: (state, calendar) => {
        const index = state.myCalendars.findIndex(myCal => myCal.uid === calendar.uid);
        if (index !== -1) {
            state.myCalendars.splice(index, 1, calendar);
        }
    },
    ADD_OTHER_CALENDARS: (state, calendars) => {
        state.otherCalendars.push(...calendars);
    },
    REMOVE_OTHER_CALENDAR: (state, uid) => {
        const index = state.otherCalendars.findIndex(otherCal => otherCal.uid === uid);
        if (index !== -1) {
            state.otherCalendars.splice(index, 1);
        }
    },
    UPDATE_OTHER_CALENDAR: (state, calendar) => {
        const index = state.otherCalendars.findIndex(otherCal => otherCal.uid === calendar.uid);
        if (index !== -1) {
            state.otherCalendars.splice(index, 1, calendar);
        }
    },

    // mailboxes
    SET_MAILBOX_CONTAINERS: (state, { myContainers, otherContainers }) => {
        state.myMailboxContainer = myContainers[0];
        state.otherMailboxesContainers = otherContainers;
    },
    ADD_OTHER_MAILBOXES: (state, mailboxes) => {
        state.otherMailboxesContainers.push(...mailboxes);
    },
    REMOVE_OTHER_MAILBOX_CONTAINER: (state, containerUid) => {
        const index = state.otherMailboxesContainers.findIndex(container => container.uid === containerUid);
        if (index !== -1) {
            state.otherMailboxesContainers.splice(index, 1);
        }
    },
    UPDATE_OTHER_MAILBOX_CONTAINER: (state, updatedContainer) => {
        const index = state.otherMailboxesContainers.findIndex(
            mailboxContainer => mailboxContainer.uid === updatedContainer.uid
        );
        if (index !== -1) {
            state.otherMailboxesContainers.splice(index, 1, updatedContainer);
        }
    },

    // addressbooks
    SET_ADDRESSBOOKS: (state, { myContainers, otherContainers }) => {
        state.myAddressbooks = myContainers;
        state.otherAddressbooks = otherContainers;
    },
    ADD_PERSONAL_ADDRESSBOOK: (state, myAddressbook) => {
        state.myAddressbooks.push(myAddressbook);
    },
    REMOVE_PERSONAL_ADDRESSBOOK: (state, addressbookUid) => {
        const index = state.myAddressbooks.findIndex(myAddressbook => myAddressbook.uid === addressbookUid);
        if (index !== -1) {
            state.myAddressbooks.splice(index, 1);
        }
    },
    UPDATE_PERSONAL_ADDRESSBOOK: (state, addressbook) => {
        const index = state.myAddressbooks.findIndex(myAddressbook => myAddressbook.uid === addressbook.uid);
        if (index !== -1) {
            state.myAddressbooks.splice(index, 1, addressbook);
        }
    },
    ADD_OTHER_ADDRESSBOOK: (state, calendars) => {
        state.otherAddressbooks.push(...calendars);
    },
    REMOVE_OTHER_ADDRESSBOOK: (state, uid) => {
        const index = state.otherAddressbooks.findIndex(otherAddressbook => otherAddressbook.uid === uid);
        if (index !== -1) {
            state.otherAddressbooks.splice(index, 1);
        }
    },
    UPDATE_OTHER_ADDRESSBOOK: (state, addressbook) => {
        const index = state.otherAddressbooks.findIndex(otherAddressbook => otherAddressbook.uid === addressbook.uid);
        if (index !== -1) {
            state.otherAddressbooks.splice(index, 1, addressbook);
        }
    }
};

const getters = {
    SECTIONS: state => Object.values(state.sectionById).filter(section => section.visible),
    MAILBOX_FILTER_CHANGED: state =>
        JSON.stringify(state.mailboxFilter.local) !== JSON.stringify(state.mailboxFilter.remote),
    STATUS: ({ status }, getters) => {
        if (getters["fields/HAS_CHANGED"]) return "idle";
        if (status === "saved" && getters["fields/HAS_ERROR"]) return "error";
        return status;
    }
};

export default {
    namespaced: true,
    actions,
    mutations,
    state,
    getters,
    modules: {
        fields: {
            namespaced: true,
            state: {},
            actions: {
                // async SAVE() {
                //     //ALERT !
                // },
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
                HAS_CHANGED: state =>
                    Object.values(state).some(
                        ({ current }) => current?.options && !current.options.saved && !current.options.autosave
                    ),
                HAS_ERROR: state => Object.values(state).some(({ current }) => current?.options.error),
                IS_RELOAD_NEEDED: state => Object.values(state).some(({ saved }) => saved?.options.reload),
                IS_LOGOUT_NEEDED: state => Object.values(state).some(({ saved }) => saved?.options.logout)
            }
        }
    }
};

function getContainers(expectedContainerType, allReadableContainers, subscriptions) {
    const userId = inject("UserSession").userId;
    const filteredByContainerType = allReadableContainers.filter(container => container.type === expectedContainerType);
    const myContainers = filteredByContainerType
        .filter(container => container.owner === userId)
        .sort(container => (container.defaultContainer ? 0 : 1));
    const otherOwnerContainers = filteredByContainerType.filter(container => container.owner !== userId);
    const otherManagedContainers = otherOwnerContainers.filter(isManaged);
    const subscribedContainers = otherOwnerContainers.filter(
        container =>
            subscriptions.findIndex(sub => sub.value.containerUid === container.uid) !== -1 &&
            otherManagedContainers.findIndex(managed => managed.uid === container.uid) === -1
    );
    return {
        myContainers,
        otherContainers: otherManagedContainers.concat(subscribedContainers)
    };
}
