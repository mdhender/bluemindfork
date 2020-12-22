<template>
    <div class="folder-list-loading">
        <p class="sr-only"><slot /></p>
        <div aria-hidden="true">
            <div v-if="hasName" class="pl-2 pt-3 pb-2 text-info-dark">
                <bm-icon icon="caret-down" size="sm" class="bm-icon mr-2" />
                <span class="font-weight-bold">{{ name }}</span>
            </div>
            <div v-else class="pl-2 pt-3 pb-1 text-info-dark">
                <span class="font-weight-bold loading-placeholder d-inline-block"></span>
            </div>
            <bm-tree :tree="tree" :has-children-property="() => false">
                <template v-slot="{ value: { size } }">
                    <div :style="{ width: 60 / size + '%' }" class="loading-placeholder"></div>
                </template>
            </bm-tree>
        </div>
    </div>
</template>
<script>
import { BmIcon, BmTree } from "@bluemind/styleguide";
export default {
    name: "FolderListLoading",
    components: { BmIcon, BmTree },
    props: {
        name: {
            type: String,
            required: false,
            default: ""
        }
    },
    data() {
        return {
            tree: Array(10)
                .fill(0)
                .map((zero, index) => ({ key: index, size: Math.floor(Math.random() * Math.floor(3)) }))
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
@import "~@bluemind/styleguide/css/_variables";

@keyframes pulse {
    0% {
        background-color: rgba(165, 165, 165, 0.1);
    }
    50% {
        background-color: rgba(165, 165, 165, 0.2);
    }
    100% {
        background-color: rgba(165, 165, 165, 0.1);
    }
}

.folder-list-loading .bm-tree .loading-placeholder {
    min-height: 18px !important;
    margin: $sp-1 0;
}

.folder-list-loading span.loading-placeholder {
    min-height: 1rem;
    min-width: 15em;
}

.loading-placeholder {
    background-color: $extra-light;
    color: transparent;
    flex: auto 0.3 0 !important;
    animation: pulse 3s infinite ease-in-out;
}
</style>
