import injector from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";

const CLEAN_UP_DELAY = 1000;

export function addAttachments({ getters, commit, dispatch }, files) {
    if (files.length > 0) {
        let promises = [];
        for (let file of files) {
            promises.push(addAttachment({ getters, commit }, file));
        }
        return Promise.all(promises).then(() => dispatch("saveDraft"));
    }
}

/** Build a DTO representing an attachment. */
const buildAttachment = function(file) {
    return {
        content: file,
        uid: UUIDGenerator.generate(),
        mime: file.type || "application/octet-stream",
        filename: file.name,
        size: file.size,
        headers: [
            {
                name: "Content-Disposition",
                values: ["attachment;filename=" + file.name + ";size=" + file.size]
            },
            {
                name: "Content-Transfer-Encoding",
                values: ["base64"]
            }
        ]
    };
};

async function addAttachment({ getters, commit }, file) {
    const attachment = buildAttachment(file);
    const reader = new FileReader();

    // this will contain a function for cancelling the upload
    let canceller = { cancel: undefined };

    // this will make the attachment component appear in the UI
    commit("draft/addAttachment", attachment);

    // upload this attachment then save the draft
    return new Promise((resolve, reject) => {
        // here we bind the FileReader's callbacks to the Promise API
        reader.onload = resolve;
        reader.onerror = reject;
        reader.readAsArrayBuffer(file);
    })
        .then(() => {
            commit("draft/setAttachmentProgress", { attachmentUid: attachment.uid, loaded: 0, total: 100, canceller });
            const service = injector.getProvider("MailboxItemsPersistence").get(getters.my.DRAFTS.uid);
            return service.uploadPart(
                reader.result,
                canceller,
                createOnUploadProgress(commit, getters, attachment, canceller)
            );
        })
        .then(addrPart => {
            attachment.address = addrPart;
            commit("draft/updateAttachment", attachment);
        })
        .catch(event => {
            const error = event.target && event.target.error ? event.target.error : event;
            handleError(commit, error, attachment, canceller);
        });
}

function createOnUploadProgress(commit, getters, attachment, canceller) {
    return progress => {
        commit("draft/setAttachmentProgress", {
            attachmentUid: attachment.uid,
            loaded: progress.loaded,
            total: progress.total,
            canceller
        });
        if (progress.loaded === progress.total) {
            setTimeout(() => {
                if (getters["draft/getAttachmentStatus"](attachment.uid) !== "ERROR") {
                    commit("draft/removeAttachmentProgress", attachment.uid);
                }
            }, CLEAN_UP_DELAY);
        }
    };
}

function handleError(commit, error, attachment, canceller) {
    if (error.message === "CANCELLED_BY_CLIENT") {
        commit("draft/removeAttachment", attachment.uid);
    } else {
        commit("draft/setAttachmentProgress", {
            attachmentUid: attachment.uid,
            loaded: 100,
            total: 100,
            canceller
        });
        commit("draft/setAttachmentStatus", { attachmentUid: attachment.uid, status: "ERROR" });
    }
}
