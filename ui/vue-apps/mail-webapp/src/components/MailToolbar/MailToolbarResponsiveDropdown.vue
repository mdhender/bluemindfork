<template>
    <div class="mail-toolbar-responsive-dropdown mail-toolbar-item" :class="{ compact }">
        <bm-toolbar-icon-dropdown
            ref="icon-dropdown"
            :variant="compact ? 'compact' : 'compact-on-fill-primary'"
            size="lg"
            v-bind="[$attrs, $props]"
            :title="title ? title : label"
            v-on="$listeners"
        >
            <slot />
        </bm-toolbar-icon-dropdown>
        <bm-toolbar-captioned-icon-dropdown
            ref="captioned-icon-dropdown"
            v-bind="[$attrs, $props]"
            :caption="label"
            v-on="$listeners"
        >
            <slot />
        </bm-toolbar-captioned-icon-dropdown>
    </div>
</template>

<script>
import { BmToolbarIconDropdown, BmToolbarCaptionedIconDropdown } from "@bluemind/ui-components";

export default {
    name: "MailToolbarResponsiveDropdown",
    components: { BmToolbarIconDropdown, BmToolbarCaptionedIconDropdown },
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
        },
        compact: {
            type: Boolean,
            default: false
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
<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";

.mail-toolbar-responsive-dropdown {
    &.compact {
        .bm-captioned-icon-dropdown {
            display: none;
        }
    }
    &:not(.compact) {
        @include from-lg {
            .bm-icon-dropdown {
                display: none;
            }
        }
    }
    @include until-lg {
        .bm-captioned-icon-dropdown {
            display: none;
        }
    }
}
</style>
