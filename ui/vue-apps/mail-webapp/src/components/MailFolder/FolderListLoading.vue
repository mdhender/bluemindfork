<template>
    <div class="folder-list-loading">
        <p class="sr-only"><slot /></p>
        <div aria-hidden="true">
            <div v-if="hasName" class="pl-2 pt-3 pb-2 title">
                <bm-icon icon="chevron-down" size="xs" class="mr-2" />
                <span class="font-weight-bold">{{ name }}</span>
            </div>
            <div v-else class="pl-2 pt-3 pb-1 title">
                <bm-skeleton width="80%" />
            </div>
            <bm-tree :tree="tree" :has-children-property="() => false">
                <template #default="{ value: { key } }">
                    <bm-skeleton :width="key % 2 ? '50%' : '65%'" />
                </template>
            </bm-tree>
        </div>
    </div>
</template>
<script>
import { BmIcon, BmSkeleton, BmTree } from "@bluemind/ui-components";
export default {
    name: "FolderListLoading",
    components: { BmIcon, BmTree, BmSkeleton },
    props: {
        name: {
            type: String,
            default: ""
        }
    },
    data() {
        return {
            tree: Array(10)
                .fill(0)
                .map((zero, index) => ({ key: index }))
        };
    },
    computed: {
        hasName() {
            return !!this.name;
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.folder-list-loading {
    .title {
        color: $primary-fg;
    }
    .bm-tree .b-skeleton {
        line-height: $line-height-sm;
        margin: 5px 0;
    }
}
</style>
