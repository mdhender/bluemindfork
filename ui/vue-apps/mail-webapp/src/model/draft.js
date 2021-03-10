import { EmailExtractor, Flag, MimeType } from "@bluemind/email";

import { AttachmentStatus } from "./attachment";
import {
    MessageHeader,
    // fetch,
    createWithMetadata as createMessage,
    MessageCreationModes,
    MessageForwardAttributeSeparator,
    MessageReplyAttributeSeparator,
    MessageStatus
} from "./message";
import { mergePartsForRichEditor, mergePartsForTextarea } from "./part";
import { removeSignatureIds } from "./signature";

const FAKED_INTERNAL_ID = "faked-internal-id";

export function isInternalIdFaked(internalId) {
    return internalId === FAKED_INTERNAL_ID;
}

export function createEmpty(myDraftsFolder, userSession) {
    const metadata = {
        internalId: FAKED_INTERNAL_ID,
        folder: { key: myDraftsFolder.key, uid: myDraftsFolder.remoteRef.uid }
    };
    const message = createMessage(metadata);

    message.date = new Date();
    message.from = {
        address: userSession.defaultEmail,
        dn: userSession.formatedName
    };
    message.flags = [Flag.SEEN];
    message.status = MessageStatus.LOADED;
    message.composing = true;
    message.remoteRef.imapUid = "1"; // faked imapUid because updateById needs it
    return message;
}

export function createReplyOrForward(previousMessage, myDraftsFolder, userSession, creationMode) {
    const message = createEmpty(myDraftsFolder, userSession);

    const draftInfoHeader = {
        type: creationMode,
        messageInternalId: previousMessage.remoteRef.internalId,
        folderUid: previousMessage.folderRef.uid
    };
    message.headers = [{ name: MessageHeader.X_BM_DRAFT_INFO, values: [JSON.stringify(draftInfoHeader)] }];

    if (creationMode === MessageCreationModes.REPLY_ALL || creationMode === MessageCreationModes.REPLY) {
        message.to = computeToRecipients(
            creationMode,
            previousMessage,
            userSession.defaultEmail,
            userSession.formatedName
        );
        message.cc = computeCcRecipients(creationMode, previousMessage);
    }
    if (creationMode === MessageCreationModes.FORWARD) {
        message.attachments = previousMessage.attachments.map(attachment => ({
            ...attachment,
            status: AttachmentStatus.NOT_LOADED
        }));
    }

    message.subject = computeSubject(creationMode, previousMessage);
    if (previousMessage.messageId) {
        const inReplyToHeader = {
            name: MessageHeader.IN_REPLY_TO,
            values: [previousMessage.messageId]
        };
        message.headers.push(inReplyToHeader);
        message.references = [previousMessage.messageId].concat(previousMessage.references);
    } else {
        message.references = previousMessage.references;
    }

    return message;
}

// INTERNAL METHOD (exported only for testing purpose)
export function computeCcRecipients(creationMode, previousMessage) {
    let cc = [];
    const mailFollowUpTo = previousMessage.headers.find(header => header.name === MessageHeader.MAIL_FOLLOWUP_TO);
    if (creationMode === MessageCreationModes.REPLY_ALL && !mailFollowUpTo) {
        cc = previousMessage.cc.slice(0);
    }
    return cc;
}

// INTERNAL METHOD (exported only for testing purpose)
export function computeToRecipients(creationMode, previousMessage, myEmail, myName) {
    let to = [];

    const isReplyAll = creationMode === MessageCreationModes.REPLY_ALL;
    const mailFollowUpTo = previousMessage.headers.find(header => header.name === MessageHeader.MAIL_FOLLOWUP_TO);

    const mailReplyToHeader = previousMessage.headers.find(header => header.name === MessageHeader.MAIL_REPLY_TO);
    const replyToHeader = previousMessage.headers.find(header => header.name === MessageHeader.REPLY_TO);

    if (isReplyAll && mailFollowUpTo) {
        to = extractAddressesFromHeader(mailFollowUpTo, true);
    } else if (mailReplyToHeader) {
        to = extractAddressesFromHeader(mailReplyToHeader, isReplyAll);
    } else if (replyToHeader) {
        to = extractAddressesFromHeader(replyToHeader, isReplyAll);
    } else {
        // compute recipients from "From" or "To"
        let recipients = [previousMessage.from];
        if (isReplyAll) {
            // respond to sender and all recipients except myself
            recipients.push(...previousMessage.to);
            recipients = recipients.filter(r => r.address !== myEmail);
            // FIXME: avoid duplicates
            if (recipients.length === 0) {
                // I was alone, respond to myself then
                recipients = [{ address: myEmail, name: myName }];
            }
        } else if (recipients.map(r => r.address).includes(myEmail)) {
            // all recipients except myself
            recipients = previousMessage.to.filter(r => r.address !== myEmail);
            if (recipients.length === 0) {
                // I was alone, respond to myself then
                recipients = [{ address: myEmail, name: myName }];
            } else {
                // respond to the first "not me" recipient only
                recipients = [recipients[0]];
            }
        }
        to = recipients;
    }
    return to;
}

