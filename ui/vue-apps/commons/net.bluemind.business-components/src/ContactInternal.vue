<template>
    <div class="contact" :class="{ 'no-avatar': noAvatar }">
        <a
            :id="uniqueId"
            class="btn btn-link"
            role="button"
            :class="{ 'avatar-on-left': !noAvatar && !expandable, invalid, 'text-truncate': textTruncate }"
            tabindex="0"
            @click="showContactCard = !showContactCard"
            @keypress.enter="showContactCard = !showContactCard"
            @keydown.tab.exact="showContactCard ? focusPopover() : undefined"
            @keydown.tab.shift="showContactCard ? (showContactCard = false) : undefined"
        >
            <div v-if="transparent || noText" class="transparent-contact" :title="tooltip">
                <template v-if="!noAvatar">
                    <bm-avatar
                        :size="avatarSize"
                        :alt="dn || address"
                        :urn="contact.urn"
                        :icon="icon"
                        :color="avatarColor"
                    />
                </template>
                <span v-if="!noText" :class="textClass" class="contact-main-part">
                    <span :class="{ 'font-weight-bold': boldDn }">{{ dn }}</span>
                    {{ showAddress || !dn ? ` ${address}` : "" }}
                </span>
            </div>
            <bm-chip
                v-else-if="contact.kind === 'group'"
                size="lg"
                :selected="selected"
                :title="tooltip"
                :closeable="closeable"
                @remove="$emit('remove')"
            >
                <template #left-part>
                    <bm-icon-button
                        v-if="expandable"
                        variant="compact"
                        size="sm"
                        icon="plus"
                        @click.stop="$emit('expand')"
                    />
                    <bm-avatar size="sm" icon="group" :color="groupColor" />
                </template>
                <span :class="{ ...textClass, 'font-weight-bold': bold || boldDn }">{{ dn }}</span>
            </bm-chip>
            <bm-chip
                v-else
                size="lg"
                :selected="selected"
                :title="tooltip"
                :closeable="closeable"
                @remove="$emit('remove')"
            >
                <template #left-part>
                    <bm-avatar
                        v-if="!noAvatar"
                        size="sm"
                        :icon="icon"
                        :color="avatarColor"
                        :alt="dn || address"
                        :urn="contact.urn"
                    />
                </template>
                <span :class="textClass">
                    <span :class="{ 'font-weight-bold': boldDn }">{{ dn }}</span>
                    {{ showAddress || !dn ? ` ${address}` : "" }}
                </span>
            </bm-chip>
        </a>
        <resolved-contact v-if="enableCard && !invalid" :resolve="showContactCard" :recipient="recipient">
            <template #default="{ resolvedContact }">
                <div>
                    <contact-popover
                        ref="contact-popover"
                        :target="uniqueId"
                        :contact="resolvedContact"
                        :show.sync="showContactCard"
                    >
                        <template #email="slotProps">
                            <slot name="email" :email="slotProps.email" />
                        </template>
                        <template #actions="slotProps">
                            <slot name="actions" :contact="slotProps.contact" />
                        </template>
                    </contact-popover>
                    <contact-modal :contact="resolvedContact" :show.sync="showContactCard">
                        <template #email="slotProps">
                            <slot name="email" :email="slotProps.email" />
                        </template>
                        <template #actions="slotProps">
                            <slot name="actions" :contact="slotProps.contact" />
                        </template>
                    </contact-modal>
                </div>
            </template>
        </resolved-contact>
    </div>
</template>

<script>
import { BmAvatar, BmChip, BmIconButton } from "@bluemind/ui-components";
import ContactModal from "./ContactModal";
import ContactPopover from "./ContactPopover";
import ResolvedContact from "./ResolvedContact";

export default {
    name: "ContactInternal",
    components: { BmAvatar, BmIconButton, BmChip, ContactModal, ContactPopover, ResolvedContact },
    props: {
        // only applicable outside a contact chip, i.e. with transparent or no-text prop set to true
        avatarSize: { type: String, default: "sm" },
        bold: { type: Boolean, default: false },
        boldDn: { type: Boolean, default: false },
        closeable: { type: Boolean, default: false },
        contact: {
            type: Object,
            default() {
                return {
                    address: "",
                    dn: "",
                    kind: "individual",
                    members: [],
                    urn: ""
                };
            }
        },
        invalid: { type: Boolean, default: false },
        invalidIcon: { type: String, default: "exclamation" },
        invalidTooltip: {
            type: String,
            default: function () {
                return this.$t("styleguide.contact-input.invalid");
            }
        },
        noAvatar: { type: Boolean, default: false },
        noText: { type: Boolean, default: false },
        enableCard: { type: Boolean, default: false },
        selected: { type: Boolean, default: false },
        showAddress: { type: Boolean, default: false },
        textTruncate: { type: Boolean, default: true },
        transparent: { type: Boolean, default: false }
    },
    data() {
        return { groupColor: "#0095fa", showContactCard: false };
    },
    computed: {
        address() {
            return this.contact.address ? (this.contact.dn ? `<${this.contact.address}>` : this.contact.address) : "";
        },
        dn() {
            if (this.invalid) {
                return this.contact.address;
            }
            return this.contact.dn || "";
        },
        recipient() {
            return this.contact.dn
                ? this.contact.dn + (this.contact.address ? ` <${this.contact.address}>` : "")
                : this.contact.address;
        },
        icon() {
            return this.invalid ? this.invalidIcon : null;
        },
        avatarColor() {
            return this.invalid ? "var(--fill-danger-bg)" : null;
        },
        expandable() {
            return this.contact.members?.length > 0;
        },
        textClass() {
            return {
                "font-weight-bold": this.bold,
                "pl-2": this.noAvatar && !this.transparent,
                "text-truncate": this.textTruncate
            };
        },
        tooltip() {
            if (this.invalid) {
                return this.invalidTooltip;
            }
            const tips = [];
            if (this.contact.address) {
                tips.push(this.contact.address);
            }
            const count = this.contact.members?.length;
            if (count > 0) {
                tips.push(this.$tc("styleguide.contact-input.members", count, { count }));
            }
            return tips.join(", ");
        },
        uniqueId() {
            return `contact-${this._uid}`;
        }
    },
    methods: {
        focusPopover() {
            this.$refs["contact-popover"].focus();
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/variables.scss";

$avatar-text-gap: $sp-4;

.contact {
    display: inline-flex;

    .transparent-contact {
        display: inline-flex;
        align-items: center;
        min-width: 0;
    }

    .bm-avatar {
        & + .contact-main-part {
            margin-left: $avatar-text-gap;
        }
    }

    .bm-chip .chip-left-part {
        display: flex;

        .bm-icon-button {
            outline-offset: base-px-to-rem(-4);
        }
    }

    .bm-chip .chip-main-part {
        padding-left: $avatar-text-gap;
    }

    &.avatar-on-left .bm-chip {
        $avatar-half-width: math.div($avatar-width-sm, 2);

        .chip-left-part {
            background: none;
            border-radius: 0;
            margin-right: -$avatar-half-width;

            .bm-avatar {
                margin-right: 0;
            }
        }

        .chip-main-part {
            padding-left: $avatar-half-width + $avatar-text-gap;
        }
    }

    &.invalid {
        .contact-main-part,
        .bm-chip .chip-main-part {
            color: $danger-fg-hi1;
        }
    }

    a.btn {
        text-decoration: none;
    }
}
</style>
