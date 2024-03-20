import { html2text, text2html } from "@bluemind/html-utils";
import { inject } from "@bluemind/inject";
import { RuleMoveDirection, RuleMoveRelativePosition } from "@bluemind/mailbox.api";
import {
    read as readRules,
    write as writeRule
} from "../../components/preferences/fields/customs/FilterRules/filterRules";

const state = { forwarding: {}, imipRule: {}, rules: [], vacation: {} };

const getUserId = () => inject("UserSession").userId;

const add = async ({ commit, state }, { rule, relative, delta }) => {
    const userId = getUserId();
    const rawRule = writeRule(rule);
    const index =
        delta !== undefined ? state.rules.findIndex(({ id }) => id === relative.id) + delta : state.rules.length;
    const direction = delta > 0 ? RuleMoveRelativePosition.AFTER : RuleMoveRelativePosition.BEFORE;
    commit("ADD_RULE", { rule, index });
    try {
        const id =
            delta !== undefined
                ? await inject("MailboxesPersistence").addMailboxRuleRelative(userId, direction, relative.id, rawRule)
                : await inject("MailboxesPersistence").addMailboxRule(userId, rawRule);
        rule.id = id;
    } catch (e) {
        commit("REMOVE_RULE", rule);
        throw e;
    }
};

const actions = {
    async FETCH_MAILBOX_FILTER({ commit }, userLang) {
        const userId = getUserId();
        const mailboxFilter = await inject("MailboxesPersistence").getMailboxFilter(userId);
        if (!mailboxFilter.vacation.textHtml && mailboxFilter.vacation.text) {
            mailboxFilter.vacation.textHtml = text2html(mailboxFilter.vacation.text, userLang);
        }
        commit("SET_FORWARDING", mailboxFilter.forwarding);
        commit("SET_VACATION", mailboxFilter.vacation);
        commit("SET_RULES", readRules(mailboxFilter.rules));
        const imipRule = await inject("MailboxesPersistence").getMailboxDelegationRule(userId);
        commit("SET_IMIP_RULE", imipRule);
    },
    async SAVE_VACATION({ commit, state }, vacation) {
        const oldVacation = state.vacation;
        if (vacation.textHtml) {
            vacation = { ...vacation, text: html2text(vacation.textHtml) };
        }
        commit("SET_VACATION", vacation);
        try {
            const userId = getUserId();
            await inject("MailboxesPersistence").setMailboxVacation(userId, vacation);
        } catch (e) {
            commit("SET_VACATION", oldVacation);
            throw e;
        }
    },
    async SAVE_FORWARDING({ commit, state }, forwarding) {
        const oldForwarding = state.forwarding;
        commit("SET_FORWARDING", forwarding);
        try {
            const userId = getUserId();
            await inject("MailboxesPersistence").setMailboxForwarding(userId, forwarding);
        } catch (e) {
            commit("SET_FORWARDING", oldForwarding);
            throw e;
        }
    },
    async SAVE_IMIP_RULE({ commit, state }, { imipRule, calendarUid }) {
        const oldImipRule = state.imipRule;

        // overwrite with mandatory data
        Object.assign(imipRule, {
            delegatorCalendarUid: calendarUid,
            delegatorUid: getUserId()
        });

        commit("SET_IMIP_RULE", imipRule);
        try {
            await inject("MailboxesPersistence").setMailboxDelegationRule(getUserId(), imipRule);
        } catch (e) {
            commit("SET_IMIP_RULE", oldImipRule);
            throw e;
        }
    },
    async ADD_RULE(store, rule) {
        add(store, { rule });
    },
    async ADD_RULE_BEFORE(store, { rule, relative }) {
        add(store, { rule, relative, delta: 0 });
    },
    async ADD_RULE_AFTER(store, { rule, relative }) {
        add(store, { rule, relative, delta: 1 });
    },
    async REMOVE_RULE({ commit }, rule) {
        const userId = getUserId();
        const index = state.rules.findIndex(({ id }) => id === rule.id);
        commit("REMOVE_RULE", rule);
        try {
            await inject("MailboxesPersistence").deleteMailboxRule(userId, rule.id);
        } catch (e) {
            commit("ADD_RULE", rule, index);
            throw e;
        }
    },
    async UPDATE_RULE({ commit, state }, rule) {
        const userId = getUserId();
        const rawRule = writeRule(rule);
        const oldRule = state.rules.find(({ id }) => id === rule.id);
        commit("SET_RULE", rule);
        try {
            await inject("MailboxesPersistence").updateMailboxRule(userId, rule.id, rawRule);
        } catch (e) {
            commit("SET_RULE", oldRule);
            throw e;
        }
    },
    async MOVE_UP_RULE({ commit, state }, { rule, relativeTo }) {
        const userId = getUserId();
        const index = state.rules.findIndex(({ id }) => id === rule.id);
        if (index > 0) {
            const newIndex = relativeTo ? state.rules.findIndex(({ id }) => id === relativeTo.id) : index - 1;
            commit("MOVE_RULE", { index, newIndex });
            try {
                relativeTo
                    ? await inject("MailboxesPersistence").moveMailboxRuleRelative(
                          userId,
                          rule.id,
                          RuleMoveRelativePosition.BEFORE,
                          relativeTo.id
                      )
                    : await inject("MailboxesPersistence").moveMailboxRule(userId, rule.id, RuleMoveDirection.UP);
            } catch (e) {
                commit("MOVE_RULE", { index: newIndex, newIndex: index });
                throw e;
            }
        }
    },
    async MOVE_DOWN_RULE({ commit }, { rule, relativeTo }) {
        const userId = getUserId();
        const index = state.rules.findIndex(({ id }) => id === rule.id);
        if (index < state.rules.length - 1) {
            const newIndex = relativeTo ? state.rules.findIndex(({ id }) => id === relativeTo.id) : index + 1;
            commit("MOVE_RULE", { index, newIndex });
            try {
                relativeTo
                    ? await inject("MailboxesPersistence").moveMailboxRuleRelative(
                          userId,
                          rule.id,
                          RuleMoveRelativePosition.AFTER,
                          relativeTo.id
                      )
                    : await inject("MailboxesPersistence").moveMailboxRule(userId, rule.id, RuleMoveDirection.DOWN);
            } catch (e) {
                commit("MOVE_RULE", { index: newIndex, newIndex: index });
                throw e;
            }
        }
    },
    async MOVE_RULE_TOP({ commit, state }, rule) {
        const userId = getUserId();
        const index = state.rules.findIndex(({ id }) => id === rule.id);
        if (index > 0) {
            const newIndex = 0;
            commit("MOVE_RULE", { index, newIndex });
            try {
                await inject("MailboxesPersistence").moveMailboxRule(userId, rule.id, RuleMoveDirection.TOP);
            } catch (e) {
                commit("MOVE_RULE", { index: newIndex, newIndex: index });
                throw e;
            }
        }
    },
    async MOVE_RULE_BOTTOM({ commit, state }, rule) {
        const userId = getUserId();
        const index = state.rules.findIndex(({ id }) => id === rule.id);
        if (index < state.rules.length - 1) {
            const newIndex = state.rules.length - 1;
            commit("MOVE_RULE", { index, newIndex });
            try {
                await inject("MailboxesPersistence").moveMailboxRule(userId, rule.id, RuleMoveDirection.BOTTOM);
            } catch (e) {
                commit("MOVE_RULE", { index: newIndex, newIndex: index });
                throw e;
            }
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
    SET_IMIP_RULE: (state, imipRule) => {
        state.imipRule = imipRule;
    },
    SET_RULES: (state, rules) => {
        state.rules = rules;
    },
    ADD_RULE: (state, { rule, index }) => {
        state.rules = state.rules.toSpliced(index, 0, rule);
    },
    REMOVE_RULE: (state, rule) => {
        state.rules = state.rules.filter(r => r.id !== rule.id);
    },
    SET_RULE: (state, rule) => {
        const index = state.rules.findIndex(({ id }) => id === rule.id);
        state.rules = state.rules.with(index, rule);
    },
    MOVE_RULE: (state, { index, newIndex }) => {
        state.rules = state.rules.toSpliced(index, 1).toSpliced(newIndex, 0, state.rules[index]);
    }
};

export default { actions, mutations, state };
