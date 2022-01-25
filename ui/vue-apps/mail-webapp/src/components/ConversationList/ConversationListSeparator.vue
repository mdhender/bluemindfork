<template>
    <bm-list-group-separator class="px-2 py-0">
        <div class="text-right text-muted font-weight-bold">
            {{ text }}
        </div>
    </bm-list-group-separator>
</template>

<script>
import { BmListGroupSeparator } from "@bluemind/styleguide";
export default {
    name: "ConversationListSeparator",
    components: {
        BmListGroupSeparator
    },
    props: {
        text: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.list-group-separator {
    padding: $sp-1;
}
</style>
