import { getPartContent } from "../../../src/MailboxItemsStore/getters/getPartContent";
import { PartKey } from "../../../src/MailboxItemsStore/PartKey";

const state = {
    partContents: { [PartKey.encode(2, 1)]: "Part 1", [PartKey.encode("B", "A")]: "Part 2" }
};

describe("[MailboxItemsStore][getters] : getPartContent ", () => {
    test("return part for a given message key and part address", () => {
        let content = getPartContent(state)(1, 2);
        expect(content).toEqual("Part 1");
        content = getPartContent(state)("A", "B");
        expect(content).toEqual("Part 2");
    });
    test("return undefined if no message match", () => {
        const content = getPartContent(state)("A", 2);

        expect(content).toBeUndefined();
    });
});
