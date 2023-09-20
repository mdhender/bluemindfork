import escape from "lodash.escape";
import { EmailExtractor, Flag, MimeType } from "@bluemind/email";
import i18n from "@bluemind/i18n";

import { createDocumentFragment } from "@bluemind/html-utils";

import { LoadingStatus } from "./loading-status";
import {
    extractHeaderValues,
    MessageHeader,
    createWithMetadata as createMessage,
    MessageCreationModes,
    MessageForwardAttributeSeparator,
    MessageReplyAttributeSeparator,
    MessageStatus,
    messageKey
} from "./message";
import { mergePartsForRichEditor, mergePartsForTextarea } from "./part";
import { removeSignature, removeSignatureAttr } from "./signature";

export const TEMPORARY_MESSAGE_ID = 0;
let DRAFT_HASH = 0;
let LAST_GENERATED_NEW_MESSAGE_KEY;

export function isNewMessage({ remoteRef: { internalId } }) {
    return internalId === TEMPORARY_MESSAGE_ID;
}

export function draftKey(myDrafts) {
    return messageKey(DRAFT_HASH, myDrafts.key);
}

// FIXME remove once we use 'real' message ids for new message
export function getLastGeneratedNewMessageKey() {
    return LAST_GENERATED_NEW_MESSAGE_KEY;
}

export function createEmpty(folder) {
    const metadata = {
        internalId: TEMPORARY_MESSAGE_ID,
        folder: { key: folder.key, uid: folder.remoteRef.uid }
    };
    const message = createMessage(metadata);
    LAST_GENERATED_NEW_MESSAGE_KEY = messageKey(--DRAFT_HASH, folder.key);
    message.key = LAST_GENERATED_NEW_MESSAGE_KEY;

    message.date = new Date();
    message.flags = [Flag.SEEN];
    message.status = MessageStatus.NEW;
    message.loading = LoadingStatus.LOADED;
    message.composing = true;
    message.remoteRef.imapUid = "1"; // faked imapUid because updateById needs it

    message.conversationRef = { key: message.key, internalId: 1 }; // faked conversationKey because it's necessary
    return message;
}

/**
 * Handle identification fields, as described in RFC-5322.
 * @see https://tools.ietf.org/html/rfc5322#section-3.6.4
 */
function handleIdentificationFields(previousMessage) {
    const headers = [];
    const references =
        extractHeaderValues(previousMessage, MessageHeader.REFERENCES) ||
        extractHeaderValues(previousMessage, MessageHeader.IN_REPLY_TO) ||
        [];

    if (previousMessage.messageId) {
        const inReplyToHeader = {
            name: MessageHeader.IN_REPLY_TO,
            values: [previousMessage.messageId]
        };
        headers.push(inReplyToHeader);
        references.push(previousMessage.messageId);
    }

    if (references.length) {
        const referencesHeader = {
            name: MessageHeader.REFERENCES,
            values: [references.join(" ")]
        };
        headers.push(referencesHeader);
    }
    return headers;
}

export function createFromDraft(previous, folder) {
    const message = createEmpty(folder);
    message.from = { ...previous.from };
    message.to = previous.to.slice();
    message.cc = previous.cc.slice();
    message.bcc = previous.bcc.slice();
    message.subject = previous.subject;
    message.preview = previous.preview;
    message.composing = true;
    return message;
}

export function computeIdentityForReplyOrForward(message, identities, currentMailbox, defaultIdentity) {
    let chosenIdentity;
    const identitiesFoundInTo = findIdentities(message.to, identities);
    const identitiesFoundInCc = findIdentities(message.cc, identities);
    const identitiesFound = identitiesFoundInTo.concat(identitiesFoundInCc);
    if (identitiesFound.length === 0) {
        chosenIdentity = findIdentityFromMailbox(currentMailbox, identities, defaultIdentity);
    } else if (identitiesFound.length === 1) {
        chosenIdentity = identitiesFound[0];
    } else {
        // multiple identities matching recipients
        if (identitiesFound.find(id => !!id.isDefault)) {
            chosenIdentity = defaultIdentity;
        } else if (identitiesFoundInTo.length > 0) {
            chosenIdentity = identitiesFoundInTo[0];
        } else {
            chosenIdentity = identitiesFound[0];
        }
    }
    return chosenIdentity;
}

export function findIdentityFromMailbox(currentMailbox, identities, defaultIdentity) {
    const matchingId = findIdentities([currentMailbox], identities);
    if (matchingId[0]) {
        return matchingId[0];
    }
    return defaultIdentity;
}

function identityToRecipient(identity) {
    return { address: identity.email, dn: identity.displayname };
}

