import "@testing-library/jest-dom";
import { TextEncoder, TextDecoder } from "util";
import crypto from "node:crypto";
import registerRequireContextHook from "babel-plugin-require-context-hook/register";
class MockResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
}
registerRequireContextHook();

window.bmcSessionInfos = { lang: "fr", sid: "", userId: "" };

global.TextDecoder = TextDecoder;
global.TextEncoder = TextEncoder;
global.URL.createObjectURL = jest.fn();
global.ResizeObserver = MockResizeObserver;

global.navigator.serviceWorker = { controller: true };

Object.defineProperty(globalThis, "crypto", {
    value: crypto.webcrypto
});
