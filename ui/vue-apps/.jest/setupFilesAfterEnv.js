import "@testing-library/jest-dom";
import { TextEncoder, TextDecoder } from "util";
import crypto from "node:crypto";
import registerRequireContextHook from "babel-plugin-require-context-hook/register";

registerRequireContextHook();

window.bmcSessionInfos = { lang: "fr", sid: "", userId: "" };

global.TextDecoder = TextDecoder;
global.TextEncoder = TextEncoder;
<<<<<<< HEAD

=======
>>>>>>> 206766ffa0d (FEATWEBML-2602 Feat: add X-Bm-Events Insert)
global.URL.createObjectURL = jest.fn();
global.navigator.serviceWorker = { controller: true };

Object.defineProperty(globalThis, "crypto", {
    value: crypto.webcrypto
});
