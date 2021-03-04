<template>
    <div>
        <div class="mb-4">{{ options.label }}</div>
        <bm-button variant="primary" @click="resetLocalData">{{ options.text }}</bm-button>
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmButton } from "@bluemind/styleguide";

import PrefFieldMixin from "../../mixins/PrefFieldMixin";

export default {
    name: "PrefFieldCheck",
    components: {
        BmButton
    },
    mixins: [PrefFieldMixin],
    methods: {
        async resetLocalData() {
            // LocalStorage
            window.localStorage.clear();

            // Cache API
            const baseUrl = "https://webmail-master.loc/webapp/";
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
            dbNames.map(name => indexedDB.deleteDatabase(name));
        }
    }
};
</script>
