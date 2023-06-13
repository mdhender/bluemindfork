<template>
    <bm-popover
        triggers="manual"
        :show="show"
        :target="target"
        placement="bottom"
        no-fade
        custom-class="contact-card-popover scroller-y"
        @hidden="focusTarget"
    >
        <global-events
            @keyup.esc="show && isVisible() && $emit('update:show', false)"
            @click="show && isVisible() && !isMouseOver($event) ? $emit('update:show', false) : undefined"
        />
        <contact-card v-focus-out :contact="contact" @focusout="isVisible() && $emit('update:show', false)">
            <template #email="slotProps">
                <slot name="email" :email="slotProps.email" />
            </template>
            <template #actions="slotProps">
                <slot name="actions" :contact="slotProps.contact" />
            </template>
        </contact-card>
    </bm-popover>
</template>

<script>
import GlobalEvents from "vue-global-events";
import { BmPopover } from "@bluemind/ui-components";
import ContactCard from "./ContactCard";
import FocusOut from "./FocusOut";

export default {
    name: "ContactPopover",
    components: { BmPopover, ContactCard, GlobalEvents },
    directives: { FocusOut },
    props: {
        contact: { type: Object, default: undefined },
        show: { type: Boolean, default: false },
        target: { type: String, required: true }
    },
    methods: {
        focus() {
            document.body.querySelector(".contact-card-popover").focus();
        },
        isMouseOver(mouseEvent) {
            return (
                mouseOver(mouseEvent, document.body.querySelector(".contact-card-popover")) ||
                mouseOver(mouseEvent, document.getElementById(this.target))
            );
        },
        focusTarget() {
            document.getElementById(this.target)?.focus();
        },
        isVisible() {
            return document.body.querySelector(".contact-card-popover")?.getClientRects().length;
        }
    }
};

function mouseOver(mouseEvent, element) {
    const pos = element?.getClientRects()[0];
    return (
        pos &&
        mouseEvent.clientX >= pos.left &&
        mouseEvent.clientX <= pos.right &&
        mouseEvent.clientY >= pos.top &&
        mouseEvent.clientY <= pos.bottom
    );
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.contact-card-popover {
    @include until-lg {
        display: none !important;
    }
    max-width: 50vw !important;
    min-width: $popover-min-width;
    max-height: 65vh;

    .arrow {
        display: none;
    }

    div:focus,
    .contact-card-body a:focus {
        outline-width: 1px !important;
        outline-style: dashed !important;
        outline-color: var(--neutral-fg);
    }
}
</style>
