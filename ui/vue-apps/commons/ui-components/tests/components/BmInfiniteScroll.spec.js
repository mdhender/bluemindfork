import { mount } from "@vue/test-utils";
import BmInfiniteScroll from "../../src/components/BmInfiniteScroll";
jest.useFakeTimers();

function getDatas() {
    let tab = [];
    for (let i = 0; i < 1000; i++) {
        tab.push(i);
    }
    return tab;
}

describe("BmInfiniteScroll", () => {
    let datas = getDatas();
    let firstItemSelector = ".bm-infinite-scroll .items > div:first-child";
    let scrollbarSelector = ".bm-infinite-scroll .scroller-y";

    function defaultMount() {
        return mount(BmInfiniteScroll, {
            propsData: {
                items: datas,
                position: 0,
                total: datas.length,
                //Dynamic and static item size always read offsetHeight at 0px...
                itemSize: "10px"
            }
        });
    }

    function mountWithSlot(slot) {
        return mount(BmInfiniteScroll, {
            propsData: {
                items: datas,
                position: 0,
                total: datas.length,
                scrollbar: true,
                //Dynamic and static item size always read offsetHeight at 0px...
                itemSize: "10px"
            },
            scopedSlots: {
                item: slot
            }
        });
    }

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("an items prop is available and taken into account", () => {
        const wrapper = defaultMount();
        expect(wrapper.props().items).toEqual(getDatas());
        expect(wrapper.find(firstItemSelector).text()).toContain(getDatas()[0]);
    });

    test("a goto method is available", async () => {
        const wrapper = mount(BmInfiniteScroll, {
            propsData: {
                items: datas,
                total: datas.length,
                //Dynamic and static item size always read offsetHeight at 0px...
                itemSize: "10px"
            }
        });

        wrapper.vm.goto(10, true);
        await wrapper.vm.$nextTick();
        await wrapper.find(scrollbarSelector).trigger("scroll");
        expect(wrapper.find(firstItemSelector).text()).toContain("20");
        expect(wrapper.find(firstItemSelector).html()).toContain("order: 10;");
    });

    test("a scrollbar prop is available and taken into account", () => {
        // default : no scrollbar
        const wrapperWithoutScrollbar = defaultMount();
        expect(wrapperWithoutScrollbar.props().scrollbar).toBe(false);
        expect(wrapperWithoutScrollbar.find(scrollbarSelector).html()).toContain("scrollbar-hidden");

        // with scrollbar
        const wrapperWithScrollbar = mount(BmInfiniteScroll, {
            propsData: {
                items: datas,
                position: 0,
                total: datas.length,
                scrollbar: true
            }
        });
        expect(wrapperWithScrollbar.props().scrollbar).toBe(true);
        expect(wrapperWithScrollbar.find(scrollbarSelector).html()).not.toContain("scrollbar-hidden");
    });

    test("no scrollbar displayed if no total prop", () => {
        const wrapper = mount(BmInfiniteScroll, {
            propsData: {
                items: datas,
                position: 0,
                scrollbar: true
            }
        });
        expect(wrapper.props().scrollbar).toBe(true);
        expect(wrapper.props().total).toBe(Infinity);
        expect(wrapper.find(scrollbarSelector).html()).toContain("scrollbar-hidden");
    });

    test("item slot works", () => {
        const wrapper = mountWithSlot('<p slot-scope="foo">Hello world {{ foo.item }}</p>');
        expect(wrapper.find(firstItemSelector).text()).toBe("Hello world 0");
    });

    // CAN'T TEST SCROLL, MAYBE DUE TO NO HEIGHT SET BY PARENT
    test("scroll displays more items", async () => {
        const wrapper = mountWithSlot('<p style="height: 10px" slot-scope="foo">Hello world {{ foo.item }}</p>');
        expect(wrapper.find(".bm-infinite-scroll").text()).not.toContain("Hello world 40");
        await wrapper.vm.$nextTick();
        wrapper.find(scrollbarSelector).element.scrollTop = 39 * 10;
        await wrapper.find(scrollbarSelector).trigger("scroll");
        expect(wrapper.find(".bm-infinite-scroll").text()).toContain("Hello world 40");
    });

    // CAN'T TEST IT DUE TO AN ERROR --> "TypeError: _vm.$t is not a function"
    // test("loading slot works", () => {
    //     let d = [1, 2, 3, 4, 5, 6];
    //     const wrapper = mount(BmInfiniteScroll, {
    //         propsData: {
    //             items: d,
    //             total: 10
    //         },
    //         slots: {
    //             loading: '<p>Item is loading.</p>'
    //         }
    //     });
    //     expect(wrapper.vm).toBeTruthy();
    //     expect(wrapper.find(".itemBox:nth-of-type(9)").text()).toBe('Item is loading');
    // });
});
