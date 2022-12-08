import { PluginEventType } from "roosterjs-editor-types";

export default class BmEditorEventListener {
    constructor(richEditorRoot) {
        this.richEditorRoot = richEditorRoot;
        this.handleEventTypes = [PluginEventType.Input, PluginEventType.ContentChanged];
    }

    getName() {
        return "BmEditorEventListener";
    }

    initialize(editor) {
        this.editor = editor;
    }

    dispose() {
        this.editor = null;
    }

    onPluginEvent(event) {
        if (this.handleEventTypes.includes(event.eventType)) {
            const eventName = PluginEventType[event.eventType].toLowerCase();
            this.richEditorRoot.dispatchEvent(new Event(eventName));
        }
    }
}
