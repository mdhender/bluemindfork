<script setup>
import { onMounted, onUnmounted, ref } from "vue";
import { ItemFlag } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import store from "@bluemind/store";
import { useBus } from "@bluemind/vue-bus";
import { BmButton, BmSpinner } from "@bluemind/ui-components";

const count = ref(0);
const loading = ref(true);
const bus = useBus();
const updateCount = async (address = "mail-webapp/pushed_folder_changes", uid = store.state.mail.activeFolder) => {
    if (uid === store.state.mail.activeFolder) {
        count.value = (await inject("MailboxItemsPersistence", uid).count({ must: [ItemFlag.Deleted] })).total;
    }
};
onMounted(async () => {
    await updateCount();
    loading.value = false;
    bus.$on("mail-webapp/pushed_folder_changes", updateCount);
});
onUnmounted(() => bus.$off("mail-webapp/pushed_folder_changes", updateCount));
</script>

<template>
    <div class="trash-result-header">
        <span>{{ $t("mail.list.recoverable.header") }}</span>
        <br />
        <bm-button :disabled="count === 0" variant="text-accent" :loading="loading" to="/">
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
    padding: $sp-5 $sp-5 0;
    color: $neutral-fg;
    @include regular;
}
</style>
