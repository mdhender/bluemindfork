<template>
    <div>
        <bm-button variant="none" :aria-label="$tc('mail.actions.send.aria')" @click="send()">
            <bm-icon icon="send" size="2x" />
            {{ $tc("mail.actions.send") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.actions.save.aria')">
            <bm-icon icon="save" size="2x" />
            {{ $tc("mail.actions.save") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.actions.remove.compose.aria')">
            <bm-icon icon="trash" size="2x" />
            {{ $tc("mail.actions.remove") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.actions.attach.aria')">
            <bm-icon icon="paper-clip" size="2x" />
            {{ $tc("mail.actions.attach") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import { mapActions } from "vuex";

export default {
    name: "MailToolbarComposeMessage",
    components: {
        BmButton,
        BmIcon
    },
    methods: {
        ...mapActions("mail-webapp", ["send"])
    }
};
</script>
