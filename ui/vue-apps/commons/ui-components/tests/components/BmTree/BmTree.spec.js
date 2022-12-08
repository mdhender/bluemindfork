import { mount } from "@vue/test-utils";
import BmTree from "../../../src/components/BmTree";
import cloneDeep from "lodash.clonedeep";
import { setIgnoreVisibility } from "../../../src/mixins/BrowsableContainer";

const exampleData = [
    {
        label: "Level 0",
        id: "4",
        expanded: false,
        hasChildren: true,
        children: [
            {
                label: "Level 1",
                id: "12",
                expanded: false,
                hasChildren: true,
                children: [
                    {
                        label: "Level 2",
                        id: "26",
                        expanded: false,
                        hasChildren: false
                    }
                ]
            }
        ]
    },
    {
        label: "Level 0 bis",
        id: "42",
        expanded: false,
        hasChildren: false
    }
];

describe("BmTree", () => {
    let wrapper;
    let toggleSelector = ".bm-button-expand";

    beforeEach(() => {
        wrapper = mount(BmTree, {
            propsData: {
                tree: exampleData,
                idProperty: "id"
            }
        });
    });

    test("is a Vue instance", () => {
        expect(wrapper.vm).toBeTruthy();
    });

    test("BmTree should match snapshot", () => {
        expect(wrapper.vm.$el).toMatchSnapshot();
    });

    test("a tree prop is required", () => {
        //eslint-disable-next-line no-console
        console.error = jest.fn();
        mount(BmTree, {
            propsData: {
                idProperty: "id"
            }
        });
        //eslint-disable-next-line no-console
        expect(console.error).toBeCalledWith(expect.stringContaining('Missing required prop: "tree"'));
    });

    test("BmTree emit a toggle event when expanding or collapsing", async () => {
        const toggleButton = wrapper.find(toggleSelector);
        await toggleButton.trigger("click");
        expect(wrapper.emitted().toggle).toBeTruthy();

        await toggleButton.trigger("click");
        expect(wrapper.emitted().toggle).toBeTruthy();
    });

    test("BmTreeNode children are in DOM only when parent is expanded", async () => {
        expect(wrapper.html()).not.toContain("Level 1");
        await wrapper.find(toggleSelector).trigger("click");
        expect(wrapper.html()).toContain("Level 1");
    });

    test("BmTreeNode must update its internal expanded data when props change", async () => {
        expect(wrapper.html()).not.toContain("Level 1");
        let duplicatedExampleData = cloneDeep(exampleData);
        duplicatedExampleData[0].expanded = true;
        wrapper.setProps({ tree: duplicatedExampleData });
        await wrapper.vm.$nextTick();
        expect(wrapper.html()).toContain("Level 1");
    });

    test("BmTreeNode must be toggled correctly if no expanded prop defined ", async () => {
        const exampleData = [
            {
                label: "Level 0",
                id: "4",
                hasChildren: true,
                children: [
                    {
                        label: "Level 1",
                        id: "12"
                    }
                ]
            }
        ];
        wrapper = mount(BmTree, {
            propsData: {
                tree: exampleData,
                idProperty: "id"
            }
        });
        expect(wrapper.html()).not.toContain("Level 1");
        const toggleButton = wrapper.find(toggleSelector);
        await toggleButton.trigger("click");
        expect(wrapper.html()).toContain("Level 1");
        await toggleButton.trigger("click");
        expect(wrapper.html()).not.toContain("Level 1");
    });

    test("Click on a node content triggers click event with nodeId", () => {
        wrapper.find(".bm-tree-node-content").trigger("click"); // click on first node
        expect(wrapper.emitted("select")).toBeTruthy();
        expect(wrapper.emitted("select")[0]).toEqual(["4"]); // id of first node
    });

    test("Click on a BmTreeNode makes it display as selected", async () => {
        expect(wrapper.html()).not.toContain("bm-tree-node-active");
        await wrapper.find(".bm-tree-node-content").trigger("click"); // click on first node
        expect(wrapper.vm.$data.selected_).toEqual("4");
        expect(wrapper.html()).toContain("bm-tree-node-active");
        expect(wrapper.find(".bm-tree-node-active").text()).toEqual("Level 0");
    });

    test("BmTreeNode expands/collapse dont trigger click event", async () => {
        await wrapper.find(toggleSelector).trigger("click");
        expect(wrapper.emitted("select")).not.toBeTruthy();
        await wrapper.find(toggleSelector).trigger("click");
        expect(wrapper.emitted("select")).not.toBeTruthy();
    });

    test("Selected node can be initialized or updated by a prop but it's managed internally otherwise", async () => {
        const duplicatedExampleData = cloneDeep(exampleData);
        duplicatedExampleData[0].expanded = true;
        duplicatedExampleData[0].children[0].expanded = true;
        wrapper = mount(BmTree, {
            propsData: {
                tree: duplicatedExampleData,
                idProperty: "id",
                selected: "12"
            }
        });
        expect(wrapper.vm.$data.selected_).toEqual("12");
        expect(wrapper.find(".bm-tree-node-active").text()).toEqual("Level 1");

        await wrapper.find(".bm-tree-node-content").trigger("click"); // click on first node
        expect(wrapper.vm.$data.selected_).toEqual("4");
        expect(wrapper.find(".bm-tree-node-active").text()).toEqual("Level 0");

        wrapper.setProps({ selected: "26" });
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.$data.selected_).toEqual("26");
        expect(wrapper.find(".bm-tree-node-active").text()).toEqual("Level 2");
    });

    test("BmTreeNode expands/collapse with right/left arrows", async () => {
        await wrapper.find(".bm-tree-node").trigger("keydown.right");
        expect(wrapper.emitted().toggle).toBeTruthy();
        await wrapper.find(".bm-tree-node").trigger("keydown.left");
        expect(wrapper.emitted().toggle).toBeTruthy();
    });

    test("Navigate in BmTree using up/down arrows", async () => {
        setIgnoreVisibility(true);

        // select first node
        await wrapper.find(".bm-tree-node-content").trigger("click");
        wrapper.find(".bm-tree-node-content").element.focus();
        expect(document.activeElement.innerHTML).toContain("Level 0");

        // go down
        await wrapper.find(".bm-tree").trigger("keydown.down");
        expect(document.activeElement.innerHTML).toContain("Level 0 bis");

        // go up
        await wrapper.find(".bm-tree").trigger("keydown.up");
        expect(document.activeElement.innerHTML).toContain("Level 0");

        // expand and go down
        expect(wrapper.html()).not.toContain("Level 1");
        await wrapper.find(".bm-tree-node").trigger("keydown.right");
        expect(wrapper.html()).toContain("Level 1");
        await wrapper.find(".bm-tree").trigger("keydown.down");
        expect(document.activeElement.innerHTML).toContain("Level 1");

        // expand and go down
        await wrapper.findAll(".bm-tree-node").at(1).trigger("keydown.right");
        await wrapper.find(".bm-tree").trigger("keydown.down");
        expect(document.activeElement.innerHTML).toContain("Level 2");

        // go down
        await wrapper.find(".bm-tree").trigger("keydown.down");
        expect(document.activeElement.innerHTML).toContain("Level 0 bis");

        // go up
        await wrapper.find(".bm-tree").trigger("keydown.up");
        expect(document.activeElement.innerHTML).toContain("Level 2");

        // go up
        await wrapper.find(".bm-tree").trigger("keydown.up");
        expect(document.activeElement.innerHTML).toContain("Level 1");

        // go up
        await wrapper.find(".bm-tree").trigger("keydown.up");
        expect(document.activeElement.innerHTML).toContain("Level 0");

        setIgnoreVisibility(false);
    });
});
