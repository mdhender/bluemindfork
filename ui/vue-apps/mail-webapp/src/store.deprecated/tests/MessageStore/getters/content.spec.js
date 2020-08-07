import { content } from "../../../MessageStore/getters/content";
import { PartsHelper } from "@bluemind/email";

PartsHelper.insertInlineImages = jest.fn().mockReturnValue([]);

describe("[Mail-WebappStore/MessageStore][getters] : content ", () => {
    const state = {};
    let rootState = {};
    let rootGetters = {};
    let parts = {};

    beforeEach(() => {
        PartsHelper.insertInlineImages.mockClear();
        Object.assign(state, {
            key: "key",
            parts: {
                inlines: [
                    { address: "1.1", mime: "text/html" },
                    { address: "1.2", mime: "text/plain" }
                ]
            }
        });
        rootState = {
            "mail-webapp": {
                messages: {
                    itemsParts: {
                        key: ["dummy"],
                        "not-key": []
                    },
                    partContents: {}
                }
            }
        };
        parts = { "not-key": {}, key: { "1.1": "HTML", "1.2": "TEXT" } };
        rootGetters["mail-webapp/messages/getPartContent"] = (key, addr) => parts[key][addr];
    });

    test("return current message inline parts ", () => {
        const result = content(state, undefined, rootState, rootGetters);
        expect(result.length).toEqual(2);
        expect(result[0].address).toEqual("1.1");
        expect(result[0].content).toEqual("HTML");
        expect(result[1].address).toEqual("1.2");
        expect(result[1].content).toEqual("TEXT");
    });

    test("set content from the messages state not from the currentMessagesParts ", () => {
        state.parts.inlines[0].content = null;
        const result = content(state, undefined, rootState, rootGetters);
        expect(result[0].address).toEqual("1.1");
        expect(result[0].content).not.toBeNull();
    });

    test("do not return part without content ", () => {
        state.parts.inlines.push({ address: "1.3", mime: "text/html" });
        state.parts.inlines.push({ address: "1.4", mime: "text/plain" });
        const result = content(state, undefined, rootState, rootGetters);
        expect(result.length).toEqual(2);
        expect(result.find(part => ["1.3", "1.4"].includes(part.address))).not.toBeTruthy();
    });

    test("inline images are included in html ", () => {
        state.parts.inlines = [
            { address: "1.1", mime: "text/html" },
            { address: "1.2", mime: "text/pain" },
            { address: "1.3", mime: "image/png", contentId: "uid" }
        ];
        parts.key = {
            "1.1": "html",
            "1.2": "text",
            "1.3": "image"
        };
        PartsHelper.insertInlineImages.mockReturnValue(["uid"]);
        const result = content(state, undefined, rootState, rootGetters);
        expect(PartsHelper.insertInlineImages).toHaveBeenCalledWith(
            [{ address: "1.1", mime: "text/html", content: "html" }],
            [{ address: "1.3", mime: "image/png", contentId: "uid", content: "image" }]
        );
        expect(result.length).toEqual(2);
        expect(result[0].address).toEqual("1.1");
        expect(result[1].address).toEqual("1.2");
    });
});
