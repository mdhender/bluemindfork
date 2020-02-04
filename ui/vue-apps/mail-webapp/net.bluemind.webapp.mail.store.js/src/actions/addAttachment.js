import injector from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";

const errorAlert = function({ commit }, file, reason) {
    commit("alert/add", { code: "MSG_DRAFT_ATTACH_ERROR", props: { filename: file.name, reason } }, { root: true });
};

export function addAttachment({ getters, commit, dispatch }, file) {
    // read the local file
    const reader = new FileReader();
    reader.readAsArrayBuffer(file);

    // build our DTO representing an attachment
    const attachment = {
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

    // enable the preview of this attachment
    attachment.content = file;
    commit("draft/addAttachment", attachment);

    // upload this attachment then save the draft
    return new Promise((resolve, reject) => {
        reader.onload = resolve;
        reader.onerror = reject;
    })
        .then(() => {
            const service = injector.getProvider("MailboxItemsPersistence").get(getters.my.DRAFTS.uid);
            return service.uploadPart(reader.result);
        })
        .then(addrPart => {
            attachment.address = addrPart;
            commit("draft/updateAttachment", attachment);
        })
        .catch(reason => {
            errorAlert({ commit }, file, reason);
        });
}
