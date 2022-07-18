import { inject } from "@bluemind/inject";
import global from "@bluemind/global";
import UUIDGenerator from "@bluemind/uuid";
import { partUtils, attachmentUtils, fileUtils } from "@bluemind/mail";
const { create, AttachmentAdaptor } = attachmentUtils;
const { createFromFile: createPartFromFile } = partUtils;
const { FileStatus } = fileUtils;

export default {
    async ADD_FH_ATTACHMENT({ commit }, { file, message }) {
        const attachment = createFhAttachment(file, message);
        const { files, attachments } = AttachmentAdaptor.extractFiles([attachment]);
        const adaptedFile = files[0];
        const adaptedAttachment = attachments[0];

        commit("ADD_FILE", { file: adaptedFile });
        commit("ADD_ATTACHMENT", { messageKey: message.key, attachment: adaptedAttachment });
        commit("SET_FILE_STATUS", {
            key: adaptedAttachment.fileKey,
            status: FileStatus.NOT_LOADED
        });
        commit("SET_MESSAGE_HAS_ATTACHMENT", { key: message.key, hasAttachment: true });

        global.cancellers = global.cancellers || {};
        global.cancellers[adaptedFile.key] = { cancel: undefined };

        const serviceMbItems = inject("MailboxItemsPersistence", message.folderRef.uid);
        try {
            const address = await serviceMbItems.uploadPart("");

            const serviceAttachment = inject("AttachmentPersistence");
            const shareInfos = await serviceAttachment.share(
                adaptedFile.fileName,
                file,
                global.cancellers[adaptedFile.key],
                createOnUploadProgress(commit, adaptedAttachment.fileKey)
            );

            const mozillaHeader = getMozillaHeader(shareInfos);
            const bmHeader = getBmHeader(shareInfos, file);

            commit("SET_FILE_HEADERS", {
                key: adaptedFile.key,
                headers: [
                    { name: "X-Mozilla-Cloud-Part", values: [mozillaHeader] },
                    { name: "X-BM-Disposition", values: [bmHeader] }
                ]
            });

            commit("SET_ATTACHMENT_ADDRESS", {
                messageKey: message.key,
                oldAddress: adaptedAttachment.address,
                address
            });
            commit("SET_FILE_ADDRESS", {
                key: adaptedFile.key,
                address
            });
            commit("SET_FILE_STATUS", {
                key: adaptedFile.key,
                status: FileStatus.UPLOADED
            });
        } catch (event) {
            const error = event.target && event.target.error ? event.target.error : event;
            handleError(commit, message, error, adaptedAttachment);
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
        FileStatus.NOT_LOADED
    );
    return attachment;
}

function createOnUploadProgress(commit, fileKey) {
    return progress => {
        commit("SET_FILE_PROGRESS", {
            key: fileKey,
            loaded: progress.loaded,
            total: progress.total
        });
    };
}

function handleError(commit, message, error, attachment) {
    if (error.message === "CANCELLED_BY_CLIENT") {
        commit("REMOVE_ATTACHMENT", { messageKey: message.key, address: attachment.address });
        commit("REMOVE_FILE", { key: attachment.fileKey });
        commit("SET_MESSAGE_HAS_ATTACHMENT", {
            key: message.key,
            hasAttachment: message.attachments.length > 0
        });
    } else {
        commit("SET_FILE_PROGRESS", {
            key: attachment.fileKey,
            loaded: 100,
            total: 100
        });
        commit("SET_FILE_STATUS", {
            key: attachment.fileKey,
            status: FileStatus.ERROR
        });
    }
}

function getBmHeader({ publicUrl, name, expirationDate }, file) {
    let bmHeader = `filehosting;url=${publicUrl}`;
    bmHeader += name ? `;name=${name}` : `;name=${inject("i18n").t("mail.viewer.no.name")}`;
    bmHeader += file.size ? `;size=${file.size}` : ";size=0";
    bmHeader += file.type ? `;mime=${file.type}` : ";mime=application/octet-stream";
    bmHeader += expirationDate ? `;expirationDate=${expirationDate}` : "";
    return bmHeader;
}

function getMozillaHeader({ publicUrl, name }) {
    let mozillaHeader = `cloudFile;url=${publicUrl}`;
    mozillaHeader += name ? `;name=${name}` : `;name=${inject("i18n").t("mail.viewer.no.name")}`;
    return mozillaHeader;
}
