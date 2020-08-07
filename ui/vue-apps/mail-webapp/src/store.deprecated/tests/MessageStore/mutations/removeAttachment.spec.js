import { removeAttachment } from "../../../MessageStore/mutations/removeAttachment";

const state = {
    parts: {
        attachments: [{ uid: "att01" }, { uid: "att02" }, { uid: "att03" }]
    },
    attachmentStatuses: {
        att02: {}
    },
    attachmentProgresses: {
        att02: {}
    }
};

describe("[Mail-WebappStore/MessageStore][mutations] : removeAttachment ", () => {
    test("Basic", () => {
        expect(state.parts.attachments).toStrictEqual([{ uid: "att01" }, { uid: "att02" }, { uid: "att03" }]);
        removeAttachment(state, "att02");
        expect(state.parts.attachments).toStrictEqual([{ uid: "att01" }, { uid: "att03" }]);
        expect(state.attachmentProgresses).toStrictEqual({});
        expect(state.attachmentStatuses).toStrictEqual({});
    });
});
