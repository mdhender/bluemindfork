import { EditorPlugin } from "roosterjs-editor-types";
import Vue from "vue";
import debounce from "lodash.debounce";

export interface ObservablePlugin extends EditorPlugin {
    dispatch(message: { type: string; message: string }): void;
}

export class StatusBarPlugin implements ObservablePlugin {
    _proxy = Vue.observable({ message: "" });
    _resetMessage = debounce(() => {
        this._proxy.message = "";
    }, 5000);

    getName() {
        return "StatusBarPlugin";
    }

    private set message(message: string) {
        this._proxy.message = message;
        this._resetMessage();
    }

    get message() {
        return this._proxy.message.toString();
    }

    get hasMessage() {
        return Boolean(this._proxy.message);
    }

    dispatch(message: { type: string; message: string }) {
        this.message = message.message;
    }

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    initialize() {}

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    dispose() {}
}
