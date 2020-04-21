import AlertFactory from "../src/AlertFactory";
import alertRegistry from "../src/AlertRegistry";
import AlertTypes from "../src/AlertTypes";
import global from "@bluemind/global";
import ServiceLocator from "@bluemind/inject";

jest.mock("@bluemind/inject");
ServiceLocator.getProvider.mockReturnValue({
    get: jest.fn().mockReturnValue({
        t: jest.fn().mockReturnValue("blabla")
    })
});

describe("[AlertStore][mutations] : AlertFactory", () => {
    const alerts = {
        MSG_LOADING: {
            type: AlertTypes.LOADING,
            key: "msg.loading"
        },
        MSG_SUCCESS: {
            type: AlertTypes.SUCCESS,
            key: "msg.ok"
        }
    };

    beforeEach(() => {
        global.$alerts = undefined;
    });

    test("register alerts", () => {
        AlertFactory.register(alerts);
        expect(alertRegistry).toEqual(alerts);
    });

    test("create alert", () => {
        AlertFactory.register(alerts);
        const props = { subject: "My Subject" };
        const alert = AlertFactory.create("MSG_LOADING", props);

        expect(alert.code).toEqual("MSG_LOADING");
        expect(alert.type).toEqual(AlertTypes.LOADING);
        expect(alert.key).toEqual("msg.loading");
        expect(alert.message).toEqual("blabla");
        expect(alert.props).toEqual(props);
        expect(alert.uid).not.toEqual(undefined);
    });
});
