import { EmailExtractor, EmailValidator, Flag, MimeType, PartsBuilder, InlineImageHelper } from "@bluemind/email";
import { html2text, sanitizeHtml } from "@bluemind/html-utils";

import {
    MessageCreationModes,
    MessageHeader,
    MessageForwardAttributeSeparator,
    MessageReplyAttributeSeparator,
    fetch
} from "./message";
import { create as createAttachment } from "./attachment";
import { setAddresses } from "./part";

// FIXME: must remove this import, model must depend only of other models and commons packages
import PlayWithInlinePartsByCapabilities from "../store/messages/helpers/PlayWithInlinePartsByCapabilities";
import { removeSignatureIds } from "./signature";

export function adaptDraft(creationMode, previousMessage, userSession) {
    let draft = {
        date: new Date(),
        from: {
            address: userSession.defaultEmail,
            dn: userSession.formatedName
        },
        flags: [Flag.SEEN],
        composing: true
    };

    if (creationMode !== MessageCreationModes.NEW) {
        draft = adaptDraftForReplyOrForward(draft, creationMode, previousMessage, userSession);
    }

    return draft;
}

function adaptDraftForReplyOrForward(draft, creationMode, previousMessage, userSession) {
    const draftInfoHeader = {
        type: creationMode,
        messageInternalId: previousMessage.remoteRef.internalId,
        folderUid: previousMessage.folderRef.uid
    };
    const adaptedForReplyOrForward = {
        headers: [{ name: MessageHeader.X_BM_DRAFT_INFO, values: [JSON.stringify(draftInfoHeader)] }],
        ...buildRecipients(creationMode, previousMessage, userSession.defaultEmail, userSession.formatedName),
        subject: buildSubject(creationMode, previousMessage)
    };
    if (previousMessage.messageId) {
        adaptedForReplyOrForward.headers.push({
            name: MessageHeader.IN_REPLY_TO,
            values: [previousMessage.messageId]
        });
        adaptedForReplyOrForward.references = [previousMessage.messageId].concat(previousMessage.references);
    } else {
        adaptedForReplyOrForward.references = previousMessage.references;
    }
    return { ...draft, ...adaptedForReplyOrForward };
}

/**
 * Compute the subject in function of creationMode (like "Re: My Subject" when replying).
 * INTERNAL METHOD (exported only for testing purpose)
 */
export function buildSubject(creationMode, previousMessage) {
    const subjectPrefix = creationMode === MessageCreationModes.FORWARD ? "Fw: " : "Re: ";
    // avoid subject prefix repetitions (like "Re: Re: Re: Re: My Subject")
    if (subjectPrefix !== previousMessage.subject.substring(0, subjectPrefix.length)) {
        return subjectPrefix + previousMessage.subject;
    }
    return previousMessage.subject;
}

/**
 * INTERNAL METHOD (exported only for testing purpose)
 */
export function buildRecipients(creationMode, previousMessage, myEmail, myName) {
    let cc = [],
        to = [];

    if (creationMode !== MessageCreationModes.FORWARD) {
        const isReplyAll = creationMode === MessageCreationModes.REPLY_ALL;
        const mailFollowUpTo = previousMessage.headers.find(header => header.name === MessageHeader.MAIL_FOLLOWUP_TO);

        cc = isReplyAll && !mailFollowUpTo ? previousMessage.cc.slice(0) : [];

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
    }
    return { to, cc };
}

function extractAddressesFromHeader(header, isReplyAll) {
    if (isReplyAll) {
        return header.values.map(value => ({ address: EmailExtractor.extractEmail(value), dn: "" }));
    } else {
        return [{ address: EmailExtractor.extractEmail(header.values[0]), dn: "" }];
    }
}

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

