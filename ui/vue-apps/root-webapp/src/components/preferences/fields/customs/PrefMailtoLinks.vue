<template>
    <div v-if="done" class="pref-mailto-links">
        <bm-label-icon icon="check-circle" class="pl-2 text-success">
            {{ $t("preferences.mail.mailto_links.action.done") }}
        </bm-label-icon>
        <p>
            <span class="info1">{{ $t("preferences.mail.mailto_links.action.done.info.1") }}</span>
            <br />
            <span class="info2">{{ $t("preferences.mail.mailto_links.action.done.info.2") }}</span>
        </p>
    </div>
    <bm-button v-else class="pref-mailto-links" variant="link" @click="addMailtoHandler">
        {{ $t("preferences.mail.mailto_links.action") }}
    </bm-button>
</template>

<script>
import { BmButton, BmLabelIcon } from "@bluemind/ui-components";
import { ERROR } from "@bluemind/alert.store";

export default {
    name: "PrefMailtoLinks",
    components: { BmButton, BmLabelIcon },
    data() {
        return {
            done: false
        };
    },
    methods: {
        addMailtoHandler() {
            let supported = window.navigator.registerProtocolHandler;

            if (supported) {
                try {
                    window.navigator.registerProtocolHandler("mailto", "/webapp/mail/%s", "Mailto Handler");
                } catch {
                    supported = false;
                }
            }

            if (!supported) {
                this.$store.dispatch(`alert/${ERROR}`, {
                    alert: { uid: "PrefMailtoLinks", name: "preferences.mail.mailto_links.action" },
                    options: { area: "pref-right-panel" }
                });
            } else {
                this.done = true;
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables.scss";

.pref-mailto-links {
    .info1 {
        color: $neutral-fg-hi1;
    }
    .info2 {
        color: $neutral-fg-lo1;
    }
}
</style>
