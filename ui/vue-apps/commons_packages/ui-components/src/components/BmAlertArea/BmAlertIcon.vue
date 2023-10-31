<template>
    <div class="bm-alert-icon">
        <bm-icon v-if="icon(alert)" :icon="icon(alert)" :class="['text-' + variant(alert)]" size="lg" />
        <p class="sr-only">{{ $t("styleguide.alert.type." + alert.type.toLowerCase()) }}</p>
    </div>
</template>

<script>
import { AlertTypes } from "@bluemind/alert.store";
import AlertMixin from "./mixin";
import BmIcon from "../BmIcon";

export default {
    name: "BmAlertIcon",
    components: {
        BmIcon
    },
    mixins: [AlertMixin],
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    methods: {
        icon({ icon, type }) {
            if (icon) {
                return icon;
            }
            switch (type) {
                case AlertTypes.LOADING:
                    return "";
                case AlertTypes.SUCCESS:
                    return "check-circle";
                case AlertTypes.INFO:
                    return "info-circle-plain";
                case AlertTypes.WARNING:
                case AlertTypes.ERROR:
                    return "exclamation-circle";
            }
        }
    }
};
</script>
