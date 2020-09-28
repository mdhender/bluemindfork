import cloneDeep from "lodash.clonedeep";
import pick from "lodash.pick";

import ItemUri from "@bluemind/item-uri";
import { MessageBodyRecipientKind as RecipientKind } from "@bluemind/backend.mail.api";

import GetAttachmentPartsVisitor from "./GetAttachmentPartsVisitor";
import GetInlinePartsVisitor from "./GetInlinePartsVisitor";
import GetMultiPartAddressesVisitor from "./GetMultiPartAddressesVisitor";
import { MessageStatus } from "../../../model/message";
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
            ...computeRecipients(remote),
            messageId: remote.value.body.messageId,
            references: remote.value.body.references || [],
            headers: remote.value.body.headers,
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

    createWithMetadata(myDraftsFolder, { defaultEmail, formatedName }, structure) {
        return {
            key: null,
            folderRef: { key: myDraftsFolder.key, uid: myDraftsFolder.remoteRef.uid },
            remoteRef: { imapUid: null, internalId: null },
            status: MessageStatus.LOADED,
            flags: [],
            date: new Date(),
            from: {
                address: defaultEmail,
                name: formatedName
            },
            to: [],
            cc: [],
            bcc: [],
            messageId: "",
            references: [],
            headers: [],
            ...computeParts(structure),
            subject: "",
            composing: true
        };
    },

    partialCopy(message, properties = []) {
        return cloneDeep(pick(message, properties.concat("key", "folderRef", "status", "remoteRef")));
    }
};

// FIXME: remove DUPLICATED FUNCTION once MailViewer use new store (see Message.js in deprecated store)
function computeParts(structure) {
    const inlineVisitor = new GetInlinePartsVisitor();
    const attachmentVisitor = new GetAttachmentPartsVisitor();
    const multipartAddressesVisitor = new GetMultiPartAddressesVisitor();

    const walker = new TreeWalker(structure, [inlineVisitor, attachmentVisitor, multipartAddressesVisitor]);
    walker.walk();

    return {
        attachments: attachmentVisitor.result(),
        inlinePartsByCapabilities: inlineVisitor.result(),
        multipartAddresses: multipartAddressesVisitor.result()
    };
}

function computeRecipients(remote) {
    const from = remote.value.body.recipients.find(rcpt => rcpt.kind === RecipientKind.Originator);
    return {
        from: { dn: from.dn, address: from.address },
        to: remote.value.body.recipients
            .filter(rcpt => rcpt.kind === RecipientKind.Primary)
            .map(rcpt => ({ dn: rcpt.dn, address: rcpt.address })),
        cc: remote.value.body.recipients
            .filter(rcpt => rcpt.kind === RecipientKind.CarbonCopy)
            .map(rcpt => ({ dn: rcpt.dn, address: rcpt.address })),
        bcc: remote.value.body.recipients
            .filter(rcpt => rcpt.kind === RecipientKind.BlindCarbonCopy)
            .map(rcpt => ({ dn: rcpt.dn, address: rcpt.address }))
    };
}

function buildRecipients(local) {
    const primaries = buildRecipientsForKind(RecipientKind.Primary, local.to);
    const carbonCopies = buildRecipientsForKind(RecipientKind.CarbonCopy, local.cc);
    const blindCarbonCopies = buildRecipientsForKind(RecipientKind.BlindCarbonCopy, local.bcc);
    const originator = [
        {
            kind: RecipientKind.Originator,
            address: local.from.address,
            dn: local.from.name
        }
    ];

    return primaries.concat(carbonCopies).concat(blindCarbonCopies).concat(originator);
}

function buildRecipientsForKind(kind, recipients) {
    return (recipients || []).map(recipient => {
        return {
            kind: kind,
            address: recipient.address,
            dn: recipient.name
        };
    });
}
