import Preferences from "../components/preferences/Preferences";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { MountComponentUtils } from "@bluemind/test-utils";

describe("Preferences", () => {
    let wrapper;
    let mockedStore;
    let props;

    beforeEach(() => {
        props = {
            applications: [{ href: "/mail/", icon: {} }, { href: "unknown" }],
            user: { displayname: "my name" }
        };
        mockedStore = MountComponentUtils.mockSessionStore();
    });

    test("can be mounted", () => {
        wrapper = MountComponentUtils.createWrapper(Preferences, mockedStore, props);
        expect(wrapper.findComponent(Preferences)).toBeDefined();
    });

    test("is idle at init", async () => {
        wrapper = MountComponentUtils.createWrapper(Preferences, mockedStore, props);
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.$data.status).toBe("idle");
    });

    test("after a successful save, status is saved", async () => {
        mockedStore.modules.session.actions.SAVE_SETTINGS = jest.fn().mockResolvedValue();
        wrapper = MountComponentUtils.createWrapper(Preferences, mockedStore, props);
        await wrapper.vm.save("updated !");
        expect(mockedStore.modules.session.actions.SAVE_SETTINGS).toHaveBeenCalled();
        expect(wrapper.vm.$data.status).toBe("saved");
    });

    test("after a failed save, status is error", async () => {
        mockedStore.modules.session.actions.SAVE_SETTINGS = jest.fn().mockRejectedValue();
        wrapper = MountComponentUtils.createWrapper(Preferences, mockedStore, props);
        await wrapper.vm.save("updated !");
        expect(mockedStore.modules.session.actions.SAVE_SETTINGS).toHaveBeenCalled();
        expect(wrapper.vm.$data.status).toBe("error");
    });
});
