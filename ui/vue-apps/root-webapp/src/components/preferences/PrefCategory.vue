<template>
    <div :id="anchor(category)" class="pref-category">
        <div class="category-heading">
            <bm-icon :icon="category.icon" />
            <h2>{{ category.name }}</h2>
        </div>
        <template v-for="(group, index) in category.groups">
            <pref-group
                v-if="group.visible"
                :key="group.id"
                :group="group"
                :no-heading="index === 0 && group.name === category.name"
            />
        </template>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";
import Navigation from "./mixins/Navigation";
import PrefGroup from "./PrefGroup";

export default {
    name: "PrefCategory",
    components: { BmIcon, PrefGroup },
    mixins: [Navigation],
    props: {
        category: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/variables";

.pref-category {
    @include from-lg {
        padding-left: $sp-7;
    }
    padding-bottom: $sp-5;

    .category-heading {
        display: flex;
        align-items: start;
        gap: $sp-5;
        padding-top: $sp-6;
        padding-bottom: $sp-6;

        color: $secondary-fg-hi1;

        .bm-icon {
            @mixin set-icon-size($size-name) {
                $size: map-get($icon-sizes, $size-name);
                width: $size;
                height: $size;
            }
            @include set-icon-size("lg");
            @include from-lg {
                @include set-icon-size("xl");
            }
        }

        h2 {
            margin: 0;
        }
    }
}
</style>
