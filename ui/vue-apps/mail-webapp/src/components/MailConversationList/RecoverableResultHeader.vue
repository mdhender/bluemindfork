<script setup>
import { BmButton } from "@bluemind/ui-components";
const route = { name: "v:mail:home", params: { filter: null } };
</script>

<template>
    <div class="recoverable-result-header d-none d-lg-block">
        <bm-button icon="arrow-back" variant="text" :to="$router.relative(route, $route)">
            {{ $t("mail.recoverable.back") }}
        </bm-button>
    </div>
</template>
<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables.scss";

.recoverable-result-header {
    text-align: center;
}
</style>
