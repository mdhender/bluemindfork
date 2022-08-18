<template>
    <bm-list-group-separator class="conversation-list-separator">
        <div class="text-right bold">
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
.list-group-separator.conversation-list-separator {
    background-color: $neutral-bg;
    color: $neutral-fg;
    border-color: $neutral-fg-lo2;
    padding: base-px-to-rem(2) $sp-4;
}
</style>
