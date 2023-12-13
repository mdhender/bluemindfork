import { inject } from "@bluemind/inject";
import { draftUtils, messageUtils, folderUtils } from "@bluemind/mail";
import { Verb } from "@bluemind/core.container.api";
import { REMOVE_MESSAGE_HEADER, SET_MESSAGE_FROM, SET_MESSAGE_HEADERS, SET_PERSONAL_SIGNATURE } from "~/mutations";
import { CURRENT_MAILBOX, MAILBOX_SENT } from "~/getters";
import { MailboxAdaptor } from "~/store/helpers/MailboxAdaptor";
import store from "@bluemind/store";

const { DEFAULT_FOLDERS } = folderUtils;
const { computeIdentityForReplyOrForward, findIdentityFromMailbox } = draftUtils;
const { MessageHeader } = messageUtils;

export async function setFrom(identity, message) {
    store.commit("mail/" + SET_MESSAGE_FROM, {
        messageKey: message.key,
        from: { address: identity.email, dn: identity.displayname }
    });
    const fullIdentity = setIdentity(identity, message);
    const rawIdentity = await inject("UserMailIdentitiesPersistence").get(fullIdentity.id);

    const mailboxes = store.state.mail.mailboxes;
    const destinationMailboxUid = await computeDestinationMailbox(rawIdentity, mailboxes);
    let mailbox = mailboxes[`user.${destinationMailboxUid}`] || mailboxes[destinationMailboxUid];
    let sentFolderUid;
    if (mailbox) {
        sentFolderUid = store.getters["mail/" + MAILBOX_SENT](mailbox)?.remoteRef.uid;
    } else {
        const mailboxContainer = await inject("ContainersPersistence").get("mailbox:acls-" + destinationMailboxUid);
        mailbox = MailboxAdaptor.fromMailboxContainer(mailboxContainer);
        const folderName = [mailbox.root, "Sent"].filter(Boolean).join("%2f");
        sentFolderUid = (await inject("MailboxFoldersPersistence", mailbox.remoteRef.uid).byName(folderName))?.uid;
    }
    if (sentFolderUid) {
        store.commit(`mail/${REMOVE_MESSAGE_HEADER}`, {
            messageKey: message.key,
            headerName: MessageHeader.X_BM_SENT_FOLDER
        });
        const xBmSentFolder = { name: MessageHeader.X_BM_SENT_FOLDER, values: [sentFolderUid] };
        store.commit("mail/" + SET_MESSAGE_HEADERS, {
            messageKey: message.key,
            headers: [...message.headers, xBmSentFolder]
        });
    }
}

export function getIdentityForNewMessage() {
    const currentMailbox = store.getters[`mail/${CURRENT_MAILBOX}`];
    const identities = store.state["root-app"].identities;
    const autoSelectFromPref = store.state.settings.auto_select_from;
    const defaultIdentity = store.getters["root-app/DEFAULT_IDENTITY"];
    if (autoSelectFromPref === "replies_and_new_messages") {
        return findIdentityFromMailbox(currentMailbox, identities, defaultIdentity);
    }
    return defaultIdentity;
}

export function getIdentityId(headers = []) {
    return headers.find(({ name }) => name === MessageHeader.X_BM_DRAFT_IDENTITY)?.values?.[0];
}

export function getIdentityForReplyOrForward(previousMessage) {
    const currentMailbox = store.getters["mail/" + CURRENT_MAILBOX];
    const identities = store.state["root-app"].identities;
    const autoSelectFromPref = store.state.settings.auto_select_from;
    if (autoSelectFromPref === "only_replies" || autoSelectFromPref === "replies_and_new_messages") {
        const defaultIdentity = store.getters["root-app/DEFAULT_IDENTITY"];
        return computeIdentityForReplyOrForward(previousMessage, identities, currentMailbox, defaultIdentity);
    }
    return getIdentityForNewMessage();
}

export function setIdentity(identity, message) {
    const fullIdentity = store.state["root-app"].identities.find(i => i.id === identity.id);
    setIdentityHeader(fullIdentity.id, message);
    store.commit("mail/" + SET_PERSONAL_SIGNATURE, { html: fullIdentity.signature, id: fullIdentity.id });
    return fullIdentity;
}

function setIdentityHeader(identityId, message) {
    store.commit(`mail/${REMOVE_MESSAGE_HEADER}`, {
        messageKey: message.key,
        headerName: MessageHeader.X_BM_DRAFT_IDENTITY
    });
    const identityHeader = {
        name: MessageHeader.X_BM_DRAFT_IDENTITY,
        values: [identityId]
    };
    const headers = message.headers?.length ? [...message.headers, identityHeader] : [identityHeader];
    store.commit(`mail/${SET_MESSAGE_HEADERS}`, { messageKey: message.key, headers });
}

async function computeDestinationMailbox(rawIdentity, mailboxes) {
    let destinationMailboxUid = inject("UserSession").userId;
    if (rawIdentity.sentFolder !== DEFAULT_FOLDERS.SENT) {
        const mailboxInStore = mailboxes[`user.${rawIdentity.mailboxUid}`] || mailboxes[rawIdentity.mailboxUid];
        let writable;
        if (mailboxInStore) {
            writable = mailboxInStore.writable;
        } else {
            const mailboxContainer = await inject("ContainersPersistence").get(
                "mailbox:acls-" + rawIdentity.mailboxUid
            );
            writable = mailboxContainer.verbs.includes(Verb.Write);
        }
        if (writable) {
            destinationMailboxUid = rawIdentity.mailboxUid;
        }
    }
    return destinationMailboxUid;
}
