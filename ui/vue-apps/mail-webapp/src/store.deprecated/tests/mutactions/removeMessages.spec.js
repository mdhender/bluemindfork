import { _removeMessages } from "../../mutactions/removeMessages";
import mutationTypes from "../../../store/mutationTypes";

const context = {
    commit: jest.fn()
};

describe("[Mail-WebappStore][mutactions] : _removeMessages", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("Basic", () => {
        _removeMessages(context, [1, 2]);
        expect(context.commit).toHaveBeenCalledWith("deleteSelectedMessageKey", 1);
        expect(context.commit).toHaveBeenCalledWith("deleteSelectedMessageKey", 2);
        expect(context.commit).toHaveBeenCalledWith("messages/removeParts", [1, 2]);
        expect(context.commit).toHaveBeenCalledWith("mail/" + mutationTypes.REMOVE_MESSAGES, [1, 2], { root: true });
    });
});
