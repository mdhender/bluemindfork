import { addApplicationAlert } from "../../src/mutations/addApplicationAlert";
import { AlertFactory } from "../../src";

let alertCodeExample = "MSG_EXAMPLE";

jest.mock("../../src/AlertFactory");
AlertFactory.create.mockReturnValue({
    code: alertCodeExample
});

describe("[AlertStore][mutations] : add application alert", () => {
    test("add application alert to the state", () => {
        const state = {
            applicationAlerts: [
                {
                    code: "PREVIOUS_ALERT"
                }
            ]
        };
        const newAlert = { code: alertCodeExample, props: {} };
        addApplicationAlert(state, newAlert);

        expect(AlertFactory.create).toHaveBeenCalledTimes(1);
        expect(AlertFactory.create).toHaveBeenCalledWith(alertCodeExample, {}, undefined);
        expect(state.applicationAlerts.length).toEqual(2);
        expect(state.applicationAlerts.find(a => a.code === alertCodeExample)).not.toEqual(undefined);
    });
});