export function preserveFromOrDefault(previous, identities, defaultIdentity) {
    const matchingIdentities = findIdentities([previous], identities);
    if (!matchingIdentities[0]) {
        return identityToRecipient(defaultIdentity);
    }
    return identityToRecipient(matchingIdentities[0]);
}

// items must have both { address, dn } properties
function findIdentities(items, identities) {
    let matchingIdentities = [];
    items.forEach(({ address, dn }) => {
        const exactMatch = identities.find(id => id.displayname === dn && id.email === address);
        if (exactMatch) {
            matchingIdentities.push(exactMatch);
        } else {
            const approximateMatch = identities.find(id => id.email === address);
            if (approximateMatch) {
                matchingIdentities.push(approximateMatch);
            }
        }
    });
    return matchingIdentities;
}

// INTERNAL METHOD (exported only for testing purpose)
export function computeCcRecipients(creationMode, previousMessage, senderIdentity) {
    let cc = [];
    const mailFollowUpTo = previousMessage.headers.find(header => header.name === MessageHeader.MAIL_FOLLOWUP_TO);
    if (creationMode === MessageCreationModes.REPLY_ALL && !mailFollowUpTo) {
        const myself = identityToRecipient(senderIdentity);
        // keep previous cc except myself
        cc = previousMessage.cc.filter(recipient => recipient.address !== myself.address);
        cc = removeDuplicatedContacts(cc);
    }
    return cc;
}

// INTERNAL METHOD (exported only for testing purpose)
export function computeToRecipients(creationMode, previousMessage, senderIdentity) {
    let recipients;

    const myself = identityToRecipient(senderIdentity);
    const isReplyAll = creationMode === MessageCreationModes.REPLY_ALL;
    const mailFollowUpTo = previousMessage.headers.find(header => header.name === MessageHeader.MAIL_FOLLOWUP_TO);
    const mailReplyToHeader = previousMessage.headers.find(header => header.name === MessageHeader.MAIL_REPLY_TO);
    const replyToHeader = previousMessage.headers.find(header => header.name === MessageHeader.REPLY_TO);

    if (isReplyAll && mailFollowUpTo) {
        recipients = extractRecipientsFromHeader(mailFollowUpTo, true);
    } else if (mailReplyToHeader) {
        recipients = extractRecipientsFromHeader(mailReplyToHeader, isReplyAll);
        if (isReplyAll) {
            recipients = [...recipients, ...previousMessage.to];
        }
    } else if (replyToHeader) {
        recipients = extractRecipientsFromHeader(replyToHeader, isReplyAll);
        if (isReplyAll) {
            recipients = [...recipients, ...previousMessage.to];
        }
    } else {
        // compute recipients from "From" or "To"
        recipients = [previousMessage.from];
        if (isReplyAll) {
            // respond to sender and all recipients except myself
            recipients.push(...previousMessage.to);
            recipients = recipients.filter(r => r.address !== myself.address);
            recipients = removeDuplicatedContacts(recipients);
            if (recipients.length === 0) {
                // I was alone, respond to myself then
                recipients = [{ ...myself }];
            }
        } else if (recipients.map(r => r.address).includes(myself.address)) {
            // all recipients except myself
            recipients = previousMessage.to.filter(r => r.address !== myself.address);
            if (recipients.length === 0) {
                // I was alone, respond to myself then
                recipients = [{ ...myself }];
            } else {
                // respond to the first "not me" recipient only
                recipients = [recipients[0]];
            }
        }
    }
    return recipients.filter(Boolean);
}

export function removeDuplicatedContacts(contacts) {
    const result = [],
        addresses = [];
    for (const contact of contacts) {
        if (!addresses.includes(contact.address)) {
            addresses.push(contact.address);
            result.push(contact);
        }
    }
    return result;
}

export function computeSubject(creationMode, previousMessage) {
    const subjectPrefix = creationMode === MessageCreationModes.FORWARD ? "Fw: " : "Re: ";
    if (!previousMessage.subject) {
        return `${subjectPrefix} ${i18n.t("mail.viewer.no.subject")}`;
    }
    // avoid subject prefix repetitions (like "Re: Re: Re: Re: My Subject")
    if (subjectPrefix !== previousMessage.subject.substring(0, subjectPrefix.length)) {
        return subjectPrefix + previousMessage.subject;
    }
    return previousMessage.subject;
}

