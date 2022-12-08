import NeedReconnectionAlert from "../Alerts/NeedReconnectionAlert";
import NotValidAlert from "../Alerts/NotValidAlert";
import ReloadAppAlert from "../Alerts/ReloadAppAlert";
import SaveErrorAlert from "../Alerts/SaveErrorAlert";
import { ERROR, REMOVE, WARNING } from "@bluemind/alert.store";
import { BmAlertArea } from "@bluemind/ui-components";
import { mapActions, mapState } from "vuex";

export default {
    components: { BmAlertArea, NeedReconnectionAlert, NotValidAlert, ReloadAppAlert, SaveErrorAlert },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "pref-right-panel") })
    },
    methods: {
        ...mapActions("alert", { ERROR, REMOVE, WARNING })
    }
};