export async function uploadInlineParts(creationMode, previousMessage, service, userPrefTextOnly, vueI18n) {
    const partContentByMimeType = { [MimeType.IMAGE]: [] },
        inlineImageParts = [];
    const inlinePartAddresses = { [MimeType.TEXT_PLAIN]: [], [MimeType.TEXT_HTML]: [] };
    let text = "",
        html = "";
    if (creationMode !== MessageCreationModes.NEW) {
        text = await PlayWithInlinePartsByCapabilities.getTextFromStructure(previousMessage);
        text = addSeparator(text, previousMessage, creationMode, MimeType.TEXT_PLAIN, vueI18n);
        text = sanitizeForCyrus(text);
    }
    const textAddress = await service.uploadPart(text);
    partContentByMimeType[MimeType.TEXT_PLAIN] = text;
    inlinePartAddresses[MimeType.TEXT_PLAIN].push(textAddress);
    if (!userPrefTextOnly) {
        if (creationMode !== MessageCreationModes.NEW) {
            const result = await PlayWithInlinePartsByCapabilities.getHtmlFromStructure(previousMessage);
            html = result.html;
            await Promise.all(
                result.inlineImageParts.map(async (part, index) => {
                    const partContent = result.inlineImagePartsContent[index];
                    partContentByMimeType[MimeType.IMAGE].push(partContent);
                    part.address = await service.uploadPart(partContent);
                    inlineImageParts.push(part);
                    return Promise.resolve();
                })
            );
            html = addSeparator(html, previousMessage, creationMode, MimeType.TEXT_HTML, vueI18n);
            html = sanitizeForCyrus(html);
        }
        const htmlAddress = await service.uploadPart(html);
        partContentByMimeType[MimeType.TEXT_HTML] = html;
        inlinePartAddresses[MimeType.TEXT_HTML].push(htmlAddress);
    }
    return { partContentByMimeType, inlinePartAddresses, inlineImageParts };
}

export async function uploadAttachments(previousMessage, service) {
    const attachments = [];
    for (const attachment of previousMessage.attachments) {
        const stream = await fetch(previousMessage.remoteRef.imapUid, service, attachment, true);
        const address = await service.uploadPart(stream);
        attachments.push(
            createAttachment(
                address,
                attachment.charset,
                attachment.fileName,
                attachment.encoding,
                attachment.mime,
                attachment.size,
                true
            )
        );
    }
    return attachments;
}

export function sanitizeForCyrus(text) {
    return text.replace(/\r?\n/g, "\r\n");
}

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
function addSeparator(inlinePartContent, previousMessage, creationMode, expectedMimeType, vueI18n) {
    let content = inlinePartContent;
    if (creationMode === MessageCreationModes.REPLY || creationMode === MessageCreationModes.REPLY_ALL) {
        content = adaptPreviousMessageForReply(expectedMimeType, inlinePartContent);
    }
    const lineBreakSeparator = expectedMimeType === MimeType.TEXT_PLAIN ? "\n" : "<br>";
    const separator =
        creationMode === MessageCreationModes.FORWARD
            ? buildSeparatorForForward(previousMessage, lineBreakSeparator, vueI18n)
            : buildSeparatorForReply(previousMessage, vueI18n);

    content = separator + content;

    if (expectedMimeType === MimeType.TEXT_HTML) {
        const attribute = MessageCreationModes.FORWARD
            ? MessageForwardAttributeSeparator
            : MessageReplyAttributeSeparator;
        content = "<div " + attribute + ">" + removeSignatureIds(content) + "</div>";
    }
    return content;
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

function adaptPreviousMessageForReply(expectedMimeType, content) {
    if (MimeType.equals(expectedMimeType, MimeType.TEXT_PLAIN)) {
        return (
            "\n\n" +
            content
                .split("\n")
                .map(line => "> " + line)
                .join("\n")
        );
    } else if (MimeType.equals(expectedMimeType, MimeType.TEXT_HTML)) {
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

export function prepareDraft(draft, messageCompose, userPrefTextOnly) {
    const partsToUpload = {};
    let inlineImages = [];
    const editorContent = prepareEditorContent(messageCompose);

    if (userPrefTextOnly) {
        partsToUpload[MimeType.TEXT_PLAIN] = [editorContent];
    } else {
        const previousInlineImages = draft.inlinePartsByCapabilities
            .find(byCapabilities => byCapabilities.capabilities[0] === MimeType.TEXT_HTML)
            .parts.filter(part => part.dispositionType === "INLINE" && part.mime.startsWith(MimeType.IMAGE));
        const insertCidsResults = InlineImageHelper.insertCid(editorContent, previousInlineImages);
        inlineImages = insertCidsResults.inlineImages;
        // if image is not referenced in one html part and is not a new one, ignore it
        inlineImages = inlineImages.filter(
            part => !part.address || insertCidsResults.alreadyUploaded.includes(part.address)
        );
        const inlineImagesToUpload = inlineImages.filter(part => !part.address);

        const html = insertCidsResults.html;
        partsToUpload[MimeType.TEXT_HTML] = [html];
        partsToUpload[MimeType.TEXT_PLAIN] = [html2text(html).replace(/\r?\n/g, "\r\n")];
        partsToUpload[MimeType.IMAGE] = inlineImagesToUpload.map(part => insertCidsResults.streamByCid[part.contentId]);
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
