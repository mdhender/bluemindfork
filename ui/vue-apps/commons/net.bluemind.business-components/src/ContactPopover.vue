<template>
    <bm-popover
        triggers="manual"
        :show="show"
        :target="target"
        placement="bottom"
        no-fade
        custom-class="contact-card-popover"
        @hidden="focusTarget"
    >
        <global-events
            @keyup.esc="show && $emit('update:show', false)"
            @click="show && !isMouseOver($event) ? $emit('update:show', false) : undefined"
        />
        <resolved-contact :recipient="recipient">
            <template v-slot:default="{ resolvedContact }">
                <contact-card ref="contact-card" :contact="resolvedContact" tabindex="0" @focusout.native="onFocusOut">
                    <template #email="slotProps">
                        <slot name="email" :email="slotProps.email" />
                    </template>
                    <template #actions="slotProps">
                        <slot name="actions" :contact="slotProps.contact" />
                    </template>
                </contact-card>
            </template>
        </resolved-contact>
    </bm-popover>
</template>

<script>
import GlobalEvents from "vue-global-events";
import { BmPopover } from "@bluemind/ui-components";
import ContactCard from "./ContactCard";
import ResolvedContact from "./ResolvedContact";

export default {
    name: "ContactPopover",
    components: { BmPopover, ContactCard, GlobalEvents, ResolvedContact },
    props: {
        recipient: { type: String, required: true },
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
        onFocusOut(event) {
            if (!this.$refs["contact-card"]?.$el.contains(event?.relatedTarget)) {
                this.$emit("update:show", false);
            }
        },
        focusTarget() {
            document.getElementById(this.target)?.focus();
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
