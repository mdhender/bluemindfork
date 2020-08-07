import { getAttachmentStatus } from "../../../MessageStore/getters/getAttachmentStatus";

const state = {
    attachmentStatuses: {
        att01: "OK",
        att02: "ERROR"
    }
};

describe("[Mail-WebappStore/MessageStore][getters] : getAttachmentStatus ", () => {
    test("Basic", () => {
        const result = getAttachmentStatus(state)("att02");
        expect(result).toEqual("ERROR");
    });
});
