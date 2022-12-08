<template>
    <bm-list-group>
        <bm-list-group-item class="pl-4">
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
    name: "SearchResultContent",
    components: {
        BmListGroup,
        BmListGroupItem
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_COUNT })
    }
};
</script>
