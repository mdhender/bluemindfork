<template>
    <svg viewBox="0 0 20 30" class="conversation-avatar" fill="none" xmlns="http://www.w3.org/2000/svg" role="img">
        <title>{{ title }}</title>
        <path class="hexagon" d="M19 20.297V9.44175L10 4.00891L1 9.44175V20.297L10 25.7194L19 20.297Z" />
        <path class="border" d="M19 6.43284L10 1L1 6.43284" />
        <path class="border" d="M19 23.5672L10 29L1 23.5672" />
        <text
            x="50%"
            y="50%"
            text-anchor="middle"
            dominant-baseline="middle"
            class="text font-weight-bold"
            :font-size="fontSize"
        >
            {{ text }}
        </text>
    </svg>
</template>
<script>
export default {
    name: "ConversationAvatar",
    props: {
        text: {
            type: [String, Number],
            required: false,
            default: ""
        },
        fontSize: {
            type: String,
            required: false,
            default: "unset"
        },
        title: {
            type: String,
            required: false,
            default: ""
        }
    }
};
</script>
<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.conversation-avatar {
    width: 2em;
    .hexagon {
        fill: $fill-neutral-bg;
    }
    .border {
        stroke: $neutral-fg;
    }
    .text {
        fill: $fill-neutral-fg;
    }
}
</style>
