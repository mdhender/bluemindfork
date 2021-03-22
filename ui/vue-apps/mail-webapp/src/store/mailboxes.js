import Vue from "vue";
import { inject } from "@bluemind/inject";
import { MailboxAdaptor } from "./helpers/MailboxAdaptor";
import { MailboxType } from "~model/mailbox";
import {
    MAILBOX_BY_NAME,
    MAILBOXES_ARE_LOADED,
    MAILSHARES,
    MAILSHARE_KEYS,
    MY_MAILBOX,
    MY_MAILBOX_KEY
} from "~getters";
import { ADD_MAILBOXES, SET_MAILBOX_FOLDERS } from "~mutations";
import { FETCH_MAILBOXES } from "~actions";
import { LoadingStatus } from "../model/loading-status";

export default {
    state: {},
    getters: {
        [MY_MAILBOX_KEY]: (state, getters) => getters[MY_MAILBOX].key,
        [MY_MAILBOX]: state => Object.values(state).find(mailbox => mailbox.owner === inject("UserSession").userId),
        [MAILSHARE_KEYS]: (state, getters) => getters[MAILSHARES].map(({ key }) => key),
        [MAILSHARES]: state => Object.values(state).filter(({ type }) => type === MailboxType.MAILSHARE),
        [MAILBOX_BY_NAME]: state => name =>
            Object.values(state).find(mailbox => mailbox.name.toLowerCase() === name.toLowerCase()),
        [MAILBOXES_ARE_LOADED]: state => Object.values(state).length >= 1 && Object.values(state).pop().remoteRef.id
    },

    mutations: {
        [ADD_MAILBOXES]: (state, mailboxes) => mailboxes.forEach(mailbox => Vue.set(state, mailbox.key, mailbox)),
        [SET_MAILBOX_FOLDERS]: (state, { mailbox: { key } }) => {
            if (state[key]) {
                state[key].loading = LoadingStatus.LOADED;
            }
        }
    },
    actions: {
        [FETCH_MAILBOXES]: async ({ state, commit }) => {
            const subscriptions = await inject("SubscriptionPersistence").list();
            const mailboxUids = subscriptions
                .filter(subscription => subscription.value.containerType === "mailboxacl")
                .map(subscription => subscription.value.containerUid);
            const mailboxes = (await inject("ContainersPersistence").getContainers(mailboxUids))
                .map(MailboxAdaptor.fromMailboxContainer)
                .filter(Boolean)
                .filter(({ key }) => !state[key] || state[key].loading !== LoadingStatus.LOADED);
            commit(ADD_MAILBOXES, mailboxes);
        }
    }
};
