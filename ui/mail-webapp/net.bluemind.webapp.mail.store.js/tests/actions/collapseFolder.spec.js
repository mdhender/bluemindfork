import { collapseFolder } from "../../src/actions/collapseFolder";

const context = {
    commit: jest.fn()
};

describe("[Mail-WebappStore][actions] : collapseFolder", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("mutate state to collapse folder", () => {
        collapseFolder(context, 1);
        expect(context.commit).toHaveBeenCalledWith("collapseFolder", 1);
    });
});
