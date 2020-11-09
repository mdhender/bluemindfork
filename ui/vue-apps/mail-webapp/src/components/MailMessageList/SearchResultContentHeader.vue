<template>
    <bm-list-group>
        <bm-list-group-item class="font-size-lg pl-4">
            {{ $t("common.search") }} : {{ $tc("common.messages", MESSAGE_LIST_COUNT, { MESSAGE_LIST_COUNT }) }}
        </bm-list-group-item>
    </bm-list-group>
</template>

<script>
import { mapGetters } from "vuex";
import { BmListGroup, BmListGroupItem } from "@bluemind/styleguide";
import { MESSAGE_LIST_COUNT } from "~getters";

export default {
    name: "SearchResultContent",
    components: {
        BmListGroup,
        BmListGroupItem
    },
    computed: {
        ...mapGetters("mail", { MESSAGE_LIST_COUNT })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.font-size-lg {
    font-size: $font-size-lg;
}
</style>
