import Vue from "vue";
import { inject } from "@bluemind/inject";
import { MailboxAdaptor } from "./helpers/MailboxAdaptor";
import { MailboxType } from "~model/mailbox";
import { MAILSHARES, MAILSHARE_KEYS, MY_MAILBOX, MY_MAILBOX_KEY } from "~getters";
import { ADD_MAILBOXES } from "~mutations";
import { FETCH_MAILBOXES } from "~actions";

export default {
    state: {},
    getters: {
        [MY_MAILBOX_KEY]: (state, getters) => getters[MY_MAILBOX].key,
        [MY_MAILBOX]: state => Object.values(state).find(mailbox => mailbox.owner === inject("UserSession").userId),
        [MAILSHARE_KEYS]: (state, getters) => getters[MAILSHARES].map(({ key }) => key),
        [MAILSHARES]: state => Object.values(state).filter(({ type }) => type === MailboxType.MAILSHARE)
    },

    mutations: {
        [ADD_MAILBOXES]: (state, mailboxes) => mailboxes.forEach(mailbox => Vue.set(state, mailbox.key, mailbox))
    },
    actions: {
        [FETCH_MAILBOXES]: async ({ commit }) => {
            const subscriptions = await inject("SubscriptionPersistence").list();
            const mailboxUids = subscriptions
                .filter(subscription => subscription.value.containerType === "mailboxacl")
                .map(subscription => subscription.value.containerUid);
            const items = await inject("ContainersPersistence").getContainers(mailboxUids);
            commit(ADD_MAILBOXES, items.map(MailboxAdaptor.fromMailboxContainer));
        }
    }
};
