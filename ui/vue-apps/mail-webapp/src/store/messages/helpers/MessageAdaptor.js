import merge from "lodash.merge";

import { MessageBodyRecipientKind as RecipientKind } from "@bluemind/backend.mail.api";

import GetAttachmentPartsVisitor from "./GetAttachmentPartsVisitor";
import GetInlinePartsVisitor from "./GetInlinePartsVisitor";
import { createWithMetadata, MessageHeader, MessageStatus } from "../../../model/message";
import TreeWalker from "./TreeWalker";

export default {
    fromMailboxItem(remote, { key, uid }) {
        const message = createWithMetadata({ internalId: remote.internalId, folder: { key, uid } });
        const adapted = {
            remoteRef: {
                imapUid: remote.value.imapUid
            },
            flags: remote.value.flags,
            date: new Date(remote.value.body.date),
            ...computeRecipients(remote),
            messageId: remote.value.body.messageId,
            references: remote.value.body.references || [],
            headers: remote.value.body.headers,
            ...this.computeParts(remote.value.body.structure),
            subject: remote.value.body.subject,
            composing: false,
            status: MessageStatus.LOADED,
            preview: remote.value.body.preview,
            hasAttachment: remote.value.body.smartAttach,
            hasICS: remote.value.body.headers.some(({ name }) => name === MessageHeader.X_BM_EVENT)
        };
        return merge(message, adapted);
    },

    toMailboxItem(local, structure) {
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

    computeParts(structure) {
        const inlineVisitor = new GetInlinePartsVisitor();
        const attachmentVisitor = new GetAttachmentPartsVisitor();

        const walker = new TreeWalker(structure, [inlineVisitor, attachmentVisitor]);
        walker.walk();

        return {
            attachments: attachmentVisitor.result(),
            inlinePartsByCapabilities: inlineVisitor.result()
        };
    }
};

function computeRecipients(remote) {
    const from = remote.value.body.recipients.find(rcpt => rcpt.kind === RecipientKind.Originator) || {
        dn: "Anonymous",
        address: "no-reply@no-reply.com"
    };
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
    const originator = buildRecipientsForKind(RecipientKind.Originator, [local.from]);

    return primaries.concat(carbonCopies).concat(blindCarbonCopies).concat(originator);
}

function buildRecipientsForKind(kind, recipients) {
    return (recipients || []).map(recipient => ({
        kind: kind,
        address: recipient.address,
        dn: recipient.dn
    }));
}
