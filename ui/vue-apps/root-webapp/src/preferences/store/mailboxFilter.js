import { html2text, text2html } from "@bluemind/html-utils";
import { inject } from "@bluemind/inject";

const state = { forwarding: {}, rules: [], vacation: {} };

const actions = {
    async FETCH_MAILBOX_FILTER({ commit }, userLang) {
        const userId = inject("UserSession").userId;
        const mailboxFilter = await inject("MailboxesPersistence").getMailboxFilter(userId);
        if (!mailboxFilter.vacation.textHtml && mailboxFilter.vacation.text) {
            mailboxFilter.vacation.textHtml = text2html(mailboxFilter.vacation.text, userLang);
        }
        commit("SET_FORWARDING", mailboxFilter.forwarding);
        commit("SET_VACATION", mailboxFilter.vacation);
        commit("SET_RULES", mailboxFilter.rules);
    },
    async SAVE_VACATION({ commit, state }, vacation) {
        const oldVacation = state.vacation;
        if (vacation.textHtml) {
            vacation = { ...vacation, text: html2text(vacation.textHtml) };
        }
        commit("SET_VACATION", vacation);
        try {
            const userId = inject("UserSession").userId;
            await inject("MailboxesPersistence").setMailboxFilter(userId, state);
        } catch (e) {
            commit("SET_VACATION", oldVacation);
            throw e;
        }
    },
    async SAVE_FORWARDING({ commit, state }, forwarding) {
        const oldForwarding = state.forwarding;
        commit("SET_FORWARDING", forwarding);
        try {
            const userId = inject("UserSession").userId;
            await inject("MailboxesPersistence").setMailboxFilter(userId, state);
        } catch (e) {
            commit("SET_FORWARDING", oldForwarding);
            throw e;
        }
    },
    async SAVE_RULES({ commit, state }, rules) {
        const oldRules = state.rules;
        commit("SET_RULES", rules);
        try {
            const userId = inject("UserSession").userId;
            await inject("MailboxesPersistence").setMailboxFilter(userId, state);
        } catch (e) {
            commit("SET_RULES", oldRules);
            throw e;
        }
    }
};

const mutations = {
    SET_VACATION: (state, vacation) => {
        state.vacation = vacation;
    },
    SET_FORWARDING: (state, forwarding) => {
        state.forwarding = forwarding;
    },
    SET_RULES: (state, rules) => {
        state.rules = rules;
    }
};

export default { actions, mutations, state };
