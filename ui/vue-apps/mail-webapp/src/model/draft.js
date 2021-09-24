import { EmailExtractor, Flag, MimeType } from "@bluemind/email";
import { createDocumentFragment } from "@bluemind/html-utils";

import MessagePathParam from "~/router/MessagePathParam";
import { AttachmentStatus } from "./attachment";
import { LoadingStatus } from "./loading-status";
import {
    extractHeaderValues,
    MessageHeader,
    // fetch,
    createWithMetadata as createMessage,
    MessageCreationModes,
    MessageForwardAttributeSeparator,
    MessageReplyAttributeSeparator,
    MessageStatus,
    messageKey
} from "./message";
import { mergePartsForRichEditor, mergePartsForTextarea } from "./part";
import { removeSignature, removeSignatureIds } from "./signature";

const TEMPORARY_MESSAGE_ID = 0;
let DRAFT_HASH = 0;

export function isNewMessage({ remoteRef: { internalId } }) {
    return internalId === TEMPORARY_MESSAGE_ID;
}

export function draftKey(myDrafts) {
    return messageKey(DRAFT_HASH, myDrafts.key);
}

export function draftPath(myDrafts) {
    return MessagePathParam.build(undefined, {
        remoteRef: { internalId: TEMPORARY_MESSAGE_ID },
        folderRef: { key: myDrafts.key }
    });
}

// FIXME remove once we use 'real' message ids for new message
export let FIXME_NEW_DRAFT_KEY;

export function createEmpty(myDraftsFolder, userSession) {
    const metadata = {
        internalId: TEMPORARY_MESSAGE_ID,
        folder: { key: myDraftsFolder.key, uid: myDraftsFolder.remoteRef.uid }
    };
    const message = createMessage(metadata);
    FIXME_NEW_DRAFT_KEY = messageKey(--DRAFT_HASH, myDraftsFolder.key);
    message.key = FIXME_NEW_DRAFT_KEY;

    message.date = new Date();
    message.from = {
        address: userSession.defaultEmail,
        dn: userSession.formatedName
    };
    message.flags = [Flag.SEEN];
    message.status = MessageStatus.NEW;
    message.loading = LoadingStatus.LOADED;
    message.composing = true;
    message.remoteRef.imapUid = "1"; // faked imapUid because updateById needs it

    message.conversationRef = { key: message.key, internalId: 1 }; // faked conversationKey because it's necessary
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

    handleIdentificationFields(message, previousMessage);

    return message;
}

/**
 * Handle identification fields, as described in RFC-5322.
 * @see https://tools.ietf.org/html/rfc5322#section-3.6.4
 */
function handleIdentificationFields(message, previousMessage) {
    const references =
        extractHeaderValues(previousMessage, MessageHeader.REFERENCES) ||
        extractHeaderValues(previousMessage, MessageHeader.IN_REPLY_TO) ||
        [];

    if (previousMessage.messageId) {
        const inReplyToHeader = {
            name: MessageHeader.IN_REPLY_TO,
            values: [previousMessage.messageId]
        };
        message.headers.push(inReplyToHeader);
        references.push(previousMessage.messageId);
    }

    if (references.length) {
        const referencesHeader = {
            name: MessageHeader.REFERENCES,
            values: [references.join(" ")]
        };
        message.headers.push(referencesHeader);
    }
}

export function createFromDraft(previous, folder) {
    const message = createEmpty(folder, {});
    message.from = { ...previous.from };
    message.to = previous.to.slice();
    message.cc = previous.cc.slice();
    message.bcc = previous.bcc.slice();
    message.subject = previous.subject;
    message.attachments = previous.attachments.slice();
    message.hasAttacment = previous.hasAttacment;
    message.inlinePartsByCapabilities = previous.inlinePartsByCapabilities.slice();
    message.preview = previous.preview;
    message.composing = true;
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
    if (!previousMessage.subject) {
        return subjectPrefix;
    }
    // avoid subject prefix repetitions (like "Re: Re: Re: Re: My Subject")
    if (subjectPrefix !== previousMessage.subject.substring(0, subjectPrefix.length)) {
        return subjectPrefix + previousMessage.subject;
    }
    return previousMessage.subject;
}

export function getEditorContent(userPrefTextOnly, parts, partsByMessageKey, userLang) {
    let content;
    if (userPrefTextOnly) {
        content = mergePartsForTextarea(parts, partsByMessageKey);
    } else {
        content = mergePartsForRichEditor(parts, partsByMessageKey, userLang);
    }
    return content;
}

// TODO move elsewhere
export function findReplyOrForwardContentNode(document) {
    return (
        document.querySelector('div[id="' + MessageReplyAttributeSeparator + '"]') ||
        document.querySelector('div[id="' + MessageForwardAttributeSeparator + '"]')
    );
}

// TODO move elsewhere
export function handleSeparator(content) {
    let collapsed,
        newContent = content;
    const doc = new DOMParser().parseFromString(content, "text/html");
    const separator = findReplyOrForwardContentNode(doc);

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

export function draftInfoHeader(message) {
    const draftInfoHeader = message.headers?.find(h => h.name === MessageHeader.X_BM_DRAFT_INFO);
    if (draftInfoHeader) {
        return JSON.parse(draftInfoHeader.values[0]);
    }
}

export function isEditorContentEmpty(content, userPrefTextOnly, signature) {
    const sanitized = removeSignature(content, userPrefTextOnly, signature);
    const fragment = createDocumentFragment(sanitized);
    return !fragment.firstElementChild.innerText.trim();
}
