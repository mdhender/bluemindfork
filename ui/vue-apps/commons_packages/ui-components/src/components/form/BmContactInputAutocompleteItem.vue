<template>
    <div class="bm-contact-input-autocomplete-item align-items-center d-flex h-100">
        <div
            class="d-flex align-items-center flex-fill py-4 pl-4 pr-2"
            :class="{ 'non-deletable': isGroupWithoutAddress }"
        >
            <div>
                <bm-avatar
                    size="sm"
                    :alt="dn"
                    :urn="contact.urn"
                    :icon="isGroup ? 'users2' : undefined"
                    :color="isGroup ? groupColor : undefined"
                />
            </div>
            <div class="px-5">
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div class="contact-dn text-truncate" v-html="boldIt(dn)" />
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div class="text-muted text-truncate caption" v-html="address" />
            </div>
            <div class="ml-auto contact-kind"><bm-icon :icon="contact.kind === 'group' ? 'users2' : 'user'" /></div>
        </div>
        <div v-if="!isGroupWithoutAddress" class="h-100 p-0 delete-autocomplete">
            <bm-button-close
                class="h-100 w-100"
                size="sm"
                :title="$t('styleguide.contact-input.autocomplete.delete')"
                @click.stop="$emit('delete')"
            />
        </div>
    </div>
</template>

<script>
import { VCard } from "@bluemind/addressbook.api";
import { normalize } from "@bluemind/string";
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
        },
        isGroup() {
            return this.contact.kind === VCard.Kind.group;
        },
        isGroupWithoutAddress() {
            return this.isGroup && !this.contact.address;
        }
    },
    methods: {
        boldIt(str) {
            if (!str) {
                return "";
            }

            const tokens = this.inputValue.split(/\s+/);
            const uniqueTokens = [];

            tokens.forEach(token => {
                if (token?.length) {
                    const uniqueToken = normalize(token);
                    if (!uniqueTokens.includes(uniqueToken)) {
                        uniqueTokens.push(uniqueToken);
                        const normalizedStr = normalize(str);
                        let start = normalizedStr.indexOf(uniqueToken);
                        if (start !== -1) {
                            let result = str.slice(0, start);
                            result += "<strong>";
                            result += str.slice(start, uniqueToken.length + start);
                            result += "</strong>";
                            result += str.slice(uniqueToken.length + start, str.length);
                            str = result;
                        }
                    }
                }
            });

            return str;
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/variables.scss";

$delete-btn-width: 1.25rem;
.bm-contact-input-autocomplete-item {
    .contact-dn {
        color: $neutral-fg-hi1;
        margin-bottom: $sp-2;
    }
    .delete-autocomplete {
        visibility: hidden;
        width: $delete-btn-width;
        border-left: 1px solid $surface;
    }
    .non-deletable {
        margin-right: $delete-btn-width;
    }
    &:hover .delete-autocomplete {
        visibility: visible;
    }
}
</style>
