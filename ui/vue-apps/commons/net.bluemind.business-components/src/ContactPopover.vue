<template>
    <bm-popover triggers="click blur" :target="target" placement="bottom" no-fade custom-class="contact-card-popover">
        <resolved-contact :recipient="recipient">
            <template v-slot:default="{ resolvedContact }">
                <contact-card :contact="resolvedContact">
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
import { BmPopover } from "@bluemind/ui-components";
import ContactCard from "./ContactCard";
import ResolvedContact from "./ResolvedContact";

export default {
    name: "ContactPopover",
    components: { BmPopover, ContactCard, ResolvedContact },
    props: {
        recipient: {
            type: String,
            required: true
        },
        target: {
            type: String,
            required: true
        }
    }
};
</script>
