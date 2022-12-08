import { mount } from "@vue/test-utils";
import BmFormAutocompleteInput from "../../../src/components/form/BmFormAutocompleteInput";
import fruits from "../../data/fruits";

describe("BmFormAutocompleteInput", () => {
    const searchedPattern = "a";
    const inputSelector = "input[type='text']";

    let alternativeResults = undefined;

    function defaultMount() {
        return mount(BmFormAutocompleteInput, {
            propsData: {
                value: searchedPattern,
                items: alternativeResults === undefined ? fruits : alternativeResults,
                selectedResult: 0,
                maxResults: 4
            }
        });
    }

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("BmFormAutocompleteInput should match snapshot", () => {
        expect(defaultMount().vm.$el).toMatchSnapshot();
    });

    test("BmFormAutocompleteInput displays a list of items and respect display results limit", () => {
        const wrapper = defaultMount();
        fruits.splice(0, 4).forEach(data => {
            expect(wrapper.text()).toContain(data);
        });
        expect(wrapper.html()).toContain('<div class="list-group-item active"');
    });

    test("Click on autocomplete result returns a selected event", async () => {
        const wrapper = defaultMount();
        wrapper.find(inputSelector).trigger("focusin");
        wrapper.find(".list-group-item:first-child").trigger("click");
        expect(wrapper.emitted().selected).toBeTruthy();
    });

    test("Click on enter on input select current autocomplete result", () => {
        const wrapper = defaultMount();
        wrapper.find(inputSelector).trigger("focusin");
        wrapper.find(inputSelector).trigger("keydown.enter");
        expect(wrapper.emitted().selected).toBeTruthy();
    });

    test("Click on tab on input select current autocomplete result", () => {
        const wrapper = defaultMount();
        wrapper.find(inputSelector).trigger("focusin");
        wrapper.find(inputSelector).trigger("keydown.tab");
        expect(wrapper.emitted().selected).toBeTruthy();
    });

    test("Click on enter returns a submit event if no results proposed", () => {
        alternativeResults = [];
        const wrapper = defaultMount();
        wrapper.find(inputSelector).trigger("keydown.enter");
        expect(wrapper.emitted().submit).toBeTruthy();
    });

    test("Typing text on input returns an input event", () => {
        const wrapper = defaultMount();
        wrapper.find(inputSelector).setValue("pea");
        expect(wrapper.emitted().input[0][0]).toBe("pea");
    });
});
