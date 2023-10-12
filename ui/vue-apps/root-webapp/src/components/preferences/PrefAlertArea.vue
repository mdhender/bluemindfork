<template>
    <bm-alert-area v-if="alerts.length > 0" :alerts="alerts" stackable @remove="REMOVE">
        <template #default="context"><component :is="context.alert.renderer" :alert="context.alert" /></template>
    </bm-alert-area>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { REMOVE } from "@bluemind/alert.store";
import { BmAlertArea } from "@bluemind/ui-components";

import NeedReconnectionAlert from "./Alerts/NeedReconnectionAlert";
import NotValidAlert from "./Alerts/NotValidAlert";
import ReloadAppAlert from "./Alerts/ReloadAppAlert";
import SaveErrorAlert from "./Alerts/SaveErrorAlert";

export default {
    name: "PrefAlertArea",
    components: {
        BmAlertArea,
        NeedReconnectionAlert,
        NotValidAlert,
        ReloadAppAlert,
        SaveErrorAlert
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "pref-right-panel") })
    },
    methods: {
        ...mapActions("alert", { REMOVE })
    }
};
</script>
