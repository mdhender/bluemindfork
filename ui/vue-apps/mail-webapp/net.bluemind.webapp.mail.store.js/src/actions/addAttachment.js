import injector from "@bluemind/inject";

const updateDraft = function({ commit, dispatch }, addrPart, file) {
    const attachment = {
        address: addrPart,
        mime: file.type || "application/octet-stream",
        fileName: file.name,
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
    commit("addAttachmentToDraft", attachment);
    return dispatch("saveDraft");
};

const errorAlert = function({ commit }, file, reason) {
    commit("alert/add", { code: "MSG_DRAFT_ATTACH_ERROR", props: { filename: file.name, reason } }, { root: true });
};

export function addAttachment({ getters, commit, dispatch }, file) {
    const reader = new FileReader();
    reader.readAsArrayBuffer(file);
    return new Promise((resolve, reject) => {
        reader.onload = resolve;
        reader.onerror = reject;
    })
        .then(() => {
            const service = injector.getProvider("MailboxItemsPersistence").get(getters.my.DRAFTS.uid);
            return service.uploadPart(reader.result);
        })
        .then(addrPart => updateDraft({ commit, dispatch }, addrPart, file))
        .catch(reason => {
            errorAlert({ commit }, file, reason);
        });
}
