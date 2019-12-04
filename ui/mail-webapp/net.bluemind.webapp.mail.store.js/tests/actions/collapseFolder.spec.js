import { collapseFolder } from "../../src/actions/collapseFolder";
import ItemUri from "@bluemind/item-uri";

const context = {
    commit: jest.fn()
};

describe("[Mail-WebappStore][actions] : collapseFolder", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("mutate state to collapse folder", () => {
        collapseFolder(context, ItemUri.encode(1, 2));
        expect(context.commit).toHaveBeenCalledWith("collapseFolder", 1);
    });
});
