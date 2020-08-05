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
    return store.dispatch("mail-webapp/loadUnreadCount", folder.key);
}

function getFlatFolders() {
    return store.getters["mail/MY_MAILBOX_FOLDERS"]
        .map(folderKey => store.state.mail.folders[folderKey])
        .filter(folder => folder.parent === null)
        .map(addOptions({ priority: 4 }))
        .concat(
            store.getters["mail/MAILSHARE_KEYS"]
                .map(mailshareKey => getMailshareRoot(mailshareKey, store.state.mail.folders))
                .map(addOptions({ priority: 5 }))
        );
}

function getMailshareRoot(mailshareKey, folders) {
    return Object.values(folders).find(folder => folder.mailbox === mailshareKey && !folder.parent);
}

function addOptions({ priority }) {
    return folder => ({
        ...folder,
        options: {
            priority: priority,
            id: folder.key
        }
    });
}
