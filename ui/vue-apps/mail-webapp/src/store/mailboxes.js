import Vue from "vue";
import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import { mailboxUtils, loadingStatusUtils } from "@bluemind/mail";
import { MailboxAdaptor } from "./helpers/MailboxAdaptor";
import {
    GROUP_MAILBOXES,
    GROUP_MAILBOX_KEYS,
    MAILBOX_BY_NAME,
    MAILBOXES_ARE_LOADED,
    MAILSHARES,
    MAILSHARE_KEYS,
    MY_MAILBOX,
    MY_MAILBOX_KEY,
    MAILBOXES,
    USER_MAILBOXES
} from "~/getters";
import { ADD_MAILBOXES, ADD_FOLDER } from "~/mutations";
import { FETCH_MAILBOXES } from "~/actions";
import { DEFAULT_FOLDERS } from "./folders/helpers/DefaultFolders";

const { LoadingStatus } = loadingStatusUtils;
const { MailboxType } = mailboxUtils;

export default {
    state: {
        keys: []
    },
    getters: {
        [GROUP_MAILBOXES]: (state, getters) =>
            getters[MAILBOXES].filter(({ type }) => type === MailboxType.GROUP).sort((a, b) =>
                a.dn.localeCompare(b.dn)
            ),
        [GROUP_MAILBOX_KEYS]: (state, getters) => getters[GROUP_MAILBOXES].map(({ key }) => key),
        [MY_MAILBOX_KEY]: (state, getters) => getters[MY_MAILBOX].key,
        [MY_MAILBOX]: (state, getters) =>
            getters[MAILBOXES].find(mailbox => mailbox.owner === inject("UserSession").userId),
        [MAILSHARE_KEYS]: (state, getters) => getters[MAILSHARES].map(({ key }) => key),
        [MAILSHARES]: (state, getters) =>
            getters[MAILBOXES].filter(({ type }) => type === MailboxType.MAILSHARE).sort((a, b) =>
                a.dn.localeCompare(b.dn)
            ),
        [MAILBOX_BY_NAME]: (state, getters) => name =>
            getters[MAILBOXES].find(mailbox => mailbox.name.toLowerCase() === name.toLowerCase()),
        [MAILBOXES_ARE_LOADED]: (state, getters) =>
            getters[MAILBOXES].length >= 1 && getters[MAILBOXES][getters[MAILBOXES].length - 1].remoteRef.id,
        [MAILBOXES]: state => state.keys.map(key => state[key]),
        [USER_MAILBOXES]: (state, getters) =>
            getters[MAILBOXES].filter(mailbox => mailbox.type === MailboxType.USER).sort((a, b) =>
                a.owner === inject("UserSession").userId
                    ? -1
                    : b.owner === inject("UserSession").userId
                    ? 1
                    : a.name.localeCompare(b.name)
            )
    },

    mutations: {
        [ADD_MAILBOXES]: (state, mailboxes) =>
            mailboxes.forEach(mailbox => {
                if (!state.keys.includes(mailbox.key)) {
                    state.keys.push(mailbox.key);
                }
                Vue.set(state, mailbox.key, mailbox);
            }),
        //Hook
        [ADD_FOLDER]: (state, folder) => {
            state[folder.mailboxRef.key].loading = LoadingStatus.LOADED;
        }
    },
    actions: {
        [FETCH_MAILBOXES]: async ({ state, commit }) => {
            const subscriptions = await inject("OwnerSubscriptionsPersistence").list();
            const mailboxUids = subscriptions
                .filter(subscription => subscription.value.containerType === "mailboxacl")
                .map(subscription => subscription.value.containerUid);
            const remoteMailboxes = (
                await inject("ContainersPersistence").getContainers(mailboxUids)
            ).filter(({ verbs }) => verbs.some(verb => [Verb.Read, Verb.Write, Verb.All].includes(verb)));
            const dirEntries = await inject("DirectoryPersistence").getMultiple(
                remoteMailboxes.map(({ owner }) => owner)
            );
            const mailboxes = remoteMailboxes
                .map(remote => {
                    const dirEntry = dirEntries.find(dirEntry => dirEntry.uid === remote.owner);
                    return MailboxAdaptor.fromMailboxContainer(remote, dirEntry.value);
                })
                .filter(Boolean) // unsupported mailbox (like groups one) are undefined here (due to mailbox model #create fn)
                .map(mailbox => {
                    mailbox.loading = state[mailbox.key]?.loading || mailbox.loading;
                    return mailbox;
                });
            commit(ADD_MAILBOXES, mailboxes);
        }
    },
    modules: {
        folders: {
            state: {
                defaults: {}
            },
            mutations: {
                ADD_MAILBOXES(state, mailboxes) {
                    mailboxes.forEach(mailbox => {
                        if (!state.defaults[mailbox.key]) {
                            Vue.set(state.defaults, mailbox.key, {});
                        }
                    });
                },
                ADD_FOLDER(state, folder) {
                    if (folder.default && DEFAULT_FOLDERS.includes(folder.imapName)) {
                        Vue.set(state.defaults[folder.mailboxRef.key], folder.imapName, folder.key);
                    }
                }
            }
        }
    }
};
