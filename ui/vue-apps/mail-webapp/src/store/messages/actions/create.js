import merge from "lodash.merge";

import { inject } from "@bluemind/inject";

import mutationTypes from "../../mutationTypes";
import MessageAdaptor from "../helpers/MessageAdaptor";
import { MessageCreationModes, createOnlyMetadata, createWithMetadata, clean } from "../../../model/message";
import { adaptDraft, uploadAttachments, uploadInlineParts, createDraftStructure } from "../../../model/draft";

export default async function ({ commit, state }, { myDraftsFolder, creationMode, previousMessageKey = null }) {
    const userPrefTextOnly = false; // FIXME with userSettings state
    const service = inject("MailboxItemsPersistence", myDraftsFolder.remoteRef.uid);
    const previousMessage = state[previousMessageKey];

    const { partContentByMimeType, inlinePartAddresses, inlineImageParts } = await uploadInlineParts(
        creationMode,
        previousMessage,
        service,
        userPrefTextOnly,
        inject("i18n")
    );
    let attachments = [];
    if (creationMode === MessageCreationModes.FORWARD && previousMessage.attachments.length > 0) {
        attachments = await uploadAttachments(previousMessage, service);
    }

    const structure = createDraftStructure(attachments, userPrefTextOnly, inlinePartAddresses, inlineImageParts);

    const metadata = { internalId: null, folder: { key: myDraftsFolder.key, uid: myDraftsFolder.remoteRef.uid } };
    const messageForCreate = merge(
        createWithMetadata(metadata),
        adaptDraft(creationMode, previousMessage, inject("UserSession"))
    );
    metadata.internalId = (await service.create(MessageAdaptor.realToMailboxItem(messageForCreate, structure))).id;
    messageForCreate.remoteRef.internalId = metadata.internalId;

    const messageInState = merge(
        messageForCreate,
        MessageAdaptor.computeParts(structure),
        createOnlyMetadata(metadata)
    );
    messageInState.partContentByMimeType = partContentByMimeType;
    messageInState.inlineImageParts = inlineImageParts;
    messageInState.remoteRef.imapUid = "1"; // fake imapUid, needed for updateById
    commit(mutationTypes.ADD_MESSAGES, [messageInState]);

    clean(inlinePartAddresses, attachments, service);

    return messageInState.key;
}
