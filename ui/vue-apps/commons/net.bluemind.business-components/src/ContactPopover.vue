<template>
    <bm-popover triggers="click blur" :target="target" placement="bottom" no-fade>
        <resolved-contact :address="address">
            <template v-slot:default="{ resolvedContact }">
                <contact-card :contact="resolvedContact">
                    <template #email="slotProps">
                        <slot name="email" :email="slotProps.email" />
                    </template>
                </contact-card>
            </template>
        </resolved-contact>
    </bm-popover>
</template>

<script>
import { BmPopover } from "@bluemind/styleguide";
import ContactCard from "./ContactCard";
import ResolvedContact from "./ResolvedContact";

export default {
    name: "ContactPopover",
    components: { BmPopover, ContactCard, ResolvedContact },
    props: {
        address: {
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
