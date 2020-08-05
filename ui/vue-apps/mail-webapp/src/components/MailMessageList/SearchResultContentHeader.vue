<template>
    <bm-list-group>
        <bm-list-group-item class="font-size-lg pl-4"
            >{{ $t("common.search") }} : {{ $tc("common.messages", count, { count }) }}</bm-list-group-item
        >
    </bm-list-group>
</template>

<script>
import { mapGetters } from "vuex";
import { BmListGroup, BmListGroupItem } from "@bluemind/styleguide";
export default {
    name: "SearchResultContent",
    components: {
        BmListGroup,
        BmListGroupItem
    },
    computed: {
        ...mapGetters("mail-webapp/messages", ["count"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.font-size-lg {
    font-size: $font-size-lg;
}
</style>
