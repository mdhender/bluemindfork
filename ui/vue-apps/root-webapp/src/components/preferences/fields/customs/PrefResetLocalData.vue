<template>
    <div>
        <div class="mb-2">{{ options.label }}</div>
        <bm-button v-if="status === 'IDLE'" variant="outline-warning" @click="resetLocalData">
            {{ options.text }}
        </bm-button>
        <template v-else-if="status === 'LOADING'">
            <bm-spinner class="d-inline" :size="0.3" /> {{ $t("preferences.advanced.reinit_local_data.in_progress") }}
        </template>
        <bm-label-icon v-else-if="status === 'SUCCESS'" class="text-success" icon="check-circle">
            {{ $t("preferences.advanced.reinit_local_data.success") }}
        </bm-label-icon>
        <bm-label-icon v-else-if="status === 'ERROR'" class="text-danger" icon="info-circle-plain">
            {{ $t("preferences.advanced.reinit_local_data.error") }}
        </bm-label-icon>
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmButton, BmLabelIcon, BmSpinner } from "@bluemind/styleguide";

import PrefAlertsMixin from "../../mixins/PrefAlertsMixin";
import PrefFieldMixin from "../../mixins/PrefFieldMixin";

export default {
    name: "PrefResetLocalData",
    components: { BmButton, BmLabelIcon, BmSpinner },
    mixins: [PrefAlertsMixin, PrefFieldMixin],
    data() {
        return { status: "IDLE" };
    },
    methods: {
        async resetLocalData() {
            this.status = "LOADING";

            // LocalStorage
            localStorage.clear();

            // Cache API
            const baseUrl = location.protocol + "//" + location.hostname + "/webapp/";
            const cacheNames = ["css-cache", "part-cache", "bm-assets", "workbox-runtime-" + baseUrl];
            await Promise.all(cacheNames.map(name => caches.delete(name))).catch(() => {
                this.status = "ERROR";
            });

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
            let successfullDeletions = 0;
            dbNames.forEach(name => {
                const deleteDBRequest = indexedDB.deleteDatabase(name);
                deleteDBRequest.onerror = () => {
                    this.status = "ERROR";
                };
                deleteDBRequest.onsuccess = () => {
                    successfullDeletions++;
                    if (successfullDeletions === dbNames.length && this.status !== "ERROR") {
                        this.status = "SUCCESS";
                        this.showReloadAppAlert();
                    }
                };
            });
        }
    }
};
</script>
