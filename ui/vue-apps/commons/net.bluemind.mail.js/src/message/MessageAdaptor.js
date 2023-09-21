import merge from "lodash.merge";

import { MessageBody } from "@bluemind/backend.mail.api";

import { createWithMetadata, hasXbmImipEvent, MessageHeader, MessageStatus } from "./index";
import { LoadingStatus } from "../loading-status";
import { hasAttachment } from "./structureParsers";

export default {
    fromMailboxItem(remote, { key, uid }) {
        const message = createWithMetadata({ internalId: remote.internalId, folder: { key, uid } });
        const eventInfo = getEventInfo(remote.value.body.headers);
        const adapted = {
            remoteRef: { imapUid: remote.value.imapUid },
            structure: remote.value.body.structure,
            flags: remote.value.flags,
            date: new Date(remote.value.body.date),
            ...computeRecipients(remote.value.body.recipients),
            messageId: remote.value.body.messageId,
            version: remote.version,
            conversationId: remote.value.conversationId,
            headers: remote.value.body.headers,
            size: remote.value.body.size / 1.33, // take into account the email base64 encoding : 33% more space
            subject: remote.value.body.subject,
            status: MessageStatus.IDLE,
            loading: LoadingStatus.LOADED,
            preview: remote.value.body.preview,
            hasAttachment: hasAttachment(remote.value.body.structure),
            hasICS: eventInfo.hasICS,
            eventInfo
        };
        return merge(message, adapted);
    },

    toMailboxItem(local, structure) {
        return {
            body: {
                date: new Date(local.date).getTime(),
                subject: local.subject,
                headers: local.headers,
                recipients: buildRecipients(local),
                messageId: local.messageId,
                structure: structure
            },
            imapUid: local.remoteRef.imapUid,
            flags: local.flags
        };
    }
};

function computeRecipients(remoteRecipients) {
    const from = remoteRecipients.find(rcpt => rcpt.kind === MessageBody.RecipientKind.Originator) || {
        dn: "Anonymous",
        address: "no-reply@no-reply.com"
    };
    return {
        from: { dn: normalizeDn(from.dn), address: from.address },
        to: remoteRecipients
            .filter(rcpt => rcpt.kind === MessageBody.RecipientKind.Primary)
            .map(rcpt => ({ dn: normalizeDn(rcpt.dn), address: rcpt.address })),
        cc: remoteRecipients
            .filter(rcpt => rcpt.kind === MessageBody.RecipientKind.CarbonCopy)
            .map(rcpt => ({ dn: normalizeDn(rcpt.dn), address: rcpt.address })),
        bcc: remoteRecipients
            .filter(rcpt => rcpt.kind === MessageBody.RecipientKind.BlindCarbonCopy)
            .map(rcpt => ({ dn: normalizeDn(rcpt.dn), address: rcpt.address })),
        sender: remoteRecipients
            .filter(rcpt => rcpt.kind === MessageBody.RecipientKind.Sender)
            .map(rcpt => ({ dn: normalizeDn(rcpt.dn), address: rcpt.address }))[0]
    };
}

// inspired from RFC https://www.rfc-editor.org/rfc/rfc5322#section-3.2.1
function normalizeDn(dn) {
    let normalized = "";
    [...(dn || "")].forEach((char, index) => {
        const next = dn[index + 1];
        if (char === "\\" && next && next !== "\\") {
            return;
        }
        normalized += char;
    });
    return normalized;
}

function buildRecipients(local) {
    const primaries = buildRecipientsForKind(MessageBody.RecipientKind.Primary, local.to);
    const carbonCopies = buildRecipientsForKind(MessageBody.RecipientKind.CarbonCopy, local.cc);
    const blindCarbonCopies = buildRecipientsForKind(MessageBody.RecipientKind.BlindCarbonCopy, local.bcc);
    const originator = buildRecipientsForKind(MessageBody.RecipientKind.Originator, [local.from]);

    return primaries.concat(carbonCopies).concat(blindCarbonCopies).concat(originator);
}

function buildRecipientsForKind(kind, recipients) {
    return (recipients || []).map(recipient => ({ kind, dn: recipient.dn, address: recipient.address }));
}

export function getEventInfo(headers) {
    const icsHeader = headers.find(hasXbmImipEvent);

    if (!icsHeader) {
        return { hasICS: false };
    }

    const isCounterEvent =
        Boolean(icsHeader) && MessageHeader.X_BM_EVENT_COUNTERED.toUpperCase() === icsHeader.name.toUpperCase();

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
    const calendarHeader = headers.find(({ name }) => name.toUpperCase() === MessageHeader.X_BM_CALENDAR.toUpperCase());
    const calendarUid = calendarHeader
        ? calendarHeader.values[0].trim()
        : (icsHeaderValue.match(/calendar_uid="(.*?)"/i) ?? [])[1];
    let recuridIsoDate = icsHeaderValue.match(/recurid="(.*?)"/i);
    recuridIsoDate = recuridIsoDate && recuridIsoDate[1];

    const hasICS = !!uid;
    const needsReply = isCounterEvent || Boolean(icsHeaderValue.match(/rsvp=["']true["']/i));

    return {
        hasICS,
        isCounterEvent,
        icsUid: uid,
        needsReply,
        recuridIsoDate,
        isResourceBooking,
        resourceUid,
        calendarUid
    };
}
