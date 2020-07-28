import { getAttachmentProgress } from "../../../src/MessageStore/getters/getAttachmentProgress";

const state = {
    attachmentProgresses: {
        att01: { loaded: 10, total: 100, canceller: "cancelFunction" },
        att02: { loaded: 50, total: 100, canceller: "cancelFunction2" }
    }
};

describe("[Mail-WebappStore/MessageStore][getters] : getAttachmentProgress ", () => {
    test("Basic", () => {
        const result = getAttachmentProgress(state)("att02");
        expect(result).toEqual({ loaded: 50, total: 100, canceller: "cancelFunction2" });
    });
});
