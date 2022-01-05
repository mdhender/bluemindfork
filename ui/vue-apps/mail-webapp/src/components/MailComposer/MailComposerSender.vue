<template>
    <div class="mail-composer-sender ml-3 d-flex flex-column justify-content-between">
        <div class="d-flex align-items-center">
            <span class="ml-2">{{ $t("common.from") }}</span>
            <bm-form-select v-model="selected" :options="options" class="ml-2 flex-fill" variant="inline-secondary" />
        </div>
        <hr class="m-0" />
    </div>
</template>

<script>
import { BmFormSelect } from "@bluemind/styleguide";

export default {
    name: "MailComposerSender",
    components: { BmFormSelect },
    data() {
        return { value: undefined };
    },
    computed: {
        selected: {
            get() {
                return this.value || this.defaultOption.value;
            },
            set(selected) {
                this.value = selected;
            }
        },
        // TODO replace by getter when ready
        defaultIdentity() {
            return { address: "bbb@bbb.com", dn: "Miss B" };
        },
        // TODO replace by getter when ready
        identities() {
            return [
                { address: "aaa@aaa.com", dn: "Mister A" },
                { address: "bbb@bbb.com", dn: "Miss B" }
            ];
        },
        defaultOption() {
            return this.options.find(o => o.value.address === this.defaultIdentity.address);
        },
        options() {
            return this.identities.map(i => ({ text: i.dn ? `${i.dn} <${i.address}>` : i.address, value: i }));
        }
    }
};
</script>
