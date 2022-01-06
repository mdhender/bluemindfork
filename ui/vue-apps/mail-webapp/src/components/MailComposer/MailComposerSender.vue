<template>
    <div class="mail-composer-sender ml-3 d-flex flex-column justify-content-between">
        <div class="d-flex align-items-center flex-fill">
            <span class="ml-2">{{ $t("common.from") }}</span>
            <bm-form-select
                :value="{ email: message.from.address, displayname: message.from.dn }"
                :options="options"
                class="ml-2 flex-fill"
                variant="inline-secondary"
                @input="
                    SET_MESSAGE_FROM({
                        messageKey: message.key,
                        from: { address: $event.email, dn: $event.displayname }
                    })
                "
            />
        </div>
        <hr class="m-0" />
    </div>
</template>

<script>
import { mapMutations, mapState } from "vuex";
import { BmFormSelect } from "@bluemind/styleguide";
import { SET_MESSAGE_FROM } from "~/mutations";

export default {
    name: "MailComposerSender",
    components: { BmFormSelect },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("root-app", ["identities"]),
        options() {
            return this.identities.map(i => ({
                text: i.displayname ? `${i.displayname} <${i.email}>` : i.email,
                value: { email: i.email, displayname: i.displayname }
            }));
        }
    },
    methods: {
        ...mapMutations("mail", { SET_MESSAGE_FROM })
    }
};
</script>
