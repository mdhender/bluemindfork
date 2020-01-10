import { currentMessageAttachments } from "../../src/getters/currentMessageAttachments";

describe("[Mail-WebappStore][getters] : currentMessageAttachments ", () => {
    const state = {};
    let parts;
    const getters = {};
    beforeEach(() => {
        Object.assign(state, {
            currentMessageKey: "key",
            currentMessageParts: { attachments: [{ address: "1.1" }] },
            messages: {
                itemsParts: {
                    key: ["dummy"],
                    "not-key": []
                }
            }
        });
        parts = { "not-key": {}, key: { "1.1": "good part", "1.2": "wrong part" } };
        getters["messages/getPartContent"] = (key, addr) => parts[key][addr];
    });
    test("return current Message attachments ", () => {
        const result = currentMessageAttachments(state, getters);
        expect(result.length).toEqual(1);
        expect(result[0].address).toEqual("1.1");
        expect(result[0].content).toEqual("good part");
    });
    test("set content from the messages state not from the currentMessagesParts ", () => {
        state.currentMessageParts.attachments[0].content = null;
        const result = currentMessageAttachments(state, getters);
        expect(result[0].content).not.toBeNull();
    });
});
