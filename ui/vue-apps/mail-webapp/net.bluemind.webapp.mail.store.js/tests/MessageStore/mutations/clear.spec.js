import { clear } from "../../../src/MessageStore/mutations/clear";

const state = {
    key: "AD1DZSD4",
    id: "42",
    parts: {
        attachments: ["att01", "att02"],
        inlines: ["inl01", "inl02"]
    }
};

describe("[Mail-WebappStore/MessageStore][mutations] : clear", () => {
    test("Basic", () => {
        clear(state);
        expect(state.key).toBeUndefined();
        expect(state.id).toBeUndefined();
        expect(state.parts.attachments).toHaveLength(0);
        expect(state.parts.inlines).toHaveLength(0);
    });
});
