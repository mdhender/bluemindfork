import "@testing-library/jest-dom";
import { TextEncoder, TextDecoder } from "util";
import registerRequireContextHook from "babel-plugin-require-context-hook/register";

registerRequireContextHook();

window.bmcSessionInfos = { lang: "fr", sid: "", userId: "" };

global.TextDecoder = TextDecoder;
global.TextEncoder = TextEncoder;
