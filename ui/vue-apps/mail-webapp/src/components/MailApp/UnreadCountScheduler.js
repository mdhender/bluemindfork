import { GROUP_MAILBOX_ROOT_FOLDERS, MAILSHARE_ROOT_FOLDERS, MY_MAILBOX_ROOT_FOLDERS } from "~/getters";
import { UNREAD_FOLDER_COUNT } from "~/actions";

const LIMIT = 5;
const INTERVAL = 60 * 1 * 1000;
let id;
function clear() {
    if (id) {
        window.clearInterval(id);
    }
}
export default {
    created() {
        clear();
        id = window.setInterval(() => {
            if (!fetchPendingUnreadCounts(this.$store)) {
                clear();
            }
        }, INTERVAL);
    },
    destroyed() {
        clear();
    }
};

async function fetchPendingUnreadCounts(store) {
    let folders = [
        ...store.getters[`mail/${MAILSHARE_ROOT_FOLDERS}`],
        ...store.getters[`mail/${GROUP_MAILBOX_ROOT_FOLDERS}`],
        ...store.getters[`mail/${MY_MAILBOX_ROOT_FOLDERS}`]
    ];
    let limit = LIMIT;
    for (let i = 0; i < folders.length && limit; i++) {
        if (folders[i].unread === undefined) {
            await store.dispatch(`mail/${UNREAD_FOLDER_COUNT}`, folders[i]);
            limit--;
        }
    }
    return !limit;
}
