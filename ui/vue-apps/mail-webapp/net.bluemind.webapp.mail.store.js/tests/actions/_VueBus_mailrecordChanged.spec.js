import { $_VueBus_mailrecordChanged } from "../../src/actions/$_VueBus_mailrecordChanged";

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve())
};
describe("[Mail-WebappStore][actions] :  $_VueBus_mailrecordChanged", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
    });
    test("load all messages for mail records", () => {
        $_VueBus_mailrecordChanged(context, "container");
        expect(context.dispatch).toHaveBeenCalledWith("all", "container");
    });
});
