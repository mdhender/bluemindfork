import { inject } from "@bluemind/inject";
import global from "@bluemind/global";
import UUIDGenerator from "@bluemind/uuid";
import { create, AttachmentStatus } from "~/model/attachment";
import { createFromFile as createPartFromFile } from "~/model/part";

export default {
    async ADD_FH_ATTACHMENT({ commit }, { file, message }) {
        const attachment = createFhAttachment(file, message);

        commit("ADD_ATTACHMENT", { messageKey: message.key, attachment });
        commit("SET_ATTACHMENT_STATUS", {
            messageKey: message.key,
            address: attachment.address,
            status: AttachmentStatus.NOT_UPLOADED
        });
        commit("SET_MESSAGE_HAS_ATTACHMENT", { key: message.key, hasAttachment: true });
        global.cancellers = global.cancellers || {};
        global.cancellers[attachment.address + message.key] = { cancel: undefined };

        const serviceMbItems = inject("MailboxItemsPersistence", message.folderRef.uid);
        try {
            const address = await serviceMbItems.uploadPart("");

            const serviceAttachment = inject("AttachmentPersistence");
            const { publicUrl, name } = await serviceAttachment.share(
                file.name,
                file,
                global.cancellers[attachment.address + message.key],
                createOnUploadProgress(commit, message.key, attachment.address)
            );
            // TODO handle expiration date

            commit("SET_ATTACHMENT_HEADERS", {
                messageKey: message.key,
                address: attachment.address,
                headers: [
                    {
                        name: "X-Mozilla-Cloud-Part",
                        values: [`cloudFile;url=${publicUrl};name=${name}`]
                    },
                    {
                        name: "X-BM-Disposition",
                        values: [`filehosting;url=${publicUrl};name=${name};size=${file.size};mime=${file.type}`]
                    }
                ]
            });

            commit("SET_ATTACHMENT_ADDRESS", {
                messageKey: message.key,
                oldAddress: attachment.address,
                address
            });
            commit("SET_ATTACHMENT_STATUS", {
                messageKey: message.key,
                address,
                status: AttachmentStatus.UPLOADED
            });
        } catch (event) {
            const error = event.target && event.target.error ? event.target.error : event;
            handleError(commit, message, error, attachment);
        }
    }
};

function createFhAttachment(file) {
    const attachmentFromFile = createPartFromFile(UUIDGenerator.generate(), {
        name: file.name,
        size: 0
    });
    const attachment = create(
        {
            ...attachmentFromFile,
            headers: [
                {
                    name: "X-BM-Disposition",
                    values: [`filehosting;name=${file.name};size=${file.size};mime=${file.type}`]
                }
            ]
        },
        AttachmentStatus.NOT_LOADED
    );
    return attachment;
}

function createOnUploadProgress(commit, messageKey, address) {
    return progress => {
        commit("SET_ATTACHMENT_PROGRESS", {
            messageKey,
            address,
            loaded: progress.loaded,
            total: progress.total
        });
    };
}

function handleError(commit, message, error, attachment) {
    if (error.message === "CANCELLED_BY_CLIENT") {
        commit("REMOVE_ATTACHMENT", { messageKey: message.key, address: attachment.address });
        commit("SET_MESSAGE_HAS_ATTACHMENT", {
            key: message.key,
            hasAttachment: message.attachments.length > 0
        });
    } else {
        commit("SET_ATTACHMENT_PROGRESS", {
            messageKey: message.key,
            address: attachment.address,
            loaded: 100,
            total: 100
        });
        commit("SET_ATTACHMENT_STATUS", {
            messageKey: message.key,
            address: attachment.address,
            status: AttachmentStatus.ERROR
        });
    }
}
