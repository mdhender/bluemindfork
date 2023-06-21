import { logger } from "workbox-core/_private";
import { registerRoute } from "workbox-routing";
import { NetworkFirst } from "workbox-strategies";
import BrowserData from "../BrowserData";
import Session from "../session";

export default function () {
    registerRoute(/session-infos.js/, fetchSessionInfo);
}

async function fetchSessionInfo(event) {
    try {
        await BrowserData.resetIfNeeded(await Session.infos());
    } catch (e) {
        logger.error("[SW][BrowserData] Fail to reset browser data");
        logger.error(e);
    }
    return new NetworkFirst().handle(event);
}
