import cloneDeep from "lodash.clonedeep";
import pick from "lodash.pick";

import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";
import { RecipientKind } from "@bluemind/backend.mail.api";

import MessageStatus from "./MessageStatus";

export default {
    fromMailboxItem(remote, { key, uid }) {
        return {
            key: ItemUri.encode(remote.internalId, key),
            folderRef: { key, uid },
            remoteRef: {
                imapUid: remote.value.imapUid,
                internalId: remote.internalId
            },
            status: MessageStatus.LOADED,
            flags: remote.value.flags,
            to: remote.value.body.recipients
                .filter(rcpt => rcpt.kind === RecipientKind.Primary)
                .map(rcpt => rcpt.address),
            cc: remote.value.body.recipients
                .filter(rcpt => rcpt.kind === RecipientKind.CarbonCopy)
                .map(rcpt => rcpt.address),
            bcc: remote.value.body.recipients
                .filter(rcpt => rcpt.kind === RecipientKind.BlindCarbonCopy)
                .map(rcpt => rcpt.address),
            // FIXME ? put 3 following properties in messageCompose state ?
            messageId: remote.value.body.messageId,
            references: remote.value.body.references,
            headers: remote.value.body.headers,
            /**
             * FIXME ?
             * If computeParts cost too much perf then :
             *      - exceptionnaly we should set here "structure" property which is non-adapted
             *      - move this computation in MailComposer & MailViewer components
             */
            ...computeParts(remote.value.body.structure),
            subject: remote.value.body.subject,
            composing: false,
            remote
        };
    },

    // DELETE ME ONCE deprecated messages getter is removed
    toMailboxItem(local) {
        return {
            ...local.remote,
            value: {
                ...local.remote.value,
                flags: local.flags
            }
        };
    },

    realToMailboxItem(local, sender, senderName, isSeen, structure) {
        let mailboxItem = {
            body: {
                subject: local.subject,
                headers: local.headers,
                recipients: buildRecipients(sender, senderName, local),
                messageId: local.messageId,
                references: local.references,
                structure
            },
            flags: isSeen ? [Flag.SEEN] : []
        };
        return mailboxItem;
    },

    create(internalId, { key, remoteRef: { uid } }) {
        return {
            key: ItemUri.encode(internalId, key),
            folderRef: { key, uid },
            remoteRef: { internalId },
            status: MessageStatus.NOT_LOADED
        };
    },

    partialCopy(message, properties = []) {
        return cloneDeep(pick(message, properties.concat("key", "folderRef", "status", "remoteRef")));
    }
};

// FIXME: THOSE IMPORT MUST BE MOVED IN NEW STORE
import GetAttachmentPartsVisitor from "../../store.deprecated/mailbackend/MailboxItemsStore/GetAttachmentPartsVisitor";
import GetInlinePartsVisitor from "../../store.deprecated/mailbackend/MailboxItemsStore/GetInlinePartsVisitor";
import TreeWalker from "../../store.deprecated/mailbackend/MailboxItemsStore/TreeWalker";

// FIXME: remove DUPLICATED FUNCTION (see MessageBuilder in deprecated store)
function computeParts(structure) {
    const inlineVisitor = new GetInlinePartsVisitor();
    const attachmentVisitor = new GetAttachmentPartsVisitor();
    const walker = new TreeWalker(structure, [inlineVisitor, attachmentVisitor]);
    walker.walk();
    return {
        inlinePartsByCapabilities: inlineVisitor.result(),
        attachments: attachmentVisitor.result()
    };
}

function buildRecipients(sender, senderName, message) {
    const primaries = buildRecipientsForKind(RecipientKind.Primary, message.to);
    const carbonCopies = buildRecipientsForKind(RecipientKind.CarbonCopy, message.cc);
    const blindCarbonCopies = buildRecipientsForKind(RecipientKind.BlindCarbonCopy, message.bcc);
    const originator = [
        {
            kind: RecipientKind.Originator,
            address: sender,
            dn: senderName
        }
    ];

    return primaries.concat(carbonCopies).concat(blindCarbonCopies).concat(originator);
}

function buildRecipientsForKind(kind, addresses) {
    return (addresses || []).map(address => {
        return {
            kind: kind,
            address: address,
            dn: "" // FIXME should provide the displayed name here
        };
    });
}
