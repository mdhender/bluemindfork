import { VirtualRoutes, MailRoute, PopupRoute } from "./routes";

export default [{ path: "/index.html", redirect: "/mail/" }, PopupRoute, MailRoute, ...VirtualRoutes];