function extractAddressesFromHeader(header, isReplyAll) {
    if (isReplyAll) {
        return header.values.map(value => ({ address: EmailExtractor.extractEmail(value), dn: "" }));
    } else {
        return [{ address: EmailExtractor.extractEmail(header.values[0]), dn: "" }];
    }
}

/**
 * Compute the subject in function of creationMode (like "Re: My Subject" when replying).
 */
// INTERNAL METHOD (exported only for testing purpose)
export function computeSubject(creationMode, previousMessage) {
    const subjectPrefix = creationMode === MessageCreationModes.FORWARD ? "Fw: " : "Re: ";
    // avoid subject prefix repetitions (like "Re: Re: Re: Re: My Subject")
    if (subjectPrefix !== previousMessage.subject.substring(0, subjectPrefix.length)) {
        return subjectPrefix + previousMessage.subject;
    }
    return previousMessage.subject;
}

export function getEditorContent(userPrefTextOnly, parts, partsDataByAddress) {
    let content;
    if (userPrefTextOnly) {
        content = mergePartsForTextarea(parts, partsDataByAddress);
    } else {
        content = mergePartsForRichEditor(parts, partsDataByAddress);
    }
    return content;
}

export function handleSeparator(content) {
    let collapsed,
        newContent = content;

    const doc = new DOMParser().parseFromString(content, "text/html");
    const separator =
        doc.querySelector('div[id="' + MessageReplyAttributeSeparator + '"]') ||
        doc.querySelector('div[id="' + MessageForwardAttributeSeparator + '"]');

    if (separator) {
        collapsed = separator.outerHTML;
        separator.remove();
        newContent = doc.body.innerHTML;
    }
    return { content: newContent, collapsed };
}

export const COMPOSER_CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

/**
 * Build the text representing this message as a previous message.
 *
 * @example TEXT
 * `On Tuesday 2019 01 01, John Doe wrote:
 * > Dear Jane,
 * >  I could not bear to see you with Tarzan anymore,
 * > it will kill me! Please come back!
 * ...`
 *
 * @example HTML
 * `On Tuesday 2019 01 01, John Doe wrote:
 * <blockquote>
 * Dear Jane,
 * I could not bear to see you with Tarzan anymore,
 * it will kill me! Please come back!
 * ...
 * </blockquote>`
 */
export function addSeparator(content, previousMessage, creationMode, userPrefTextOnly, vueI18n) {
    let newContent = content;
    if (creationMode === MessageCreationModes.REPLY || creationMode === MessageCreationModes.REPLY_ALL) {
        newContent = adaptPreviousMessageForReply(userPrefTextOnly, content);
    }
    const lineBreakSeparator = userPrefTextOnly ? "\n" : "<br>";
    const separator =
        creationMode === MessageCreationModes.FORWARD
            ? buildSeparatorForForward(previousMessage, lineBreakSeparator, vueI18n)
            : buildSeparatorForReply(previousMessage, lineBreakSeparator, vueI18n);

    newContent = separator + newContent;

    if (!userPrefTextOnly) {
        const id = MessageCreationModes.FORWARD ? MessageForwardAttributeSeparator : MessageReplyAttributeSeparator;
        newContent = '<div id="' + id + '">' + removeSignatureIds(newContent) + "</div>";
    }
    return lineBreakSeparator + newContent;
}

/**
 *  A separator before the previous message (reply).
 */
function buildSeparatorForReply(message, lineBreakSeparator, vueI18n) {
    return (
        "<p>" +
        vueI18n.t("mail.compose.reply.body", {
            date: vueI18n.d(message.date, "full_date_time"),
            name: nameAndAddress(message.from)
        }) +
        lineBreakSeparator +
        "</p>"
    );
}

function adaptPreviousMessageForReply(userPrefTextOnly, content) {
    if (userPrefTextOnly) {
        return (
            "\n\n" +
            content
                .split("\n")
                .map(line => "> " + line)
                .join("\n")
        );
    } else {
        return (
            `<blockquote style="margin-left: 1rem; padding-left: 1rem; border-left: 2px solid black;">` +
            content +
            "</blockquote>"
        );
    }
}

/**
 *  A separator before the previous message (forward).
 */
function buildSeparatorForForward(message, lineBreakSeparator, vueI18n) {
    let separator = vueI18n.t("mail.compose.forward.body") + lineBreakSeparator;
    separator += vueI18n.t("mail.compose.forward.prev.message.info.subject");
    separator += ": " + message.subject + lineBreakSeparator;
    separator += vueI18n.t("mail.compose.forward.prev.message.info.to");
    separator += ": " + message.to.map(to => nameAndAddress(to)) + lineBreakSeparator;
    separator += vueI18n.t("mail.compose.forward.prev.message.info.date");
    separator += ": " + vueI18n.d(message.date, "full_date_time") + lineBreakSeparator;
    separator += vueI18n.t("mail.compose.forward.prev.message.info.from");
    separator += ": " + nameAndAddress(message.from) + lineBreakSeparator + lineBreakSeparator;
    return '<p style="color: purple;">' + separator + "</p>";
}

/** @return like "John Doe <jdoe@bluemind.net>" */
function nameAndAddress(recipient) {
    return recipient.dn ? recipient.dn + " <" + recipient.address + ">" : recipient.address;
}
