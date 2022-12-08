<template>
    <div class="bm-contact-input-autocomplete-item align-items-center d-flex h-100">
        <div class="d-flex align-items-center flex-fill py-4 pl-4 pr-2">
            <div>
                <bm-avatar
                    size="sm"
                    :alt="dn"
                    :urn="contact.urn"
                    :icon="contact.kind === 'group' ? 'group' : undefined"
                    :color="contact.kind === 'group' ? groupColor : undefined"
                />
            </div>
            <div class="flew-grow-1 px-5">
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div class="contact-dn text-truncate" v-html="boldIt(dn)" />
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div class="text-muted text-truncate caption" v-html="address" />
            </div>
            <div class="ml-auto"><bm-icon :icon="contact.kind === 'group' ? 'group' : 'user'" /></div>
        </div>
        <div class="h-100 p-0 delete-autocomplete">
            <bm-button-close
                class="h-100 w-100 border-left border-white text-center"
                size="sm"
                :title="$t('styleguide.contact-input.autocomplete.delete')"
                @click.stop="$emit('delete')"
            />
        </div>
    </div>
</template>

<script>
import BmAvatar from "../BmAvatar";
import BmButtonClose from "../buttons/BmButtonClose";
import BmIcon from "../BmIcon";
import colors from "../../css/exports/colors.scss";

export default {
    name: "BmContactInputAutocompleteItem",
    components: {
        BmAvatar,
        BmButtonClose,
        BmIcon
    },
    props: {
        contact: { type: Object, required: true },
        inputValue: { type: String, required: true }
    },
    data() {
        return { groupColor: colors["blue"] };
    },
    computed: {
        dn() {
            return this.contact.dn || this.contact.address;
        },
        address() {
            const count = this.contact.members?.length;
            return (
                this.boldIt(this.contact.address) || this.$tc("styleguide.contact-input.members", count || 1, { count })
            );
        }
    },
    methods: {
        boldIt(str) {
            if (!str) {
                return "";
            }
            let begin = str.toLowerCase().indexOf(this.inputValue.toLowerCase());
            if (begin !== -1) {
                let result = str.slice(0, begin);
                result += "<strong>";
                result += str.slice(begin, this.inputValue.length + begin);
                result += "</strong>";
                result += str.slice(this.inputValue.length + begin, str.length);
                return result;
            }
            return str;
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables.scss";

.bm-contact-input-autocomplete-item {
    .contact-dn {
        color: $neutral-fg-hi1;
        margin-bottom: $sp-2;
    }
    .delete-autocomplete {
        visibility: hidden;
        width: 1.25rem;
    }
    &:hover .delete-autocomplete {
        visibility: visible;
    }
}
</style>
