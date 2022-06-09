<template>
    <div class="pref-section-navbar d-flex">
        <bm-list-group-item class="d-flex align-items-center" :to="anchor(section, true)" @click="scrollTo(section)">
            <pref-section-icon :section="section" />
        </bm-list-group-item>
        <bm-list-group-item
            v-for="category in section.categories.filter(c => c.visible)"
            :key="category.id"
            :to="anchor(category, true)"
            @click="scrollTo(category)"
        >
            <h2 class="d-inline-block py-2 pr-2">
                <bm-label-icon :icon="category.icon" icon-size="lg"> {{ category.name }} </bm-label-icon>
            </h2>
        </bm-list-group-item>
    </div>
</template>
<script>
import Navigation from "./mixins/Navigation";
import PrefSectionIcon from "./PrefSectionIcon";
import { BmLabelIcon, BmListGroupItem } from "@bluemind/styleguide";

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
@import "~@bluemind/styleguide/css/_variables";

.pref-section-navbar {
    width: 100%;
    background-color: #fff;

    .list-group-item {
        border-bottom: none !important;
        width: auto;
        padding: 0 0.5em 0 0;

        .bm-label-icon div {
            padding-left: 0.05em;
        }

        & > h2 {
            border-bottom: 3px solid !important;
            border-color: transparent !important;
        }
        &.router-link-active {
            background-color: unset;
            & > h2 {
                border-color: $secondary-fg !important;
                color: $secondary-fg;
            }
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
