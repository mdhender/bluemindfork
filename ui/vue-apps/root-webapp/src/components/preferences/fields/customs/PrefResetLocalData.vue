<template>
    <div>
        <div class="mb-2">{{ options.label }}</div>
        <bm-button variant="outline-warning" @click="resetLocalData">{{ options.text }}</bm-button>
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmButton } from "@bluemind/styleguide";

import PrefFieldMixin from "../../mixins/PrefFieldMixin";

export default {
    name: "PrefResetLocalData",
    components: {
        BmButton
    },
    mixins: [PrefFieldMixin],
    methods: {
        async resetLocalData() {
            // LocalStorage
            localStorage.clear();

            // Cache API
            const baseUrl = location.protocol + "//" + location.hostname + "/webapp/";
            const cacheNames = ["css-cache", "part-cache", "bm-assets", "workbox-runtime-" + baseUrl];
            cacheNames.map(name => caches.delete(name));

            // IndexedDB
            const { userId, domain } = inject("UserSession");
            const newWebmailDbName = `user.${userId}@${domain.replace(".", "_")}:webapp/mail`;
            const dbNames = [
                "capabilities",
                "context",
                "tag",
                "folder",
                "contact",
                "calendarview",
                "calendar",
                "todolist",
                "auth",
                "deferredaction",
                newWebmailDbName
            ];
            dbNames.forEach(name => {
                const deleteDBRequest = indexedDB.deleteDatabase(name);
                deleteDBRequest.onerror = () => indexedDB.deleteDatabase(name);
            });
        }
    }
};
</script>
