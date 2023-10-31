import { mount } from "@vue/test-utils";
import BmFileDropZone from "../../src/components/BmFileDropZone";

const shouldBeActive = () => true;
const shouldNotBeActive = () => false;

describe("BmFileDropZone", () => {
    let wrapper, wrapperNotActive, dragEventProps, dropEventProps, dragEventNoFilesProps;

    beforeEach(() => {
        wrapper = mount({
            template: `
                <div class="outerDiv">
                    <bm-file-drop-zone text="dropZoneText" @drop-files="files = $event" :should-activate-fn="shouldBeActive">
                        <div class="innerDiv">DropZone</div>
                    </bm-file-drop-zone>
                </div>`,
            components: { BmFileDropZone },
            data() {
                return { files: [] };
            },
            methods: { shouldBeActive }
        });
        wrapperNotActive = mount({
            template: `
                <div class="outerDiv">
                    <bm-file-drop-zone text="dropZoneText" @drop-files="files = $event" :should-activate-fn="shouldNotBeActive">
                        <div class="innerDiv">DropZone</div>
                    </bm-file-drop-zone>
                </div>`,
            components: { BmFileDropZone },
            data() {
                return { files: [] };
            },
            methods: { shouldNotBeActive }
        });
        dragEventProps = {
            dataTransfer: {
                types: ["application/x-moz-file", "Files"],
                items: [
                    { kind: "file", type: "image/png" },
                    { kind: "file", type: "" }
                ],
                files: null
            }
        };
        dropEventProps = {
            dataTransfer: {
                types: ["application/x-moz-file", "Files"],
                items: [
                    {
                        kind: "file",
                        type: "image/png",
                        webkitGetAsEntry: jest.fn(() => {
                            return { isFile: true };
                        }),
                        getAsFile: jest.fn(() => {
                            return { name: "myImage.png", size: 123456789 };
                        })
                    },
                    {
                        kind: "file",
                        type: "",
                        webkitGetAsEntry: jest.fn(() => {
                            return { isFile: false };
                        }),
                        getAsFile: jest.fn(() => {
                            return { name: "myFolder" };
                        })
                    }
                ],
                files: [{ name: "myImage.png", size: 123456789 }, { name: "myFolder" }]
            }
        };
        dragEventNoFilesProps = Object.assign({}, dragEventProps, {
            dataTransfer: { types: ["text/html"] }
        });

        initBoundaries(wrapper);
    });

    test("Is a Vue instance", () => {
        expect(wrapper.find(".bm-file-drop-zone").vm).toBeTruthy();
    });

    test("Drag-over outside the drop zone should enable the 'active' style", async () => {
        const event = new Event("dragover");
        Object.assign(event, dragEventProps);
        event.x = 90;
        event.y = 90;
        document.dispatchEvent(event);
        await wrapper.vm.$nextTick();
        expect(wrapper.contains(".bm-dropzone-active")).toBeTruthy();
        expect(wrapper.contains(".bm-dropzone-hover")).toBeFalsy();
    });

    test("Drag-over outside the drop zone without files should not enable the 'active' style", async () => {
        const event = new Event("dragenter");
        Object.assign(event, dragEventNoFilesProps);
        event.x = 90;
        event.y = 90;
        document.dispatchEvent(event);
        await wrapper.vm.$nextTick();
        expect(wrapper.contains(".bm-dropzone-active")).toBeFalsy();
        expect(wrapper.contains(".bm-dropzone-hover")).toBeFalsy();
    });

    test("Drag-over inside the drop zone should enable the 'hover' style", async () => {
        const event = new Event("dragover");
        Object.assign(event, dragEventProps);
        event.x = 50;
        event.y = 50;
        document.dispatchEvent(event);
        await wrapper.vm.$nextTick();
        expect(wrapper.contains(".bm-dropzone-active")).toBeFalsy();
        expect(wrapper.contains(".bm-dropzone-hover")).toBeTruthy();
    });

    test("Drag-over inside the drop zone  without files should not enable the 'hover' style", async () => {
        const event = new Event("dragover");
        Object.assign(event, dragEventNoFilesProps);
        event.x = 50;
        event.y = 50;
        document.dispatchEvent(event);
        await wrapper.vm.$nextTick();
        expect(wrapper.contains(".bm-dropzone-active")).toBeFalsy();
        expect(wrapper.contains(".bm-dropzone-hover")).toBeFalsy();
    });

    test("Drop files outside the drop zone should not emit the 'drop-files' event", async () => {
        const event = new Event("drop");
        Object.assign(event, dropEventProps);
        document.dispatchEvent(event);
        await wrapper.vm.$nextTick();
        expect(wrapper.find(".bm-file-drop-zone").emitted()["drop-files"]).toBeFalsy();
    });

    test("Drop files inside the drop zone should emit the 'drop-files' event", async () => {
        // first, let's go over the drop zone
        const overEvent = new Event("dragover");
        Object.assign(overEvent, dragEventProps);
        overEvent.x = 50;
        overEvent.y = 50;
        document.dispatchEvent(overEvent);
        await wrapper.vm.$nextTick();

        wrapper.find(".innerDiv").trigger("drop", dropEventProps);
        await wrapper.vm.$nextTick();
        const expectedFiles = [{ name: "myImage.png", size: 123456789 }];
        expect(wrapper.find(".bm-file-drop-zone").emitted()["drop-files"]).toEqual([[expectedFiles]]);
        expect(wrapper.vm._data.files).toEqual(expectedFiles);
    });

    test("Drag-over outside the drop zone when shouldBeActive callback returns false should not enable the 'active' style", async () => {
        initBoundaries(wrapperNotActive);

        const event = new Event("dragover");
        Object.assign(event, dragEventProps);
        event.x = 90;
        event.y = 90;
        document.dispatchEvent(event);
        await wrapperNotActive.vm.$nextTick();
        expect(wrapperNotActive.contains(".bm-dropzone-active")).toBeFalsy();
        expect(wrapperNotActive.contains(".bm-dropzone-hover")).toBeFalsy();
    });

    test("Drop files inside the drop zone when shouldBeActive callback returns false should not emit the 'drop-files' event", async () => {
        // first, let's go over the drop zone
        const overEvent = new Event("dragover");
        Object.assign(overEvent, dragEventProps);
        overEvent.x = 50;
        overEvent.y = 50;
        document.dispatchEvent(overEvent);
        await wrapperNotActive.vm.$nextTick();

        wrapperNotActive.find(".innerDiv").trigger("drop", dropEventProps);
        await wrapperNotActive.vm.$nextTick();
        expect(wrapper.find(".bm-file-drop-zone").emitted()["drop-files"]).toBeFalsy();
    });

    test("Drag-over over an inline dropzone should not enable the 'active' style", async () => {
        wrapper.destroy();
        wrapper = mount({
            template: `
                <div class="outerDiv">
                    <bm-file-drop-zone text="dropZoneText" @drop-files="files = $event" inline>
                        <div class="innerDiv">DropZone</div>
                    </bm-file-drop-zone>
                </div>`,
            components: { BmFileDropZone },
            data() {
                return { files: [] };
            }
        });
        initBoundaries(wrapper);

        const event = new Event("dragover");
        Object.assign(event, dragEventProps);
        event.x = 50;
        event.y = 50;
        document.dispatchEvent(event);
        await wrapper.vm.$nextTick();
        expect(wrapper.contains(".bm-dropzone-active")).toBeFalsy();
        expect(wrapper.contains(".bm-dropzone-hover")).toBeFalsy();
        expect(wrapper.contains(".bm-dropzone-inline-hover")).toBeTruthy();
    });
});

function initBoundaries(wrapper) {
    const compo = wrapper.find(".bm-file-drop-zone");
    compo.vm.$el.getBoundingClientRect = jest.fn(() => {
        return { x: 20, y: 20, width: 60, height: 60 };
    });
}
