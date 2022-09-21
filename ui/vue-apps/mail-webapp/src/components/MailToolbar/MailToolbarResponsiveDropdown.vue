<template>
    <div class="mail-toolbar-responsive-dropdown mail-toolbar-item">
        <bm-icon-dropdown
            ref="icon-dropdown"
            class="d-inline-flex d-lg-none"
            variant="compact-on-fill-primary"
            size="lg"
            v-bind="[$attrs, $props]"
            :title="title ? title : label"
            v-on="$listeners"
        >
            <slot />
        </bm-icon-dropdown>
        <bm-captioned-icon-dropdown
            ref="captioned-icon-dropdown"
            class="d-none d-lg-inline-flex"
            v-bind="[$attrs, $props]"
            :caption="label"
            v-on="$listeners"
        >
            <slot />
        </bm-captioned-icon-dropdown>
    </div>
</template>

<script>
import { BmIconDropdown, BmCaptionedIconDropdown } from "@bluemind/styleguide";

export default {
    name: "MailToolbarResponsiveDropdown",
    components: { BmIconDropdown, BmCaptionedIconDropdown },
    props: {
        icon: {
            type: String,
            required: true
        },
        label: {
            type: String,
            required: true
        },
        title: {
            type: String,
            default: null
        }
    },
    methods: {
        displayedDropdown() {
            return window.getComputedStyle(this.$refs["icon-dropdown"].$el).display === "none"
                ? this.$refs["captioned-icon-dropdown"]
                : this.$refs["icon-dropdown"];
        },
        show(bvEvent) {
            this.displayedDropdown().show(bvEvent);
        },
        hide(bvEvent) {
            this.displayedDropdown().hide(bvEvent);
        }
    }
};
</script>
