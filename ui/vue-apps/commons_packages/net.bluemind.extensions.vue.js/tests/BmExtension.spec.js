import inject from "@bluemind/inject";
import { mount } from "@vue/test-utils";
import { mapExtensions } from "@bluemind/extensions";
import { default as BmExtension, Cache } from "../src/BmExtension";
import BmExtensionList from "../src/BmExtensionList";
import BmExtensionDecorator from "../src/BmExtensionDecorator";
import BmExtensionRenderless from "../src/BmExtensionRenderless";

jest.mock("@bluemind/extensions");
inject.register({ provide: "UserSession", factory: () => ({ roles: "" }) });

self.bundleResolve = jest.fn().mockImplementation((id, callback) => callback());

describe("BmExtension", () => {
    beforeEach(() => {
        mapExtensions.mockReset();
        mapExtensions.mockReturnValue({ extensions: [] });
        Cache.clear();
    });

    test("to call mapExtensions", () => {
        mount(BmExtension, {
            propsData: {
                id: "test.dummy.id",
                path: "dummy-element"
            }
        });
        expect(mapExtensions).toHaveBeenCalledWith("test.dummy.id", ["component"]);
    });
});
