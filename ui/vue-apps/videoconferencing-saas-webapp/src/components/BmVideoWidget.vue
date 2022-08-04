<template>
    <bm-button v-if="show" variant="inline-on-fill-primary" :href="url" target="_blank" @mouseup="createRoom">
        <bm-icon icon="video" />
    </bm-button>
</template>
<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import RoomGenerator from "@bluemind/uuid";

const roles = ["hasFullVideoconferencing", "hasSimpleVideoconferencing"];
export default {
    name: "BmVideoWidget",
    components: {
        BmButton,
        BmIcon
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
