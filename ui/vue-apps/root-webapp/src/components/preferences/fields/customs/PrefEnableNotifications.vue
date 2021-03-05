<template>
    <bm-form-checkbox v-if="notifPermission === 'default'" @input="onChange">
        {{ options.label_enable_checkbox }}
    </bm-form-checkbox>
    <div v-else-if="notifPermission === 'granted'">{{ options.label_enabled }}</div>
    <div v-else>{{ options.label_disabled }}</div>
</template>

<script>
import { BmFormCheckbox } from "@bluemind/styleguide";

import PrefFieldMixin from "../../mixins/PrefFieldMixin";

export default {
    name: "PrefEnableNotifications",
    components: {
        BmFormCheckbox
    },
    mixins: [PrefFieldMixin],
    data() {
        return { notifPermission: Notification.permission };
    },
    methods: {
        onChange(checked) {
            if (checked) {
                Notification.requestPermission().then(permission => {
                    this.notifPermission = permission;
                });
            }
        }
    }
};
</script>
