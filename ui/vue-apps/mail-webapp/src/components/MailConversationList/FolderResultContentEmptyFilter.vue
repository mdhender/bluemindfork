<template>
    <div class="folder-result-content-empty-filter">
        <p v-if="CONVERSATION_LIST_UNREAD_FILTER_ENABLED">{{ $t("mail.list.unread.none") }}</p>
        <p v-if="CONVERSATION_LIST_FLAGGED_FILTER_ENABLED">{{ $t("mail.list.flagged.none") }}</p>
        <p>
            <router-link
                class="regular"
                :to="$router.relative({ name: 'v:mail:home', params: { filter: null } }, $route)"
            >
                {{ $t("mail.list.filter.remove") }}
            </router-link>
        </p>
        <bm-illustration v-if="CONVERSATION_LIST_UNREAD_FILTER_ENABLED" value="unread" size="sm" over-background />
        <bm-illustration v-if="CONVERSATION_LIST_FLAGGED_FILTER_ENABLED" value="flagged" size="sm" over-background />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { CONVERSATION_LIST_UNREAD_FILTER_ENABLED, CONVERSATION_LIST_FLAGGED_FILTER_ENABLED } from "~/getters";
import { BmIllustration } from "@bluemind/ui-components";

export default {
    name: "FolderResultContentEmptyFilter",
    components: {
        BmIllustration
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_UNREAD_FILTER_ENABLED, CONVERSATION_LIST_FLAGGED_FILTER_ENABLED })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables.scss";

.folder-result-content-empty-filter {
    display: flex;
    flex-direction: column;
    padding: $sp-8 $sp-5;
    align-items: center;
    text-align: center;
}
</style>
