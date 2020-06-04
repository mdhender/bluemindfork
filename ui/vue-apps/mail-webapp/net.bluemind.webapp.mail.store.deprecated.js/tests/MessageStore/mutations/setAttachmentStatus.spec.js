import { setAttachmentStatus } from "../../../src/MessageStore/mutations/setAttachmentStatus";

const state = {
    attachmentStatuses: {
        att01: "OK",
        att02: "WARNING"
    }
};

describe("[Mail-WebappStore/MessageStore][mutations] : setAttachmentStatus ", () => {
    test("Basic", () => {
        expect(state.attachmentStatuses).toStrictEqual({
            att01: "OK",
            att02: "WARNING"
        });
        setAttachmentStatus(state, { attachmentUid: "att03", status: "ERROR" });
        expect(state.attachmentStatuses).toStrictEqual({
            att01: "OK",
            att02: "WARNING",
            att03: "ERROR"
        });
        setAttachmentStatus(state, { attachmentUid: "att03", status: "FATAL" });
        expect(state.attachmentStatuses).toStrictEqual({
            att01: "OK",
            att02: "WARNING",
            att03: "FATAL"
        });
    });
});
