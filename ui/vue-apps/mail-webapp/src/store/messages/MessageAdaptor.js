import cloneDeep from "lodash.clonedeep";
import pick from "lodash.pick";

import ItemUri from "@bluemind/item-uri";
import { MessageBodyRecipientKind as RecipientKind } from "@bluemind/backend.mail.api";

import GetAttachmentPartsVisitor from "./GetAttachmentPartsVisitor";
import GetInlinePartsVisitor from "./GetInlinePartsVisitor";
import PartsAddressesByMimeTypeVisitor from "./PartsAddressesByMimeTypeVisitor";
import MessageStatus from "./MessageStatus";
import TreeWalker from "./TreeWalker";

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
            date: new Date(remote.value.body.date),
            from: buildFrom(remote),
            to: remote.value.body.recipients
                .filter(rcpt => rcpt.kind === RecipientKind.Primary)
                .map(rcpt => rcpt.address),
            cc: remote.value.body.recipients
                .filter(rcpt => rcpt.kind === RecipientKind.CarbonCopy)
                .map(rcpt => rcpt.address),
            bcc: remote.value.body.recipients
                .filter(rcpt => rcpt.kind === RecipientKind.BlindCarbonCopy)
                .map(rcpt => rcpt.address),
            // FIXME ? for those 3 following properties there are only used when sending a message. Maybe we can just use remote for this case
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

    realToMailboxItem(local, structure) {
        return {
            body: {
                date: local.date.getTime(),
                subject: local.subject,
                headers: local.headers,
                recipients: buildRecipients(local),
                messageId: local.messageId,
                references: local.references,
                structure
            },
            imapUid: local.remoteRef.imapUid,
            flags: local.flags
        };
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

// FIXME: remove DUPLICATED FUNCTION (see MessageBuilder in deprecated store)
function computeParts(structure) {
    const inlineVisitor = new GetInlinePartsVisitor();
    const attachmentVisitor = new GetAttachmentPartsVisitor();
    const partsAddressesByMimeTypeVisitor = new PartsAddressesByMimeTypeVisitor();

    const walker = new TreeWalker(structure, [inlineVisitor, attachmentVisitor, partsAddressesByMimeTypeVisitor]);
    walker.walk();

    return {
        attachments: attachmentVisitor.result(),
        inlinePartsByCapabilities: inlineVisitor.result(),
        partsAddressesByMimeType: partsAddressesByMimeTypeVisitor.result()
    };
}

function buildRecipients(message) {
    const primaries = buildRecipientsForKind(RecipientKind.Primary, message.to);
    const carbonCopies = buildRecipientsForKind(RecipientKind.CarbonCopy, message.cc);
    const blindCarbonCopies = buildRecipientsForKind(RecipientKind.BlindCarbonCopy, message.bcc);
    const originator = [
        {
            kind: RecipientKind.Originator,
            address: message.from.address,
            dn: message.from.name
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

function buildFrom(remote) {
    const from = remote.value.body.recipients.find(rcpt => rcpt.kind === RecipientKind.Primary);
    return {
        address: from.address,
        name: from.dn
    };
}
