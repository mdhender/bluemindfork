import { attachments } from "../../../src/MessageStore/getters/attachments";

describe("[Mail-WebappStore/MessageStore][getters] : attachments ", () => {
    const state = {};
    let parts;
    const rootGetters = {};
    beforeEach(() => {
        Object.assign(state, {
            key: "key",
            parts: { attachments: [{ address: "1.1" }] },
            messages: {
                itemsParts: {
                    key: ["dummy"],
                    "not-key": []
                }
            }
        });
        parts = { "not-key": {}, key: { "1.1": "good part", "1.2": "wrong part" } };
        rootGetters["mail-webapp/messages/getPartContent"] = (key, addr) => parts[key][addr];
    });
    test("return Message attachments", () => {
        const result = attachments(state, undefined, undefined, rootGetters);
        expect(result.length).toEqual(1);
        expect(result[0].address).toEqual("1.1");
        expect(result[0].content).toEqual("good part");
    });
    test("set content from the messages state not from the parts", () => {
        state.parts.attachments[0].content = null;
        const result = attachments(state, undefined, undefined, rootGetters);
        expect(result[0].content).not.toBeNull();
    });
});
