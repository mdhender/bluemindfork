<template>
    <div :class="'bm-avatar bm-avatar-' + size" :title="title">
        <svg
            id="hexagon"
            version="1.1"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 100 120"
            aria-hidden="true"
            role="img"
        >
            <g v-bm-clipping="clip">
                <rect x="0" y="0" width="100" height="120" :fill="bg" />
                <image
                    v-if="hasPhoto"
                    x="-7"
                    y="0"
                    width="120"
                    height="120"
                    :xlink:href="url"
                    @error="invalidUrl = true"
                />
            </g>
        </svg>
        <div v-if="!hasPhoto" id="overlay">
            <bm-icon v-if="hasIcon" :icon="icon" :size="iconSize" />
            <p v-else-if="hasCount" id="count" :class="{ 'count-overflows': countOverflows }">{{ boundedCount }}</p>
            <p v-else id="letter">{{ letter }}</p>
        </div>
        <div v-if="hasStatus" id="status" :style="{ backgroundColor: status.color }" />
    </div>
</template>

<script>
import MakeUniq from "../mixins/MakeUniq";
import colors from "../css/exports/avatar.scss";
import BmClipping from "../directives/BmClipping";
import BmIcon from "./BmIcon";

const backgrounds = Object.values(colors);
const URL = "api/addressbooks/{container}/{item}/photo";
const countLimit = 99;

export default {
    name: "BmAvatar",
    directives: { BmClipping },
    components: { BmIcon },
    mixins: [MakeUniq],
    props: {
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["sm", "md", "xl"].includes(value);
            }
        },
        alt: {
            type: String,
            default: ""
        },
        urn: {
            type: String,
            default: ""
        },
        location: {
            type: String,
            default: "/"
        },
        status: {
            type: Object,
            default: null
        },
        icon: {
            type: String,
            default: null
        },
        color: {
            type: String,
            default: null
        },
        count: {
            type: Number,
            default: null
        }
    },
    data() {
        return { invalidUrl: false };
    },
    computed: {
        bg() {
            if (this.count !== null) {
                return "var(--neutral-fg)";
            }
            return this.color || backgrounds[this.hash % backgrounds.length];
        },
        letter() {
            const str = this.alt.trimStart();
            if (str.length > 0) {
                return str[0].toUpperCase();
            }
            return "-";
        },
        hash() {
            var hash = 0,
                i,
                chr;
            if (this.alt.length === 0) {
                return hash;
            }
            for (i = 0; i < this.alt.length; i++) {
                chr = this.alt.charCodeAt(i);
                hash = (hash << 5) - hash + chr;
                hash |= 0;
            }
            return Math.abs(hash);
        },
        hasPhoto() {
            return this.url && !this.invalidUrl;
        },
        hasIcon() {
            return this.icon;
        },
        iconSize() {
            switch (this.size) {
                case "sm":
                    return "sm";
                case "xl":
                    return "2xl";
                default:
                    return "md";
            }
        },
        hasCount() {
            return this.count !== null;
        },
        countOverflows() {
            return this.count > countLimit;
        },
        boundedCount() {
            return this.countOverflows ? "+" + countLimit : this.count.toString();
        },
        hasStatus() {
            return this.status && typeof this.status === "object" && this.status.color && this.status.label;
        },
        title() {
            if (this.hasStatus) {
                return this.$t("styleguide.avatar.title", { alt: this.alt, status: this.status.label });
            }
            return this.alt;
        },
        url() {
            if (this.urn) {
                const [item, container] = this.urn.split("@");
                return this.location + URL.replace("{container}", container).replace("{item}", item);
            }
            return undefined;
        },
        clip() {
            const shape = (this.hasCount ? "count-" : "") + "hexagon" + (this.size === "sm" ? "-adjusted" : "");
            if (!this.hasStatus) {
                return shape;
            } else {
                return { clip: shape, mask: "status" };
            }
        }
    }
};
</script>

<style lang="scss">
@import "../css/_variables";

@mixin status($radius) {
    width: $radius * 2;
    height: $radius * 2;
    border-radius: $radius;
    right: -$radius;
}

.bm-avatar {
    display: inline-block;
    position: relative;
    flex: none;

    #hexagon,
    #overlay {
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        right: 0;
    }

    #overlay {
        display: flex;
        color: $lowest;
        align-items: center;
        justify-content: center;
        #count {
            margin: 0;
            font-weight: $font-weight-bold;
            letter-spacing: -0.02em;
            color: $neutral-fg;
        }
        #letter {
            margin: 0;
        }
    }

    #status {
        position: absolute;
    }

    &.bm-avatar-sm {
        width: $avatar-width-sm;
        height: $avatar-height-sm;
        font-size: base-px-to-rem(12);

        #count.count-overflows {
            font-size: base-px-to-rem(11);
        }

        #status {
            $radius: base-px-to-rem(3);
            @include status($radius);
            top: base-px-to-rem(1);
        }
    }

    &.bm-avatar-md {
        width: $avatar-width;
        height: $avatar-height;
        font-size: base-px-to-rem(15);

        #count.count-overflows {
            font-size: base-px-to-rem(13);
        }

        #status {
            $radius: base-px-to-rem(3.5);
            @include status($radius);
            top: base-px-to-rem(1.5);
        }
    }

    &.bm-avatar-xl {
        width: $avatar-width-xl;
        height: $avatar-height-xl;
        font-size: base-px-to-rem(42);

        #count.count-overflows {
            font-size: base-px-to-rem(38);
        }

        #status {
            $radius: base-px-to-rem(10);
            @include status($radius);
            top: base-px-to-rem(4);
        }
    }
}
</style>
