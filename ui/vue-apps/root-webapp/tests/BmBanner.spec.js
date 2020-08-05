import { shallowMount, createLocalVue } from "@vue/test-utils";
import merge from "lodash.merge";
import BmBanner from "../src/components/BmBanner";
import { BmPopover } from "@bluemind/styleguide";

jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
//TODO refacto createWrapper later with testutils when FEATWEBML-597 is merged
function createWrapper(overrides) {
    const localVue = createLocalVue();
    const defaultMountingOptions = {
        localVue,
        propsData: { applications: [], user: {} },
        mocks: {
            $t: () => {},
            $tc: () => {}
        }
    };
    const mergedMountingOptions = merge(defaultMountingOptions, overrides);
    return shallowMount(BmBanner, mergedMountingOptions);
}

describe("BmBanner", () => {
    test("is a Vue instance", () => {
        const wrapper = createWrapper();
        expect(wrapper.isVueInstance()).toBeTruthy();
    });

    test("should match snapshot", () => {
        const wrapper = createWrapper();
        expect(wrapper.element).toMatchSnapshot();
    });

    test("should show menu when button is clicked", () => {
        const wrapper = createWrapper();
        expect(!wrapper.find(BmPopover).isVisible()).toBeTruthy;
        wrapper.find("#all-apps-popover").trigger("click");
        expect(wrapper.find(BmPopover).isVisible()).toBeTruthy();
    });
});
