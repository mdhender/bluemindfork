import { all } from "../../../src/ContainersStore/actions/all";
import { FETCH_MAILBOXES } from "@bluemind/webapp.mail.store";

const context = {
    dispatch: jest.fn().mockResolvedValue()
};

describe("[ContainersStore][actions] : all", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
    });
    test("call 'all' service and mutate state with result", async () => {
        await all(context, { type: "T", verb: "V" });
        expect(context.dispatch).toHaveBeenCalledWith(FETCH_MAILBOXES, null, { root: true });
    });
    test("fail if 'all' call fail", () => {
        context.dispatch.mockRejectedValue("Error!");
        expect(all(context, { type: "T", verb: "V" })).rejects.toBe("Error!");
    });
});
