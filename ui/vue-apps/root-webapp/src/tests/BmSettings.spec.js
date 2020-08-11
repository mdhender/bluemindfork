import BmSettingsContent from "../components/settings/BmSettingsContent";
import BmSettings from "../components/settings/BmSettings";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { MountComponentUtils } from "@bluemind/test-utils";

describe("BmSettings", () => {
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
        wrapper = MountComponentUtils.createWrapper(BmSettings, mockedStore, props);
        expect(wrapper.findComponent(BmSettings)).toBeDefined();
    });

    test("is loading at init when data fetch has failed", async () => {
        mockedStore.modules.session.actions.FETCH_ALL_SETTINGS = jest.fn().mockRejectedValue();
        wrapper = MountComponentUtils.createWrapper(BmSettings, mockedStore, props);
        await wrapper.vm.$nextTick();
        expect(mockedStore.modules.session.actions.FETCH_ALL_SETTINGS).toHaveBeenCalled();
        expect(wrapper.vm.$data.status).toBe("loading");
    });

    test("is idle at init when data fetch has successed", async () => {
        mockedStore.modules.session.actions.FETCH_ALL_SETTINGS = jest.fn().mockResolvedValue();
        wrapper = MountComponentUtils.createWrapper(BmSettings, mockedStore, props);
        await wrapper.vm.$nextTick();
        expect(mockedStore.modules.session.actions.FETCH_ALL_SETTINGS).toHaveBeenCalled();
        expect(wrapper.vm.$data.status).toBe("idle");
    });

    test("after a successful save, status is saved", async () => {
        mockedStore.modules.session.actions.UPDATE_ALL_SETTINGS = jest.fn().mockResolvedValue();
        wrapper = MountComponentUtils.createWrapper(BmSettings, mockedStore, props);
        await wrapper.vm.save("updated !");
        expect(mockedStore.modules.session.actions.UPDATE_ALL_SETTINGS).toHaveBeenCalledWith(
            expect.anything(),
            "updated !"
        );
        expect(wrapper.vm.$data.status).toBe("saved");
    });

    test("after a failed save, status is error", async () => {
        mockedStore.modules.session.actions.UPDATE_ALL_SETTINGS = jest.fn().mockRejectedValue();
        wrapper = MountComponentUtils.createWrapper(BmSettings, mockedStore, props);
        await wrapper.vm.save("updated !");
        expect(mockedStore.modules.session.actions.UPDATE_ALL_SETTINGS).toHaveBeenCalledWith(
            expect.anything(),
            "updated !"
        );
        expect(wrapper.vm.$data.status).toBe("error");
    });
});
