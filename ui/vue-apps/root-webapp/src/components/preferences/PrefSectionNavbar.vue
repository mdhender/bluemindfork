<template>
    <div class="pref-section-navbar d-flex">
        <bm-list-group-item
            class="d-flex align-items-center px-6"
            :to="anchor(section, true)"
            @click="scrollTo(section)"
        >
            <pref-section-icon :section="section" />
        </bm-list-group-item>
        <bm-list-group-item
            v-for="category in section.categories.filter(c => c.visible)"
            :key="category.id"
            :to="anchor(category, true)"
            class="text-nowrap overflow-hidden"
            @click="scrollTo(category)"
        >
            <bm-label-icon :icon="category.icon"> {{ category.name }} </bm-label-icon>
        </bm-list-group-item>
    </div>
</template>
<script>
import Navigation from "./mixins/Navigation";
import PrefSectionIcon from "./PrefSectionIcon";
import { BmLabelIcon, BmListGroupItem } from "@bluemind/ui-components";

export default {
    name: "PrefSectionNavbar",
    components: { BmLabelIcon, BmListGroupItem, PrefSectionIcon },
    mixins: [Navigation],
    props: {
        section: {
            required: true,
            type: Object
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/_type";
@import "~@bluemind/ui-components/src/css/variables";

.pref-section-navbar {
    width: 100%;
    background-color: $surface;

    .list-group-item {
        border-bottom: none !important;
        width: auto;

        .bm-label-icon div {
            padding-left: 0.05em;
        }

        display: inline-block;
        @extend %bold;
        padding-top: $sp-4;
        padding-bottom: $sp-4;
        border-bottom: 3px solid !important;
        border-color: transparent !important;

        &.router-link-active {
            background-color: unset;
            border-color: $secondary-fg !important;
            color: $secondary-fg;
        }

        &:first-child {
            padding: 0;
        }

        &:focus {
            background-color: $neutral-bg;
        }
    }
}
</style>
