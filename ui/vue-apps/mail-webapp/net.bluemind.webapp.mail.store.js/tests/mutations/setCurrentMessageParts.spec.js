import { setCurrentMessageParts } from "../../src/mutations/setCurrentMessageParts";

describe("[Mail-WebappStore][mutations] : setCurrentMessageParts ", () => {
    const state = {};
    beforeEach(() => {
        state.currentMessageParts = { attachments: [1, 2], inlines: [3, 4] };
    });
    test(" mutate currentMessageParts", () => {
        const value = { attachments: [5, 6], inlines: [8, 10] };
        setCurrentMessageParts(state, value);
        expect(state.currentMessageParts).toEqual(value);
    });
    test(" only mutate currentMessageParts ", () => {
        setCurrentMessageParts(state, "key2");
        expect(Object.keys(state)).toEqual(["currentMessageParts"]);
    });
});
