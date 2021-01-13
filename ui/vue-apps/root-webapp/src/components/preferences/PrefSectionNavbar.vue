<template>
    <div class="pref-section-navbar d-flex">
        <bm-list-group-item :to="sectionPath(section.code)" @click="scrollTo(sectionId(section.code))">
            <bm-app-icon :icon-app="section.icon" class="text-primary" />
        </bm-list-group-item>
        <bm-list-group-item
            v-for="category in section.categories"
            :key="section.code + category.code"
            :to="sectionPath(section.code, category.code)"
            @click="scrollTo(sectionId(section.code, category.code))"
        >
            <h2 class="d-inline-block py-2 pr-2">
                <bm-label-icon :icon="category.icon" icon-size="lg"> {{ category.name }} </bm-label-icon>
            </h2>
        </bm-list-group-item>
    </div>
</template>
<script>
import { BmLabelIcon, BmListGroupItem } from "@bluemind/styleguide";
import BmAppIcon from "../BmAppIcon";
import PrefMixin from "../../mixins/PrefMixin";

export default {
    name: "PrefSectionNavbar",
    components: {
        BmLabelIcon,
        BmAppIcon,
        BmListGroupItem
    },
    mixins: [PrefMixin],
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
        &.active {
            background-color: unset;
            & > h2 {
                border-color: $primary !important;
                color: $primary;
            }
        }

        &:first-child {
            padding: 0;

            // a total width of 4rem, should be used as for the content's left margin
            .bm-app-icon svg {
                width: 2rem;
                height: 2rem;
                margin: 0 1rem 0 1rem;
            }
        }
    }
}
</style>
