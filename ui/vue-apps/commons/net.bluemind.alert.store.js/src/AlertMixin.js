import AlertTypes from "./AlertTypes";

export default {
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    data: () => ({ AlertTypes }),
    computed: {
        path() {
            const { name, type } = this.alert;
            return "alert." + name.toLowerCase() + "." + type.toLowerCase();
        },
        payload() {
            return this.alert.payload;
        },
        result() {
            return this.alert.result;
        },
        error() {
            return this.alert.error;
        }
    }
};
