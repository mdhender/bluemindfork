import { inject } from "@bluemind/inject";
import { fileUtils } from "@bluemind/mail";

const { FileStatus } = fileUtils;

export default async function addFhFile({ commit }, { file, message, content, shareFn }) {
    const fhFile = {
        ...file,
        size: 0
    };
    commit("ADD_FILE", { file: fhFile });
    commit("SET_FILE_STATUS", {
        key: fhFile.key,
        status: FileStatus.NOT_LOADED
    });
    try {
        const serviceMbItems = inject("MailboxItemsPersistence", message.folderRef.uid);
        const address = await serviceMbItems.uploadPart("");
        const shareInfos = await shareFn(fhFile, content, commit);
        const headers = [
            { name: "X-Mozilla-Cloud-Part", values: [getMozillaHeader(shareInfos)] },
            { name: "X-BM-Disposition", values: [getBmHeader(shareInfos, content)] }
        ];

        commit("SET_FILE_HEADERS", {
            key: fhFile.key,
            headers
        });

        commit("SET_FILE_ADDRESS", {
            key: fhFile.key,
            address
        });

        const status = FileStatus.UPLOADED;
        commit("SET_FILE_STATUS", {
            key: fhFile.key,
            status
        });
        return { ...fhFile, ...shareInfos, address, headers };
    } catch (event) {
        const error = event.target && event.target.error ? event.target.error : event;
        handleError(commit, message, error, file);
    }
}

function handleError(commit, message, error, file) {
    if (error.message === "CANCELLED_BY_CLIENT") {
        commit("REMOVE_ATTACHMENT", { messageKey: message.key, address: file.address });
        commit("REMOVE_FILE", { key: file.key });
        commit("SET_MESSAGE_HAS_ATTACHMENT", {
            key: message.key,
            hasAttachment: message.attachments.length > 0
        });
    } else {
        commit("SET_FILE_PROGRESS", {
            key: file.key,
            loaded: 100,
            total: 100
        });
        commit("SET_FILE_STATUS", {
            key: file.key,
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
