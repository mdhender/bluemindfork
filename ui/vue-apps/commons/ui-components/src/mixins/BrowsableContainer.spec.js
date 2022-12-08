import BrowsableContainer, { setIgnoreVisibility } from "./BrowsableContainer.js";
import { createLocalVue } from "@vue/test-utils";
import { shallowMount } from "@vue/test-utils";

const Vue = createLocalVue();

describe("BrowsableContainer", () => {
    let TestComponent;
    beforeAll(() => {
        setIgnoreVisibility(true);
    });
    beforeEach(() => {
        TestComponent = {
            template: "<div><slot /></div>",
            mixins: [BrowsableContainer]
        };
        document.body.focus();
    });
    afterAll(() => {
        setIgnoreVisibility(false);
    });
    test("BrowsableContainer mixin is installed", () => {
        const wrapper = shallowMount(TestComponent, {
            localVue: Vue
        });
        expect(wrapper.vm.focus).toBeDefined();
    });
    // eslint-disable-next-line max-len
    test("Expect the default element to be the one with browse-default, or tabindex 0 or the first browsable element", () => {
        let wrapper = shallowMount(TestComponent, {
            localVue: Vue,
            slots: {
                default:
                    "<span id='not-default' data-browse></span>" +
                    "<span id='default' data-browse data-browse-default></span>"
            }
        });
        expect(wrapper.vm.$_Container_focused.id).toBe("default");
        wrapper = shallowMount(TestComponent, {
            localVue: Vue,
            slots: {
                default: "<span id='not-default' data-browse></span><span id='default' data-browse tabindex='0'></span>"
            }
        });
        expect(wrapper.vm.$_Container_focused.id).toBe("default");
        wrapper = shallowMount(TestComponent, {
            localVue: Vue,
            slots: {
                default: "<span id='default' data-browse></span><span id='not-default' data-browse ></span>"
            }
        });
        expect(wrapper.vm.$_Container_focused.id).toBe("default");
    });
    test("Focus method give focus to the default element", () => {
        const wrapper = shallowMount(TestComponent, {
            localVue: Vue,
            slots: {
                default:
                    "<span id='not-default' data-browse></span>" +
                    "<span id='default' data-browse data-browse-default></span>"
            }
        });

        wrapper.vm.focus();
        expect(document.activeElement.id).toBe("default");
    });
    test("Expect the default element to be the only one accessible with tabindex", () => {
        const wrapper = shallowMount(TestComponent, {
            localVue: Vue,
            slots: {
                default:
                    "<span id='not-default' data-browse></span>" +
                    "<span id='default' data-browse data-browse-default></span>" +
                    "<span id='neither-default' data-browse></span>"
            }
        });
        expect(wrapper.find("#not-default").attributes("tabindex")).toBe("-1");
        expect(wrapper.find("#neither-default").attributes("tabindex")).toBe("-1");
        expect(wrapper.find("#default").attributes("tabindex")).toBe("0");
    });
    test("Expect the key and tab navigation to trigger focusNext / focusPrevious", () => {
        const wrapper = shallowMount(TestComponent, {
            localVue: Vue,
            slots: {
                default:
                    "<span id='first' data-browse></span>" +
                    "<span id='second' data-browse></span>" +
                    "<span id='third' data-browse></span>"
            }
        });
        wrapper.vm.focus();
        wrapper.trigger("keydown", {
            key: "Right"
        });
        expect(document.activeElement.id).toBe("second");
        wrapper.trigger("keydown", {
            key: "Tab"
        });
        expect(document.activeElement.id).toBe("third");
        wrapper.trigger("keydown", {
            key: "Left"
        });
        expect(document.activeElement.id).toBe("second");
        wrapper.trigger("keydown", {
            key: "Tab",
            shiftKey: true
        });
        expect(document.activeElement.id).toBe("first");
    });
    test("Expect navigation to follow browse-index order", () => {
        const wrapper = shallowMount(TestComponent, {
            localVue: Vue,
            slots: {
                default:
                    "<span id='second' data-browse data-browse-index='2'></span>" +
                    "<span id='first' data-browse data-browse-index='1'></span>" +
                    "<span id='third' data-browse data-browse-index='3'></span>"
            }
        });
        wrapper.vm.focus();
        expect(document.activeElement.id).toBe("first");
        wrapper.vm.focusNext();
        expect(document.activeElement.id).toBe("second");
        wrapper.vm.focusNext();
        expect(document.activeElement.id).toBe("third");
        wrapper.vm.focusPrevious();
        expect(document.activeElement.id).toBe("second");
        wrapper.vm.focusPrevious();
        expect(document.activeElement.id).toBe("first");
        wrapper.vm.focusLast();
        expect(document.activeElement.id).toBe("third");
        wrapper.vm.focusFirst();
        expect(document.activeElement.id).toBe("first");
        wrapper.vm.focusByIndex(1);
        expect(document.activeElement.id).toBe("second");
    });
    test("Expect focus change to trigger browse:focus event", () => {
        const wrapper = shallowMount(TestComponent, {
            attachToDocument: true,
            localVue: Vue,
            slots: {
                default:
                    "<span id='first' data-browse data-browse-key='B'></span>" +
                    "<span id='second' data-browse data-browse-key='B'></span>"
            }
        });
        wrapper.vm.focus();
        // Focus in is not trigered by focus
        wrapper.trigger("focusin");
        wrapper.vm.focusNext();
        wrapper.trigger("keydown", {
            shiftKey: true,
            ctrlKey: true
        });
        wrapper.trigger("focusin");
        const events = wrapper.emitted("browse:focus");
        expect(events.length).toBe(2);
        expect(events[0][0].target.id).toBe("first");
        expect(events[0][0].key).toBe("B");
        expect(events[0][0].shift).toBe(false);
        expect(events[0][0].ctrl).toBe(false);
        expect(events[1][0].target.id).toBe("second");
        expect(events[1][0].key).toBe("B");
        expect(events[1][0].shift).toBe(true);
        expect(events[1][0].ctrl).toBe(true);
    });
    test("Expect component blur to trigger browse:blur event", () => {
        const wrapper = shallowMount(TestComponent, {
            attachToDocument: true,
            localVue: Vue,
            slots: {
                default:
                    "<span id='first' data-browse data-browse-key='B'></span>" +
                    "<span id='second' data-browse data-browse-key='B'></span>"
            }
        });
        wrapper.vm.focus();
        wrapper.trigger("focusin");
        wrapper.vm.focusNext();
        wrapper.trigger("focusout");
        wrapper.trigger("focusin");
        document.activeElement.blur();
        wrapper.trigger("focusout");
        const events = wrapper.emitted("browse:blur");
        expect(events.length).toBe(1);
    });
    test("Expect navigation to avoid invisible elements", () => {
        // note: in tests, all elements are invisible
        setIgnoreVisibility(false);
        const wrapper = shallowMount(TestComponent, {
            localVue: Vue,
            slots: {
                default:
                    "<span id='first' data-browse data-browse-index='1'></span>" +
                    "<span id='second' data-browse data-browse-index='2'></span>" +
                    "<span id='third' data-browse data-browse-index='3'></span>" +
                    "<span id='fourth' data-browse data-browse-index='4'></span>"
            }
        });
        wrapper.vm.focus();
        expect(document.activeElement.id).toBe("first");
        wrapper.vm.focusNext();
        expect(document.activeElement.id).toBe("first");
    });
});
