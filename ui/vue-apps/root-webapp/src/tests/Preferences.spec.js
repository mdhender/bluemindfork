import Preferences from "../components/preferences/Preferences";
jest.mock("@bluemind/ui-components/src/css/utils/_variables.scss", () => ({ iconsColors: "" }));
import { MountComponentUtils } from "@bluemind/test-utils";
jest.mock("postal-mime", () => ({ TextEncoder: jest.fn() }));

import ServiceLocator from "@bluemind/inject";
ServiceLocator.register({
    provide: "UserSession",
    factory: () => ({ roles: "" })
});

describe("Preferences", () => {
    let mockedStore;
    const props = {
        applications: [{ href: "/mail/", icon: {} }, { href: "unknown" }, { href: "/cal/", icon: {} }],
        user: { displayname: "my name" }
    };

    beforeEach(() => {
        mockedStore = MountComponentUtils.mockSettingsStore();
        window.bmExtensions_ = [];
    });

    test("can be mounted", () => {
        const wrapper = MountComponentUtils.createWrapper(Preferences, mockedStore, props);

        expect(wrapper.findComponent(Preferences)).toBeDefined();
    });
});
