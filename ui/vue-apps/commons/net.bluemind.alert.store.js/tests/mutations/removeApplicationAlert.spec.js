import { removeApplicationAlert } from "../../src/mutations/removeApplicationAlert";

describe("[AlertStore][mutations] : remove application alert", () => {
    test("remove alert from the state", () => {
        const alertUid = "fer45fe5-dze441";

        const state = {
            applicationAlerts: [
                {
                    code: "PREVIOUS_ALERT",
                    uid: "454484e-d484eed"
                },
                {
                    code: "ALERT_TO_DELETE",
                    uid: alertUid
                }
            ]
        };

        removeApplicationAlert(state, alertUid);

        expect(state.applicationAlerts.length).toEqual(1);
        expect(state.applicationAlerts.find(a => a.uid === alertUid)).toEqual(undefined);
    });
});
