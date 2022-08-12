<template>
    <div>
        <div class="mb-2">{{ label }}</div>
        <bm-button v-if="status === 'IDLE'" variant="outline-danger" size="lg" icon="broom" @click="resetLocalData">
            {{ text }}
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
import { BmButton, BmLabelIcon, BmSpinner } from "@bluemind/styleguide";
import BaseField from "../../mixins/BaseField";

export default {
    name: "PrefResetLocalData",
    components: { BmButton, BmLabelIcon, BmSpinner },
    mixins: [BaseField],
    props: {
        label: { type: String, required: true },
        text: { type: String, required: true }
    },
    data() {
        return { status: "IDLE" };
    },
    mounted() {
        navigator.serviceWorker?.addEventListener("message", this.handleResetStatus);
    },
    destroyed() {
        navigator.serviceWorker?.removeEventListener("message", this.handleResetStatus);
    },
    methods: {
        handleResetStatus({ data: { type, status } }) {
            if (type === "RESET") {
                switch (status) {
                    case "START":
                        this.status = "LOADING";
                        break;
                    case "ERROR":
                        this.status = "ERROR";
                        break;
                    case "SUCCESS":
                        this.status = "SUCCESS";
                        this.NEED_RELOAD();
                        break;
                    default:
                        this.status = "IDLE";
                }
            }
        },
        async resetLocalData() {
            const confirm = await this.$bvModal.msgBoxConfirm(this.label, {
                title: this.$t("preferences.advanced.reinit_local_data"),
                okTitle: this.text,
                cancelTitle: this.$t("common.cancel"),
                okVariant: "secondary",
                cancelVariant: "simple-neutral",
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                localStorage.clear();
                const serviceWorker = navigator.serviceWorker?.controller;
                if (serviceWorker) {
                    serviceWorker.postMessage({ type: "RESET" });
                }
            }
        }
    }
};
</script>
