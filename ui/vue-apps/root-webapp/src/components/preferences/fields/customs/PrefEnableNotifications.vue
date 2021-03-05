<template>
    <div>
        <div class="mb-2">{{ $t("preferences.advanced.notifications.explanations") }}</div>
        <bm-button v-if="notifPermission === 'default'" variant="outline-secondary" @click="onClick">
            {{ $t("preferences.advanced.notifications.enable_checkbox") }}
        </bm-button>
        <div v-else-if="notifPermission === 'granted'">
            <span class="text-primary">{{ $t("preferences.advanced.notifications.enabled") }}</span>
            {{ $t("preferences.advanced.notifications.how_to_disable") }}
        </div>
        <div v-else>
            <span class="text-warning">{{ $t("preferences.advanced.notifications.disabled") }}</span>
            {{ $t("preferences.advanced.notifications.how_to_enable") }}
        </div>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/styleguide";

import PrefFieldMixin from "../../mixins/PrefFieldMixin";

export default {
    name: "PrefEnableNotifications",
    components: { BmButton },
    mixins: [PrefFieldMixin],
    data() {
        return { notifPermission: Notification.permission };
    },
    methods: {
        onClick(checked) {
            if (checked) {
                Notification.requestPermission().then(permission => {
                    this.notifPermission = permission;
                });
            }
        }
    }
};
</script>
