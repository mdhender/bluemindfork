import { removeAllApplicationAlerts } from "../../src/mutations/removeAllApplicationAlerts";

describe("[AlertStore][mutations] : remove all alerts", () => {
    test("remove all alerts from the state", () => {
        const state = [
            {
                code: "PREVIOUS_ALERT",
                uid: "454484e-d484eed"
            },
            {
                code: "ALERT_TO_DELETE"
            }
        ];
        removeAllApplicationAlerts(state);

        expect(state.length).toEqual(0);
    });
});
