import { setAttachmentProgress } from "../../../src/MessageStore/mutations/setAttachmentProgress";

const state = {
    attachmentProgresses: {
        att01: { loaded: 10, total: 100, canceller: "cancelFunction" },
        att02: { loaded: 50, total: 100, canceller: "cancelFunction2" }
    }
};

describe("[Mail-WebappStore/MessageStore][mutations] : setAttachmentProgress ", () => {
    test("Basic", () => {
        expect(state.attachmentProgresses).toStrictEqual({
            att01: { loaded: 10, total: 100, canceller: "cancelFunction" },
            att02: { loaded: 50, total: 100, canceller: "cancelFunction2" }
        });
        setAttachmentProgress(state, { attachmentUid: "att03", loaded: 2, total: 200, canceller: "cancelFunction3" });
        expect(state.attachmentProgresses).toStrictEqual({
            att01: { loaded: 10, total: 100, canceller: "cancelFunction" },
            att02: { loaded: 50, total: 100, canceller: "cancelFunction2" },
            att03: { loaded: 2, total: 200, canceller: "cancelFunction3" }
        });
        setAttachmentProgress(state, { attachmentUid: "att03", loaded: 142, total: 200, canceller: "cancelFunction3" });
        expect(state.attachmentProgresses).toStrictEqual({
            att01: { loaded: 10, total: 100, canceller: "cancelFunction" },
            att02: { loaded: 50, total: 100, canceller: "cancelFunction2" },
            att03: { loaded: 142, total: 200, canceller: "cancelFunction3" }
        });
    });
});
