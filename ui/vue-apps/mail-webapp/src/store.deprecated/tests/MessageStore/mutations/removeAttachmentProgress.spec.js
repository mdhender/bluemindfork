import { removeAttachmentProgress } from "../../../MessageStore/mutations/removeAttachmentProgress";

const state = {
    attachmentProgresses: { att01: "progress01", att02: "progress01", att03: "progress01" }
};

describe("[Mail-WebappStore/MessageStore][mutations] : removeAttachment ", () => {
    test("Basic", () => {
        expect(state.attachmentProgresses).toStrictEqual({
            att01: "progress01",
            att02: "progress01",
            att03: "progress01"
        });
        removeAttachmentProgress(state, "att02");
        expect(state.attachmentProgresses).toStrictEqual({ att01: "progress01", att03: "progress01" });
    });
});
