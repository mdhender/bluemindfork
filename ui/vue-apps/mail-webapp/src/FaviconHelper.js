import faviconIco from "../assets/favicon.png";
/** @see https://github.com/vector-im/element-web */
import Favicon from "./Favicon";
import WebsocketClient from "@bluemind/sockjs";

const LocalStorageKeys = { FAVICON_UNREAD: "bmFaviconUnread", FOCUSED: "bmFocused" };

function handleUnreadNotifInFavicon(userSession, documentTitle) {
    // listen to some events in order to store the window focus state
    addEventListeners(["focus", "pageshow"], () => {
        setFavicon(0);
        modifyTitle(documentTitle, 0);
        // broadcast to other windows/tabs
        window.localStorage.setItem(LocalStorageKeys.FOCUSED, true);
        window.localStorage.setItem(LocalStorageKeys.FAVICON_UNREAD, 0);
    });
    addEventListeners(["blur", "pagehide"], () => window.localStorage.setItem(LocalStorageKeys.FOCUSED, false));

    // listen to the "new message" event on web socket (only one window/tab may listen)
    new WebsocketClient().register(userSession.userId + ".notifications.mails", () => {
        if (window.localStorage.getItem(LocalStorageKeys.FOCUSED) !== "true") {
            const previousUnread = Number(window.localStorage.getItem(LocalStorageKeys.FAVICON_UNREAD)) || 0;
            setFavicon(previousUnread + 1);
            modifyTitle(documentTitle, previousUnread + 1);
            // broadcast to other windows/tabs
            window.localStorage.setItem(LocalStorageKeys.FAVICON_UNREAD, previousUnread + 1);
        }
    });

    // listen to localStorage changes emitted by other windows/tabs
    addEventListeners(["storage"], ({ key, newValue }) => {
        if (key === LocalStorageKeys.FAVICON_UNREAD) {
            setFavicon(Number(newValue));
            modifyTitle(documentTitle, Number(newValue));
        }
    });
}

function setFavicon(unread = 0) {
    if (unread === 0) {
        const link = document.querySelector("link[rel*='icon']") || document.createElement("link");
        link.type = "image/x-icon";
        link.rel = "shortcut icon";
        link.href = faviconIco;
        document.getElementsByTagName("head")[0].appendChild(link);
    } else {
        new Favicon().badge(unreadDisplay(unread), {
            bgColor: "#df4355",
            isUp: true,
            fontWeight: "normal"
        });
    }
}

function modifyTitle(documentTitle, unread) {
    const prefix = unread ? "(" + unreadDisplay(unread) + ") " : "";
    document.title = prefix + documentTitle;
}

function unreadDisplay(unread) {
    return String(unread < 10 ? unread : "9+");
}

function addEventListeners(events, callback) {
    events.forEach(event => window.addEventListener(event, callback));
}

export const FaviconHelper = { handleUnreadNotifInFavicon, setFavicon };
export default FaviconHelper;
