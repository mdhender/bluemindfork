import merge from "lodash.merge";

import { MessageBodyRecipientKind as RecipientKind } from "@bluemind/backend.mail.api";

import GetAttachmentPartsVisitor from "./GetAttachmentPartsVisitor";
import GetInlinePartsVisitor from "./GetInlinePartsVisitor";
import { createWithMetadata, MessageHeader, MessageStatus } from "~/model/message";
import TreeWalker from "./TreeWalker";
import { LoadingStatus } from "~/model/loading-status";

export default {
    fromMailboxItem(remote, { key, uid }) {
        const message = createWithMetadata({ internalId: remote.internalId, folder: { key, uid } });
        const eventInfo = getEventInfo(remote.value.body.headers);
        const adapted = {
            remoteRef: { imapUid: remote.value.imapUid },
            flags: remote.value.flags,
            date: new Date(remote.value.body.date),
            ...computeRecipients(remote),
            messageId: remote.value.body.messageId,
            version: remote.version,
            conversationId: remote.value.conversationId,
            headers: remote.value.body.headers,
            ...this.computeParts(remote.value.body.structure),
            subject: remote.value.body.subject,
            composing: false,
            status: MessageStatus.IDLE,
            loading: LoadingStatus.LOADED,
            preview: remote.value.body.preview,
            hasAttachment: remote.value.body.smartAttach,
            hasICS: eventInfo.hasICS,
            eventInfo
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
                structure: structure
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

function getEventInfo(headers) {
    let isCounterEvent = false;
    const icsHeader = headers.find(({ name }) => {
        if (MessageHeader.X_BM_EVENT_COUNTERED.toUpperCase() === name.toUpperCase()) {
            isCounterEvent = true;
            return true;
        }
        if (MessageHeader.X_BM_EVENT.toUpperCase() === name.toUpperCase()) {
            return true;
        }
    });

    if (!icsHeader) {
        return { hasICS: false };
    }

    let isResourceBooking = false,
        resourceUid = "";
    const resourceBooking = headers.find(
        ({ name }) => MessageHeader.X_BM_RESOURCEBOOKING.toUpperCase() === name.toUpperCase()
    );
    if (resourceBooking) {
        isResourceBooking = true;
        resourceUid = resourceBooking.values[0].trim();
    }

    const icsHeaderValue = icsHeader.values[0].trim();
    const semiColonIndex = icsHeaderValue.indexOf(";");
    const uid = semiColonIndex === -1 ? icsHeaderValue : icsHeaderValue.substring(0, semiColonIndex);
    let recuridIsoDate = icsHeaderValue.match(/recurid="(.*?)"/i);
    recuridIsoDate = recuridIsoDate && recuridIsoDate[1];

    const hasICS = !!uid;
    const eventUid = isCounterEvent || recuridIsoDate ? null : uid;
    const icsUid = isCounterEvent || recuridIsoDate ? uid : null;
    const needsReply =
        isCounterEvent || icsHeaderValue.includes('rsvp="true"') || icsHeaderValue.includes("rsvp='true'"); //TODO regexp

    return { hasICS, isCounterEvent, eventUid, icsUid, needsReply, recuridIsoDate, isResourceBooking, resourceUid };
}
