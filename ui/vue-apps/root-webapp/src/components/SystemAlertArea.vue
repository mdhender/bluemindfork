<template>
    <bm-alert-area class="system-alert-area p-0 m-0 z-index-750" :alerts="alerts" @remove="$emit('remove')">
        <template v-slot="context">
            <bm-button
                v-if="context.alert.link"
                class="w-100 text-left font-weight-normal"
                variant="text"
                @click="openLink(context.alert.link)"
            >
                {{ context.alert.message }}
            </bm-button>
            <span v-else> {{ context.alert.message }}</span>
        </template>
    </bm-alert-area>
</template>

<script>
import { BmAlertArea, BmButton } from "@bluemind/styleguide";
import { AlertTypes } from "@bluemind/alert.store";

export default {
    name: "SystemAlertArea",
    components: { BmAlertArea, BmButton },
    props: {
        systemAlerts: {
            type: Array,
            required: true
        }
    },
    data() {
        return {
            alerts: []
        };
    },
    created() {
        this.alerts = this.systemAlerts.map((announcement, index) => {
            return {
                uid: index,
                type: getAlertType(announcement.kind),
                message: announcement.message,
                dismissible: announcement.closeable,
                link: announcement.link
            };
        });
    },
    methods: {
        openLink(link) {
            window.open(link);
        }
    }
};

function getAlertType(kind) {
    switch (kind) {
        case "Error":
            return AlertTypes.ERROR;
        case "Info":
            return AlertTypes.INFO;
        case "Warn":
            return AlertTypes.WARNING;
    }
}
</script>

<style>
.system-alert-area {
    position: absolute;
    top: 0;
    left: 0;
    min-width: 100%;
}
</style>
