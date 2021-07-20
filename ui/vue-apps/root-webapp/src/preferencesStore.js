import { Verb } from "@bluemind/core.container.api";
import { html2text, text2html } from "@bluemind/html-utils";
import { inject } from "@bluemind/inject";

const state = {
    offset: 0,
    showPreferences: false,
    selectedSectionCode: "",
    sectionByCode: {},

    userPasswordLastChange: null,
    subscriptions: [],
    myCalendars: [],
    otherManagedCalendars: [], // other = dont include owned calendars

    mailboxFilter: { remote: {}, local: {}, loaded: false }
};

const actions = {
    async FETCH_USER_PASSWORD_LAST_CHANGE({ commit }) {
        const userId = inject("UserSession").userId;
        const user = await inject("UserPersistence").getComplete(userId);
        commit("SET_USER_PASSWORD_LAST_CHANGE", user);
    },
    async FETCH_CALENDARS({ commit }) {
        const managedCalendars = await inject("ContainersPersistence").all({
            type: "calendar",
            verb: [Verb.All, Verb.Manage]
        });
        const userId = inject("UserSession").userId;
        const otherManagedCalendars = managedCalendars.filter(container => container.owner !== userId);
        const myCalendars = managedCalendars
            .filter(container => container.owner === userId)
            .sort(container => (container.defaultContainer ? 0 : 1));
        commit("SET_CALENDARS", { myCalendars, otherManagedCalendars });
    },
    async FETCH_SUBSCRIPTIONS({ commit }) {
        const subscriptions = await inject("OwnerSubscriptionsPersistence").list();
        commit("SET_SUBSCRIPTIONS", subscriptions);
    },
    async ADD_SUBSCRIPTIONS({ commit }, newSubscriptions) {
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
        if (!mailboxFilter.vacation.textHtml) {
            mailboxFilter.vacation.textHtml = text2html(mailboxFilter.vacation.text, userLang);
        }
        commit("SET_MAILBOX_FILTER", mailboxFilter);
    },
    async SAVE_MAILBOX_FILTER({ commit, state }) {
        if (state.mailboxFilter.local.vacation.textHtml) {
            state.mailboxFilter.local.vacation.text = html2text(state.mailboxFilter.local.vacation.textHtml);
        }
        commit("SET_MAILBOX_FILTER", state.mailboxFilter.local);
        const userId = inject("UserSession").userId;
        return inject("MailboxesPersistence").setMailboxFilter(userId, state.mailboxFilter.local);
    }
};

const mutations = {
    SET_OFFSET: (state, offset) => (state.offset = offset),
    TOGGLE_PREFERENCES: state => (state.showPreferences = !state.showPreferences),
    SET_SECTIONS: (state, sections = []) => {
        const sectionByCode = {};
        sections.forEach(s => (sectionByCode[s.code] = s));
        state.sectionByCode = sectionByCode;
    },
    SET_SELECTED_SECTION: (state, selectedPrefSection) => {
        state.selectedSectionCode = selectedPrefSection;
    },
    SET_USER_PASSWORD_LAST_CHANGE: (state, user) => {
        state.userPasswordLastChange = user.value.passwordLastChange || user.created;
    },
    SET_CALENDARS: (state, { myCalendars, otherManagedCalendars }) => {
        state.myCalendars = myCalendars;
        state.otherManagedCalendars = otherManagedCalendars;
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
    SET_SUBSCRIPTIONS: (state, subscriptions) => {
        state.subscriptions = subscriptions;
    },
    ADD_SUBSCRIPTIONS: (state, subscriptions) => {
        state.subscriptions.push(...subscriptions);
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
    }
};

const getters = {
    SECTIONS: state => Object.values(state.sectionByCode),
    MAILBOX_FILTER_CHANGED: state =>
        JSON.stringify(state.mailboxFilter.local) !== JSON.stringify(state.mailboxFilter.remote)
};

export default {
    namespaced: true,
    actions,
    mutations,
    state,
    getters
};
