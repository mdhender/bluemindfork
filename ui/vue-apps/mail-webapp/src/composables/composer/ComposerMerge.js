import { computed, ref } from "vue";
import { InlineImageHelper, MimeType } from "@bluemind/email";
import { preventStyleInvading, removeDuplicatedIds, sanitizeHtml } from "@bluemind/html-utils";
import { draftUtils, messageUtils, partUtils } from "@bluemind/mail";
import store from "@bluemind/store";

import { FETCH_PART_DATA, SET_DRAFT_CONTENT } from "~/actions";
import { SET_MESSAGE_BCC, SET_MESSAGE_CC, SET_MESSAGE_HEADERS, SET_MESSAGE_SUBJECT, SET_MESSAGE_TO } from "~/mutations";

const { COMPOSER_CAPABILITIES, getEditorContent } = draftUtils;

const { getPartsFromCapabilities } = partUtils;
const { MessageHeader } = messageUtils;

export function useComposerMerge() {
    const partsByMessageKey = computed(() => store.state.mail.partsData.partsByMessageKey);
    const userPrefTextOnly = ref(false); // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88

    async function mergeBody(message, previousMessage, inlinePartsByCapabilities) {
        const parts = getPartsFromCapabilities({ inlinePartsByCapabilities }, COMPOSER_CAPABILITIES);

        await store.dispatch(`mail/${FETCH_PART_DATA}`, {
            messageKey: previousMessage.key,
            folderUid: previousMessage.folderRef.uid,
            imapUid: previousMessage.remoteRef.imapUid,
            parts: parts.filter(
                part => MimeType.isHtml(part) || MimeType.isText(part) || (MimeType.isImage(part) && part.contentId)
            )
        });
        let content = getEditorContent(
            userPrefTextOnly.value,
            parts,
            partsByMessageKey.value[previousMessage.key],
            store.state.settings.lang
        );

        if (!userPrefTextOnly.value) {
            const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);

            const result = await InlineImageHelper.insertAsBase64(
                [content],
                partsWithCid,
                partsByMessageKey.value[previousMessage.key]
            );
            content = sanitizeHtml(result.contentsWithImageInserted[0]);
            content = preventStyleInvading(content);
            content = removeDuplicatedIds(content);
        }
        store.dispatch(`mail/${SET_DRAFT_CONTENT}`, { html: content, draft: message });
    }

    async function mergeSubject(message, related) {
        store.commit(`mail/${SET_MESSAGE_SUBJECT}`, { messageKey: message.key, subject: related.subject });
    }

    async function mergeRecipients(message, { to, cc, bcc }) {
        const rcpts = message.to;
        let recipients = message.to.concat(to.filter(to => rcpts.every(rcpt => to.address !== rcpt.address)));
        store.commit(`mail/${SET_MESSAGE_TO}`, { messageKey: message.key, to: recipients });
        rcpts.push(...message.cc);
        recipients = message.cc.concat(cc.filter(cc => rcpts.every(rcpt => cc.address !== rcpt.address)));
        store.commit(`mail/${SET_MESSAGE_CC}`, { messageKey: message.key, cc: recipients });
        rcpts.push(...message.bcc);
        recipients = message.bcc.concat(bcc.filter(bcc => rcpts.every(rcpt => bcc.address !== rcpt.address)));
        store.commit(`mail/${SET_MESSAGE_BCC}`, { messageKey: message.key, bcc: recipients });
    }

    function mergeHeaders(message, related) {
        const headers = [...message.headers];
        const MERGEABLE_HEADERS = [MessageHeader.DISPOSITION_NOTIFICATION_TO];
        MERGEABLE_HEADERS.forEach(headerName => {
            const mergeableHeader = related.headers.find(header => header.name === headerName);
            if (mergeableHeader && !headers.some(header => header.name === headerName)) {
                headers.push(mergeableHeader);
            }
        });
        store.commit(`mail/${SET_MESSAGE_HEADERS}`, { messageKey: message.key, headers });
    }

    return {
        mergeBody,
        mergeSubject,
        mergeRecipients,
        mergeHeaders
    };
}
