import { setParts } from "../../../src/MessageStore/mutations/setParts";

const state = {
    parts: {
        attachments: ["att01", "att02"],
        inlines: ["inl01", "inl02"]
    }
};

describe("[Mail-WebappStore/MessageStore][mutations] : setParts ", () => {
    test("Basic", () => {
        expect(state.parts).toStrictEqual({ attachments: ["att01", "att02"], inlines: ["inl01", "inl02"] });
        setParts(state, { attachments: ["att03", "att04"], inlines: ["inl03", "inl04"] });
        expect(state.parts).toStrictEqual({ attachments: ["att03", "att04"], inlines: ["inl03", "inl04"] });
    });
});
