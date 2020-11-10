import { EmailExtractor, EmailValidator, Flag, MimeType, PartsBuilder, InlineImageHelper } from "@bluemind/email";
import { html2text, sanitizeHtml } from "@bluemind/html-utils";

import {
    MessageHeader,
    // fetch,
    createWithMetadata as createMessage,
    MessageCreationModes,
    MessageForwardAttributeSeparator,
    MessageReplyAttributeSeparator
} from "./message";
// import { create as createAttachment } from "./attachment";
import { mergePartsForRichEditor, mergePartsForTextarea, setAddresses } from "./part";
import { removeSignatureIds } from "./signature";

export function createDraftStructure(attachments, userPrefTextOnly, inlinePartAddresses, inlineImages = []) {
    let structure;
    const textPart = PartsBuilder.createTextPart(inlinePartAddresses[MimeType.TEXT_PLAIN][0]);

    if (userPrefTextOnly) {
        structure = textPart;
    } else {
        const htmlPart = PartsBuilder.createHtmlPart(inlinePartAddresses[MimeType.TEXT_HTML][0]);
        structure = PartsBuilder.createAlternativePart(textPart, htmlPart);
        structure = PartsBuilder.createInlineImageParts(structure, inlineImages, inlinePartAddresses[MimeType.IMAGE]);
    }
    structure = PartsBuilder.createAttachmentParts(attachments, structure);

    setAddresses(structure);

    return structure;
}

// FIXME once attachments are forwarded
// export async function uploadAttachments(previousMessage, service) {
//     const attachments = [];
//     for (const attachment of previousMessage.attachments) {
//         const stream = await fetch(previousMessage.remoteRef.imapUid, service, attachment, true);
//         const address = await service.uploadPart(stream);
//         attachments.push(
//             createAttachment(
//                 address,
//                 attachment.charset,
//                 attachment.fileName,
//                 attachment.encoding,
//                 attachment.mime,
//                 attachment.size,
//                 true
//             )
//         );
//     }
//     return attachments;
// }

export function sanitizeForCyrus(text) {
    return text.replace(/\r?\n/g, "\r\n");
}

export function prepareDraft(draft, messageCompose, userPrefTextOnly) {
    const partsToUpload = {};
    let inlineImages = [];
    const editorContent = prepareEditorContent(messageCompose);

    if (userPrefTextOnly) {
        partsToUpload[MimeType.TEXT_PLAIN] = [editorContent];
    } else {
        // FIXME inline images
        // const previousInlineImages = draft.inlinePartsByCapabilities
        //     .find(byCapabilities => byCapabilities.capabilities[0] === MimeType.TEXT_HTML)
        //     .parts.filter(part => part.dispositionType === "INLINE" && part.mime.startsWith(MimeType.IMAGE));
        // const insertCidsResults = InlineImageHelper.insertCid(editorContent, previousInlineImages);
        // inlineImages = insertCidsResults.inlineImages;
        // // if image is not referenced in one html part and is not a new one, ignore it
        // inlineImages = inlineImages.filter(
        //     part => !part.address || insertCidsResults.alreadyUploaded.includes(part.address)
        // );
        // const inlineImagesToUpload = inlineImages.filter(part => !part.address);

        // const html = insertCidsResults.html;
        partsToUpload[MimeType.TEXT_HTML] = [editorContent];
        partsToUpload[MimeType.TEXT_PLAIN] = [html2text(editorContent).replace(/\r?\n/g, "\r\n")];
        partsToUpload[MimeType.IMAGE] = [];
        // partsToUpload[MimeType.IMAGE] = inlineImagesToUpload.map(part => insertCidsResults.streamByCid[part.contentId]);
    }
    return { partsToUpload, inlineImages };
}

function prepareEditorContent(messageCompose) {
    let editorContent = messageCompose.collapsedContent
        ? messageCompose.editorContent + messageCompose.collapsedContent
        : messageCompose.editorContent;
    editorContent = sanitizeHtml(editorContent);
    editorContent = sanitizeForCyrus(editorContent);
    return editorContent;
}

/**
 * Needed by BM core to detect if mail has changed when using IMailboxItems.updateById
 */
export function forceMailRewriteOnServer(draft) {
    const headers = JSON.parse(JSON.stringify(draft.headers));
    const saveDate = new Date();

    const hasXBmDraftKeyHeader = headers.find(header => header.name === MessageHeader.X_BM_DRAFT_REFRESH_DATE);
    if (hasXBmDraftKeyHeader) {
        hasXBmDraftKeyHeader.values = [saveDate.getTime()];
    } else {
        headers.push({
            name: MessageHeader.X_BM_DRAFT_REFRESH_DATE,
            values: [saveDate.getTime()]
        });
    }

    return { saveDate, headers };
}

export function validateDraft(draft, vueI18n) {
    let recipients = draft.to.concat(draft.cc).concat(draft.bcc);
    const allRecipientsAreValid = recipients.some(recipient => EmailValidator.validateAddress(recipient.address));
    if (!allRecipientsAreValid) {
        throw vueI18n.t("mail.error.email.address.invalid");
    }
}

export function createEmpty(myDraftsFolder, userSession) {
    const fakedInternalId = "faked-internal-id";
    const metadata = {
        internalId: fakedInternalId,
        folder: { key: myDraftsFolder.key, uid: myDraftsFolder.remoteRef.uid }
    };
    const message = createMessage(metadata);

    message.date = new Date();
    message.from = {
        address: userSession.defaultEmail,
        dn: userSession.formatedName
    };
    message.flags = [Flag.SEEN];
    message.composing = true;
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

export function getEditorContent(userPrefTextOnly, parts, message) {
    let content;
    if (userPrefTextOnly) {
        content = mergePartsForTextarea(message, parts);
    } else {
        content = mergePartsForRichEditor(message, parts);
        content = handleInlineImages(message, parts, content);
        content = sanitizeHtml(content);
    }
    return content;
}

function handleInlineImages(message, parts, html) {
    const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);

    const insertionResult = InlineImageHelper.insertInlineImages([html], partsWithCid, message.partContentByAddress);

    // FIXME !!
    // const blobsUrl = insertionResult.blobsUrl;

    return insertionResult.contentsWithBlob[0];
}

export function handleSeparator(content) {
    let collapsed,
        newContent = content;

    const doc = new DOMParser().parseFromString(content, "text/html");
    const separator =
        doc.querySelector("div[" + MessageReplyAttributeSeparator + "]") ||
        doc.querySelector("div[" + MessageForwardAttributeSeparator + "]");

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
 * * @example HTML
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
            : buildSeparatorForReply(previousMessage, vueI18n);

    newContent = separator + newContent;

    if (!userPrefTextOnly) {
        const attribute = MessageCreationModes.FORWARD
            ? MessageForwardAttributeSeparator
            : MessageReplyAttributeSeparator;
        newContent = "<p " + attribute + ">" + removeSignatureIds(newContent) + "</p>";
    }
    return newContent;
}

/**
 *  A separator before the previous message (reply).
 */
function buildSeparatorForReply(message, vueI18n) {
    return (
        "<p>" +
        vueI18n.t("mail.compose.reply.body", {
            date: vueI18n.d(message.date, "full_date_time"),
            name: nameAndAddress(message.from)
        }) +
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
            `<br>
            <blockquote style="margin-left: 1rem; padding-left: 1rem; border-left: 2px solid black;">` +
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