function extractRecipientsFromHeader(header, isReplyAll) {
    if (isReplyAll) {
        return header.values.map(value => ({
            address: EmailExtractor.extractEmail(value),
            dn: EmailExtractor.extractDN(value)
        }));
    } else {
        return [
            { address: EmailExtractor.extractEmail(header.values[0]), dn: EmailExtractor.extractDN(header.values[0]) }
        ];
    }
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
 * Build the text or html representing this message as a previous message.
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
export function quotePreviousMessage(previousMessageContent, previousMessage, creationMode, userPrefTextOnly) {
    let quotedContent = previousMessageContent;
    if (creationMode === MessageCreationModes.REPLY || creationMode === MessageCreationModes.REPLY_ALL) {
        quotedContent = quoteContent(userPrefTextOnly, previousMessageContent);
    }
    const header =
        creationMode === MessageCreationModes.FORWARD
            ? headerForForward(previousMessage, userPrefTextOnly)
            : headerForReply(previousMessage, userPrefTextOnly);

    let quote = header + quotedContent;

    if (!userPrefTextOnly) {
        const id = MessageCreationModes.FORWARD ? MessageForwardAttributeSeparator : MessageReplyAttributeSeparator;
        quote = `<div id="${id}">${removeSignatureAttr(quote)}</div>`;
    }
    return (userPrefTextOnly ? "\n" : "<br>") + quote;
}

/**
 *  A separator before the previous message (reply).
 */
function headerForReply(message, userPrefTextOnly) {
    const date = i18n.d(message.date, "full_date_time");
    const name = nameAndAddress(message.from, userPrefTextOnly);
    const replyInfo = i18n.t("mail.compose.reply.body", { date, name });
    return userPrefTextOnly ? `${replyInfo}\n` : `<p>${replyInfo}</p>`;
}

function quoteContent(userPrefTextOnly, content) {
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
            `<blockquote style="margin-left: 1rem; padding-left: 1rem; border-left: 2px solid black; border-left-color: var(--neutral-fg-lo1);">` +
            content +
            "</blockquote>"
        );
    }
}

/**
 *  A separator before the previous message (forward).
 */
function headerForForward(message, userPrefTextOnly) {
    const title = i18n.t("mail.compose.forward.prev.message.info.title");
    const fromLabel = i18n.t("mail.compose.forward.prev.message.info.from");
    const from = nameAndAddress(message.from, userPrefTextOnly);
    const subjectLabel = i18n.t("mail.compose.forward.prev.message.info.subject");
    const subject = message.subject?.trim();
    const noSubject = i18n.t("mail.viewer.no.subject");
    const toLabel = i18n.t("common.to");
    const to = message.to.map(to => nameAndAddress(to, userPrefTextOnly)).join(", ");
    const ccLabel = i18n.t("common.cc");
    const cc = message.cc?.map(cc => nameAndAddress(cc, userPrefTextOnly)).join(", ");
    const dateLabel = i18n.t("mail.compose.forward.prev.message.info.date");
    const date = i18n.d(message.date, "full_date_time");

    if (userPrefTextOnly) {
        let text = `${title}\n`;
        text += `${fromLabel}: ${from}\n`;
        text += `${subjectLabel}: ${subject ? subject : noSubject}\n`;
        text += to ? `${toLabel}: ${to}\n` : "";
        text += cc ? `${ccLabel}: ${cc}\n` : "";
        text += `${dateLabel}: ${date}`;
        return text;
    } else {
        const html = `<p style="color: purple;">
            ${title}<br>
            ${fromLabel}: ${from}<br>
            ${subjectLabel}: ${subject ? subject : "<i>" + noSubject + "</i>"}<br>
            ${to ? toLabel + ": " + to + "<br>" : ""}
            ${cc ? ccLabel + ": " + cc + "<br>" : ""}
            ${dateLabel}: ${date}
        </p>`;
        return html;
    }
}

/** @return like "John Doe <jdoe@bluemind.net>" */
function nameAndAddress(recipient, userPrefTextOnly) {
    const result = recipient.dn ? recipient.dn + " <" + recipient.address + ">" : recipient.address;
    return userPrefTextOnly ? result : escape(result);
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

export default {
    computeSubject,
    quotePreviousMessage,
    COMPOSER_CAPABILITIES,
    computeIdentityForReplyOrForward,
    computeToRecipients,
    createEmpty,
    createFromDraft,
    draftInfoHeader,
    draftKey,
    computeCcRecipients,
    findIdentityFromMailbox,
    findReplyOrForwardContentNode,
    getEditorContent,
    getLastGeneratedNewMessageKey,
    handleSeparator,
    handleIdentificationFields,
    isEditorContentEmpty,
    isNewMessage,
    preserveFromOrDefault,
    TEMPORARY_MESSAGE_ID
};
