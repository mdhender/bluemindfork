import { addAttachment } from "../../../MessageStore/mutations/addAttachment";

const state = {
    parts: {
        attachments: ["att01", "att02"]
    }
};

describe("[Mail-WebappStore/MessageStore][mutations] : addAttachment ", () => {
    test("Basic", () => {
        expect(state.parts.attachments).toStrictEqual(["att01", "att02"]);
        addAttachment(state, "att03");
        expect(state.parts.attachments).toStrictEqual(["att01", "att02", "att03"]);
    });
});
