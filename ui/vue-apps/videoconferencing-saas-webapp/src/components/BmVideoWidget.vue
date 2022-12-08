<template>
    <bm-icon-button
        v-if="show"
        variant="regular-on-fill-primary"
        size="sm"
        :href="url"
        target="_blank"
        icon="video"
        @mouseup="createRoom"
    />
</template>
<script>
import { BmIconButton } from "@bluemind/ui-components";
import RoomGenerator from "@bluemind/uuid";

const roles = ["hasFullVideoconferencing", "hasSimpleVideoconferencing"];
export default {
    name: "BmVideoWidget",
    components: {
        BmIconButton
    },
    data: function () {
        return {
            show: !!window.bmcSessionInfos.roles.split(",").find(role => roles.includes(role)),
            room: RoomGenerator.generate()
        };
    },
    computed: {
        url() {
            return "/visio/" + this.room;
        }
    },
    methods: {
        createRoom() {
            this.room = RoomGenerator.generate();
        }
    }
};
</script>
