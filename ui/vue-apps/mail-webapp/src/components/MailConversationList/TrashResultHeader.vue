<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { ItemFlag } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import router from "@bluemind/router";
import store from "@bluemind/store";
import { useBus } from "@bluemind/vue-bus";
import { BmButton, BmSpinner } from "@bluemind/ui-components";

const count = ref();
const loading = computed(() => count.value === undefined);
const bus = useBus();
const updateCount = async (address = "mail-webapp/pushed_folder_changes", uid = store.state.mail.activeFolder) => {
    if (uid === store.state.mail.activeFolder) {
        count.value = (await inject("MailboxItemsPersistence", uid).count({ must: [ItemFlag.Deleted] })).total;
    }
};
onMounted(async () => {
    await updateCount();
    bus.$on("mail-webapp/pushed_folder_changes", updateCount);
});
onUnmounted(() => bus.$off("mail-webapp/pushed_folder_changes", updateCount));
const route = { name: "v:mail:home", params: { filter: "deleted" } };
</script>

<template>
    <div class="trash-result-header">
        <bm-button :disabled="!count" variant="text-accent" :loading="loading" :to="$router.relative(route, $route)">
            <template v-if="loading">
                {{ $t("mail.list.recoverable.loading") }}
            </template>
            <template v-else>
                {{ $tc("mail.list.recoverable.link", count, { count }) }}
            </template>
        </bm-button>
    </div>
</template>
<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables.scss";
@import "@bluemind/ui-components/src/css/utils/typography.scss";

.trash-result-header {
    text-align: center;
    color: $neutral-fg;
    @include regular;
    > .bm-button {
        max-width: 100%;
    }
}
</style>
