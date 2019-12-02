import { expandFolder } from "../../src/actions/expandFolder";

const context = {
    commit: jest.fn()
};

describe("[Mail-WebappStore][actions] : expandFolder", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("mutate state to expand folder", () => {
        expandFolder(context, 1);
        expect(context.commit).toHaveBeenCalledWith("expandFolder", 1);
    });
});
