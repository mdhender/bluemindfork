import cloneDeep from "lodash.clonedeep";
import { Flag, MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

import { draftUtils, messageUtils } from "@bluemind/mail";
import { ADD_FLAG } from "~/actions";
import {
    SET_MESSAGE_DATE,
    SET_MESSAGE_HEADERS,
    SET_MESSAGE_INTERNAL_ID,
    SET_MESSAGE_IMAP_UID,
    SET_MESSAGES_STATUS,
    SET_SAVE_ERROR,
    SET_MESSAGE_SIZE,
    DELETE_FLAG
} from "~/mutations";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";
import { VCard } from "@bluemind/addressbook.api";
import { fetchMembersWithAddress } from "@bluemind/contact";

const { isNewMessage } = draftUtils;
const { MessageAdaptor, MessageHeader, MessageStatus, generateMessageIDHeader } = messageUtils;

export function isReadyToBeSaved(draft) {
    return (
        draft.status === MessageStatus.IDLE ||
        draft.status === MessageStatus.NEW ||
        draft.status === MessageStatus.SAVE_ERROR
    );
}
function isValidDraft(draft) {
    const hasNullAddress = node => {
        if (!MimeType.isMultipart(node) && !node.address) {
            return true;
        }
        return node.children?.some(hasNullAddress);
    };
    return !hasNullAddress(draft.structure);
}

export async function save({ commit, dispatch }, draft) {
    if (!isValidDraft(draft)) {
        return;
    }
    const service = inject("MailboxItemsPersistence", draft.folderRef.uid);
    try {
        commit(SET_MESSAGES_STATUS, [{ key: draft.key, status: MessageStatus.SAVING }]);

        const recipients = await expandGroups(draft);
        await manageDispositionNotification({ commit, dispatch }, draft);
        await createEmlOnServer({ commit }, { ...draft, ...recipients }, service);

        commit(SET_MESSAGES_STATUS, [{ key: draft.key, status: MessageStatus.IDLE }]);
        commit(SET_SAVE_ERROR, null);
    } catch (err) {
        // eslint-disable-next-line no-console
        console.error(err);
        commit(SET_MESSAGES_STATUS, [{ key: draft.key, status: MessageStatus.SAVE_ERROR }]);
        commit(SET_SAVE_ERROR, err);
    }
}

async function createEmlOnServer({ commit }, draft, service) {
    const { saveDate, headers } = forceMailRewriteOnServer(draft);

    if (!headers.find(h => h.name.toUpperCase() === MessageHeader.MESSAGE_ID.toUpperCase())) {
        headers.push(generateMessageIDHeader(draft.from.address));
    }

    commit(SET_MESSAGE_HEADERS, { messageKey: draft.key, headers });
    commit(SET_MESSAGE_DATE, { messageKey: draft.key, date: saveDate });

    const remoteMessage = MessageAdaptor.toMailboxItem({ ...draft, headers }, draft.structure);
    const { imapUid, id: internalId } = isNewMessage(draft)
        ? await service.create(remoteMessage)
        : {
              ...(await service.updateById(draft.remoteRef.internalId, remoteMessage)),
              id: draft.remoteRef.internalId
          };
    commit(SET_MESSAGE_INTERNAL_ID, { key: draft.key, internalId });
    commit(SET_MESSAGE_IMAP_UID, { key: draft.key, imapUid });
    const mailItem = await inject("MailboxItemsPersistence", draft.folderRef.uid).getCompleteById(internalId);
    const adapted = MessageAdaptor.fromMailboxItem(mailItem, FolderAdaptor.toRef(draft.folderRef.uid));
    commit(SET_MESSAGE_SIZE, { key: draft.key, size: adapted.size });
}

/**
 * Needed by BM core to detect if mail has changed when using IMailboxItems.updateById
 */
function forceMailRewriteOnServer(draft) {
    const headers = cloneDeep(draft.headers);
    const saveDate = new Date();

    const xBmDraftKeyHeader = headers.find(header => header.name === MessageHeader.X_BM_DRAFT_REFRESH_DATE);
    if (xBmDraftKeyHeader) {
        xBmDraftKeyHeader.values = [saveDate.getTime()];
    } else {
        headers.push({ name: MessageHeader.X_BM_DRAFT_REFRESH_DATE, values: [saveDate.getTime()] });
    }

    // X-Mailer rfc2076 @see https://www.rfc-editor.org/rfc/rfc2076#section-3.4
    const xMailerHeader = headers.find(header => header.name === MessageHeader.X_MAILER);
    const versionMajorNumber = inject("UserSession").bmBrandVersion.split(".")[0];
    const version = `BlueMind-MailApp-v${versionMajorNumber}`;
    if (xMailerHeader) {
        xMailerHeader.values = [version];
    } else {
        headers.push({ name: MessageHeader.X_MAILER, values: [version] });
    }

    return { saveDate, headers };
}

/** Recursively convert groups having no address to recipients with an address.  */
async function expandGroups(draft) {
    const to = await expandGroupRecipients(draft.to);
    const cc = await expandGroupRecipients(draft.cc);
    const bcc = await expandGroupRecipients(draft.bcc);

    return { to, cc, bcc };
}

async function expandGroupRecipients(recipients) {
    const expanded = [];
    await Promise.all(
        recipients?.map(async recipient => {
            if (recipient.kind === VCard.Kind.group && !recipient.address) {
                expanded.push(...(await fetchMembersWithAddress(recipient.containerUid, recipient.uid)));
            } else {
                expanded.push(recipient);
            }
        })
    );
    return expanded;
}

async function manageDispositionNotification({ commit, dispatch }, draft) {
    const index = draft.headers.findIndex(
        header =>
            new RegExp(MessageHeader.DISPOSITION_NOTIFICATION_TO, "i").test(header.name) &&
            header.values?.filter(Boolean)?.length
    );
    if (index >= 0) {
        await dispatch(ADD_FLAG, { messages: [draft], flag: Flag.MDN_SENT });
    } else {
        commit(DELETE_FLAG, { messages: [draft], flag: Flag.MDN_SENT });
    }
}
