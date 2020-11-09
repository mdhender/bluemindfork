import { REMOVE_MESSAGES } from "~mutations";
import { _removeMessages } from "../../mutactions/removeMessages";

const context = {
    commit: jest.fn()
};

describe("[Mail-WebappStore][mutactions] : _removeMessages", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("Basic", () => {
        _removeMessages(context, [1, 2]);
        expect(context.commit).toHaveBeenCalledWith("mail/" + REMOVE_MESSAGES, [1, 2], { root: true });
    });
});
