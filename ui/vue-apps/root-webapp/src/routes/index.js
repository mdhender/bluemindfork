import OldAppWrapper from "./OldAppWrapper";
import { extensions } from "@bluemind/extensions";
import camelCase from "lodash.camelcase";

const applications = extensions.get("net.bluemind.webapp", "external-application") || [];

export default applications.map(toRoute);

function toRoute({ name, route, href }) {
    const sanitized = camelCase(name || route);
    return {
        name: `external:${sanitized}:root`,
        path: route,
        component: OldAppWrapper(`External${sanitized.charAt(0).toUpperCase()}${sanitized.slice(1)}App`, href)
    };
}
