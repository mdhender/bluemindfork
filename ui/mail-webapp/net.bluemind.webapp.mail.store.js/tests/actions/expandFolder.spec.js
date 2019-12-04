import { expandFolder } from "../../src/actions/expandFolder";
import ItemUri from "@bluemind/item-uri";

const context = {
    commit: jest.fn()
};

describe("[Mail-WebappStore][actions] : expandFolder", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("mutate state to expand folder", () => {
        expandFolder(context, ItemUri.encode(1, 2));
        expect(context.commit).toHaveBeenCalledWith("expandFolder", 1);
    });
});
