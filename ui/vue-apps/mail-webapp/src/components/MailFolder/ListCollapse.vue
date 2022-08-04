<template>
    <div class="folder-list-collapse">
        <bm-button
            variant="inline-neutral"
            class="collapse-tree-btn d-flex align-items-center pb-2 pt-3 border-0 pl-2 w-100"
            :aria-controls="`collapse-${name}`"
            :aria-expanded="isExpanded"
            @click.stop="isExpanded = !isExpanded"
        >
            <bm-icon :icon="isExpanded ? 'caret-down' : 'caret-right'" size="xs" class="mr-2" />
            <slot name="avatar" />
            <span class="font-weight-bold text-left">{{ name }}</span>
        </bm-button>
        <bm-collapse :id="`collapse-${name}`" v-model="isExpanded">
            <slot />
        </bm-collapse>
    </div>
</template>
<script>
import { BmButton, BmCollapse, BmIcon } from "@bluemind/styleguide";

export default {
    name: "FilteredMailbox",
    components: {
        BmButton,
        BmCollapse,
        BmIcon
    },
    props: {
        name: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            isExpanded: true
        };
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.folder-list-collapse {
    .collapse-tree-btn {
        color: $primary-fg;
        &:hover {
            color: $primary-fg-hi1;
        }
    }
}
</style>
