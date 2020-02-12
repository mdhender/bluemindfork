import { add } from "../../src/mutations/add";
import { AlertFactory } from "../../src";

let alertCodeExample = "MSG_EXAMPLE";

jest.mock("../../src/AlertFactory");
AlertFactory.create.mockReturnValue({
    code: alertCodeExample
});

describe("[AlertStore][mutations] : add", () => {
    test("add alert to the state", () => {
        const state = {
            alerts: [
                {
                    code: "PREVIOUS_ALERT"
                }
            ]
        };
        const newAlert = { code: alertCodeExample, props: {} };
        add(state, newAlert);

        expect(AlertFactory.create).toHaveBeenCalledTimes(1);
        expect(AlertFactory.create).toHaveBeenCalledWith(alertCodeExample, {}, undefined);
        expect(state.alerts.length).toEqual(2);
        expect(state.alerts.find(a => a.code == alertCodeExample)).not.toEqual(undefined);
    });
});
