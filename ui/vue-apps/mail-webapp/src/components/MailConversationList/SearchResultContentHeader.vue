<template>
    <bm-list-group class="search-result-content-header">
        <bm-list-group-item class="bg-surface pl-4">
            {{ $t("common.search") }} :
            {{ $tc("common.messages", CONVERSATION_LIST_COUNT, { CONVERSATION_LIST_COUNT }) }}
        </bm-list-group-item>
    </bm-list-group>
</template>

<script>
import { mapGetters } from "vuex";
import { BmListGroup, BmListGroupItem } from "@bluemind/ui-components";
import { CONVERSATION_LIST_COUNT } from "~/getters";

export default {
    name: "SearchResultContentHeader",
    components: {
        BmListGroup,
        BmListGroupItem
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_COUNT })
    }
};
</script>
<style lang="scss">
@import "@bluemind/ui-components/src/css/mixins/responsiveness";

.search-result-content-header {
    .list-group-item {
        justify-content: center;
    }
}
</style>
