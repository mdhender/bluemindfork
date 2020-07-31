import Vue from "vue";
import { inject } from "@bluemind/inject";
import { MailboxAdaptor } from "./helpers/MailboxAdaptor";

export const FETCH_MAILBOXES = "FETCH_MAILBOXES";
export const ADD_MAILBOXES = "ADD_MAILBOXES";

export const state = {
    mailboxes: {}
};

export const getters = {
    // FIXME (with mailbox.owner === state.session.userId) once we got a store session
    MY_MAILBOX_KEY: state => Object.values(state.mailboxes).find(mailbox => mailbox.type === "users").key,

    MAILSHARE_KEYS: state =>
        Object.values(state.mailboxes)
            .filter(mailbox => mailbox.type === "mailshares")
            .map(mailshare => mailshare.key)
};

export const mutations = {
    [ADD_MAILBOXES]: (state, mailboxes) => mailboxes.forEach(mailbox => Vue.set(state.mailboxes, mailbox.key, mailbox))
};

export const actions = {
    async [FETCH_MAILBOXES]({ commit }) {
        const subscriptions = await inject("SubscriptionPersistence").list();
        const items = await inject("ContainersPersistence").getContainers(
            subscriptions
                .filter(subscription => subscription.value.containerType === "mailboxacl")
                .map(subscription => subscription.value.containerUid)
        );
        commit(ADD_MAILBOXES, items.map(MailboxAdaptor.fromMailboxContainer).filter(Boolean));
    }
};
