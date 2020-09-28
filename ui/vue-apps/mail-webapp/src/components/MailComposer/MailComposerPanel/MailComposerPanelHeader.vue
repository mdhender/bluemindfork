<template>
    <div class="mail-composer-panel-header card-header d-flex justify-content-between align-items-center rounded-top">
        <span v-bm-tooltip.hover.ds500 class="text-nowrap text-truncate" :title="title">{{ title }}</span>
        <bm-button-close v-if="closeable" text-variant="light" @click="$emit('remove')" />
    </div>
</template>

<script>
import { BmButtonClose, BmTooltip } from "@bluemind/styleguide";

export default {
    name: "MailComposerPanelHeader",
    components: { BmButtonClose },
    directives: { BmTooltip },
    props: {
        title: {
            type: String,
            required: false,
            default: ""
        },
        closeable: {
            type: Boolean,
            required: false,
            default: true
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer-panel-header .close {
    // bootstrap does not adapt the shadow's color
    text-shadow: 0 1px 0 $black;

    // compensates the too big letter-spacing (dont touch letter-spacing since it modifies the click box)
    margin-right: -0.2em;
}
</style>
