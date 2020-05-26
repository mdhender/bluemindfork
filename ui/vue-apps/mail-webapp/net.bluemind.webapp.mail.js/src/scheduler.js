import store from "@bluemind/store";
import Bottleneck from "bottleneck";

export default {
    startUnreadCountersUpdater(interval = 10 * 60 * 1000) {
        const limiter = new Bottleneck({
            id: "unreadcountersupdaters",
            minTime: 250,
            maxConcurrent: 1,
            reservoir: 20,
            reservoirRefreshAmount: 20,
            reservoirRefreshInterval: 10 * 1000
        });

        limiter.on("error", function(err) {
            console.error("[bottleneck] something was wrong", err);
        });

        return window.setInterval(() => updateUnreadCounters(limiter), interval);
    },
    stopUnreadCountersUpdater(id) {
        window.clearInterval(id);
    }
};

function updateUnreadCounters(limiter) {
    try {
        const folders = getFlatFolders();
        Promise.all(
            folders.map(folder => {
                return limiter.schedule(folder.options, fetchPerUserUnread, folder);
            })
        );
    } catch (err) {
        throw new Error("[SCHEDULER] an error occured", err);
    }
}

function fetchPerUserUnread(folder) {
    return store.dispatch("mail-webapp/loadUnreadCount", folder.uid);
}

function getFlatFolders() {
    return store.getters["mail-webapp/my"].folders
        .map(addOptions({ priority: 4 }))
        .concat(
            store.getters["mail-webapp/mailshares"].flatMap(mailbox => mailbox.folders.map(addOptions({ priority: 5 })))
        );
}

function addOptions({ priority }) {
    return folder => ({
        ...folder,
        options: {
            priority: priority,
            id: folder.uid
        }
    });
}
