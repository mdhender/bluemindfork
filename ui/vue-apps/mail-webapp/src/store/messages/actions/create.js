import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

import apiMessages from "../../api/apiMessages";
import mutationTypes from "../../mutationTypes";
import MessageAdaptor from "../helpers/MessageAdaptor";
import MessageBuilder from "../helpers/MessageBuilder";
import PartsHelper from "../helpers/PartsHelper";
import { MessageCreationModes, MessageHeader } from "../../../model/message";
import { uploadAttachmentHeaders } from "../../../model/attachment";

export default async function ({ commit, state }, { myDraftsFolder, creationMode, previousMessageKey = null }) {
    const userPrefTextOnly = false; // FIXME with userSettings state
    const service = inject("MailboxItemsPersistence", myDraftsFolder.remoteRef.uid);
    const previousMessage = state[previousMessageKey];

    const inlinePartAddresses = await uploadInlineParts(creationMode, previousMessage, service, userPrefTextOnly);
    let attachments = [];
    if (creationMode === MessageCreationModes.FORWARD && previousMessage.attachments.length > 0) {
        attachments = await uploadAttachments(creationMode, previousMessage, service);
    }

    const structure = MessageBuilder.createDraftStructure(
        { attachments, multipartAddresses: [] },
        userPrefTextOnly,
        inlinePartAddresses
    );

    const userSession = inject("UserSession");
    let message = MessageAdaptor.createWithMetadata(myDraftsFolder, userSession, structure);
    adaptDraftMetadata(creationMode, message, previousMessage, userSession);

    message.remoteRef.internalId = (await service.create(MessageAdaptor.realToMailboxItem(message, structure))).id;
    PartsHelper.clean(
        inlinePartAddresses,
        attachments.map(({ address }) => address),
        service
    );

    const newMessage = (await apiMessages.multipleById([message]))[0];
    newMessage.composing = true;
    // FIXME: default subject set by server ?
    if (newMessage.subject === " ") {
        newMessage.subject = "";
    }

    commit(mutationTypes.ADD_MESSAGES, [newMessage]);
    return newMessage.key;
}

async function uploadInlineParts(creationMode, previousMessage, service, userPrefTextOnly) {
    const partAddresses = {
        [MimeType.TEXT_PLAIN]: [],
        [MimeType.TEXT_HTML]: []
    };
    let text = "";
    if (creationMode !== MessageCreationModes.NEW) {
        text = await MessageBuilder.getTextFromStructure(previousMessage);
        text = MessageBuilder.addSeparator(text, previousMessage, creationMode, MimeType.TEXT_PLAIN);
        text = MessageBuilder.sanitizeForCyrus(text);
    }
    partAddresses[MimeType.TEXT_PLAIN].push(await service.uploadPart(text));
    if (!userPrefTextOnly) {
        let html = "";
        if (creationMode !== MessageCreationModes.NEW) {
            html = (await MessageBuilder.getHtmlFromStructure(previousMessage)).html;
            html = MessageBuilder.addSeparator(html, previousMessage, creationMode, MimeType.TEXT_HTML);
            html = MessageBuilder.sanitizeForCyrus(html);
        }
        partAddresses[MimeType.TEXT_HTML].push(await service.uploadPart(html));
    }
    return partAddresses;
}

async function uploadAttachments(creationMode, previousMessage, service) {
    const attachments = [];
    for (const attachment of previousMessage.attachments) {
        const stream = await PartsHelper.fetch(
            previousMessage.remoteRef.imapUid,
            previousMessage.folderRef.uid,
            attachment,
            true
        );
        const address = await service.uploadPart(stream);
        attachments.push({
            ...attachment,
            address,
            headers: uploadAttachmentHeaders(attachment.filename, attachment.size)
        });
    }
    return attachments;
}

function adaptDraftMetadata(creationMode, message, previousMessage, userSession) {
    if (creationMode !== MessageCreationModes.NEW) {
        const draftInfoHeader = [
            creationMode,
            previousMessage.remoteRef.internalId,
            previousMessage.folderRef.uid
        ].join();
        message.headers = [{ name: MessageHeader.X_BM_DRAFT_INFO, values: [draftInfoHeader] }];
        MessageBuilder.addRecipients(
            message,
            creationMode,
            previousMessage,
            userSession.defaultEmail,
            userSession.formatedName
        );
        message.subject = MessageBuilder.addSubject(message, creationMode, previousMessage);

        if (previousMessage.messageId) {
            message.headers.push({ name: MessageHeader.IN_REPLY_TO, values: [previousMessage.messageId] });
            message.references = [previousMessage.messageId].concat(previousMessage.references);
        } else {
            message.references = previousMessage.references;
        }
    }
}
