import { inject } from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";
import { attachmentUtils, fileUtils, partUtils } from "@bluemind/mail";
const { createFromFile: createPartFromFile } = partUtils;
const { create, AttachmentAdaptor } = attachmentUtils;
const { FileStatus } = fileUtils;

export default async function addFhAttachment({ commit }, { file, message, shareFn }) {
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

    const serviceMbItems = inject("MailboxItemsPersistence", message.folderRef.uid);
    try {
        const address = await serviceMbItems.uploadPart("");
        const shareInfos = await shareFn(adaptedFile, file, commit);

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

function getBmHeader({ url, name, expirationDate }, file) {
    let bmHeader = `filehosting;url=${url}`;
    bmHeader += name ? `;name=${name}` : `;name=${inject("i18n").t("mail.viewer.no.name")}`;
    bmHeader += file.size ? `;size=${file.size}` : ";size=0";
    bmHeader += file.type ? `;mime=${file.type}` : ";mime=application/octet-stream";
    bmHeader += expirationDate ? `;expirationDate=${expirationDate}` : "";
    return bmHeader;
}

function getMozillaHeader({ url, name }) {
    let mozillaHeader = `cloudFile;url=${url}`;
    mozillaHeader += name ? `;name=${name}` : `;name=${inject("i18n").t("mail.viewer.no.name")}`;
    return mozillaHeader;
}

function createFhAttachment(file) {
    const attachmentFromFile = {
        ...file,
        ...createPartFromFile(UUIDGenerator.generate(), {
            name: file.name,
            size: 0
        })
    };
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
    return {
        ...attachment,
        mime: "application/octet-stream"
    };
}
