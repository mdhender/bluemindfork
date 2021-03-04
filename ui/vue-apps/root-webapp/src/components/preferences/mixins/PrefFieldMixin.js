export default {
    props: {
        localUserSettings: {
            type: Object,
            required: false
        },
        setting: {
            type: String,
            required: false
        },
        options: {
            type: Object,
            required: true
        },
        disabled: {
            type: Boolean,
            required: false,
            default: false
        }
    }
};
