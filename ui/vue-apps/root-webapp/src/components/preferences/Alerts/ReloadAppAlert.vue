<template>
    <div class="reload-app-alert">
        {{ $t("alert.preferences.reload_app.warning") }}
        <bm-button variant="link" @click="reloadApp()">{{ $t("common.reload_now") }}</bm-button>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/styleguide";

export default {
    name: "ReloadAppAlert",
    components: { BmButton },
    methods: {
        reloadApp() {
            location.reload();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.reload-app-alert {
    display: flex;
    gap: $sp-4;
}
</style>
