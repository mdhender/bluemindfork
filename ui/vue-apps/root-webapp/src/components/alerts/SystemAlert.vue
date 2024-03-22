<template>
    <bm-button
        v-if="alert.payload.link"
        class="system-alert flex-fill text-left font-weight-normal"
        variant="text"
        @click="openLink(alert.payload.link)"
    >
        {{ alert.payload.message }}
    </bm-button>
    <span v-else> {{ alert.payload.message }}</span>
</template>
<script>
import { BmButton } from "@bluemind/ui-components";
export default {
    name: "SystemAlert",
    components: { BmButton },
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    methods: {
        openLink(link) {
            window.open(link);
        }
    }
};
</script>
