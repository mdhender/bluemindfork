<template>
    <div class="bm-contact" :class="{ 'avatar-on-left': !noAvatar && !expandable, invalid }">
        <div v-if="transparent || noText" class="transparent-contact" :title="tooltip">
            <template v-if="!noAvatar">
                <bm-avatar :size="avatarSize" :alt="dn" :urn="contact.urn" :icon="icon" :color="avatarColor" />
            </template>
            <span v-if="!noText" :class="textClass" class="contact-main-part">
                <span :class="{ 'font-weight-bold': boldDn }">{{ dn }}</span>
                {{ showAddress ? ` ${address}` : "" }}
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
                <bm-icon-button v-if="expandable" variant="compact" size="sm" icon="plus" @click="$emit('expand')" />
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
                <bm-avatar v-if="!noAvatar" size="sm" :icon="icon" :color="avatarColor" :alt="dn" :urn="contact.urn" />
            </template>
            <span :class="textClass">
                <span :class="{ 'font-weight-bold': boldDn }">{{ dn }}</span>
                {{ showAddress ? ` ${address}` : "" }}
            </span>
        </bm-chip>
    </div>
</template>

<script>
import BmAvatar from "./BmAvatar";
import BmIconButton from "./buttons/BmIconButton";
import BmChip from "./BmChip";
import colors from "../css/exports/colors.scss";

export default {
    name: "BmContact",
    components: {
        BmAvatar,
        BmIconButton,
        BmChip
    },
    props: {
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
        invalid: {
            type: Boolean,
            default: false
        },
        selected: {
            type: Boolean,
            default: false
        },
        closeable: {
            type: Boolean,
            default: false
        },
        transparent: {
            type: Boolean,
            default: false
        },
        noText: {
            type: Boolean,
            default: false
        },
        noAvatar: {
            type: Boolean,
            default: false
        },
        // only applicable outside a contact chip, i.e. with transparent or no-text prop set to true
        avatarSize: {
            type: String,
            default: "sm"
        },
        showAddress: {
            type: Boolean,
            default: false
        },
        bold: {
            type: Boolean,
            default: false
        },
        boldDn: {
            type: Boolean,
            default: false
        },
        textTruncate: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return { groupColor: colors["blue"] };
    },
    computed: {
        address() {
            return this.contact.address ? `<${this.contact.address}>` : "";
        },
        dn() {
            if (this.invalid) {
                return this.contact.address;
            }
            return this.contact.dn || this.contact.address;
        },
        icon() {
            return this.invalid ? "exclamation" : null;
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
                return this.$t("styleguide.contact-input.invalid");
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
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "../css/_variables";

$avatar-text-gap: $sp-4;

.bm-contact {
    display: inline-flex;
    min-width: 0;

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
}
</style>
