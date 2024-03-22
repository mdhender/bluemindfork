<script setup>
import { BmButton } from "@bluemind/ui-components";

function reloadApp() {
    location.reload();
}
</script>

<template>
    <div class="timezone-changed-alert">
        {{ $t("alert.preferences.timezone_changed_alert.warning") }}
        <bm-button variant="link" @click="reloadApp()">{{ $t("common.reload_now") }}</bm-button>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.timezone-changed-alert {
    display: flex;
    gap: $sp-4;
}
</style>
