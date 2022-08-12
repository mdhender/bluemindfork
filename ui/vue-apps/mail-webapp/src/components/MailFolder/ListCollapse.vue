<template>
    <div class="folder-list-collapse">
        <bm-button
            variant="text"
            class="collapse-tree-btn pl-2 w-100"
            size="sm"
            :aria-controls="`collapse-${name}`"
            :aria-expanded="isExpanded"
            :icon="isExpanded ? 'chevron' : 'chevron-right'"
            @click.stop="isExpanded = !isExpanded"
        >
            <slot name="avatar" />{{ name }}
        </bm-button>
        <bm-collapse :id="`collapse-${name}`" v-model="isExpanded">
            <slot />
        </bm-collapse>
    </div>
</template>
<script>
import { BmButton, BmCollapse } from "@bluemind/styleguide";

export default {
    name: "FilteredMailbox",
    components: {
        BmButton,
        BmCollapse
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
