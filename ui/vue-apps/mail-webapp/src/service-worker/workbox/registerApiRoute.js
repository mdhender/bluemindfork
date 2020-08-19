import { registerRoute } from "workbox-routing";
import apiHandler from "./apiHandler";

export default function () {
    const httpMethods = ["GET", "POST", "PUT", "DELETE"];
    const API_PREFIX = "/api/";
    httpMethods.forEach(method => registerRoute(new RegExp(API_PREFIX), apiHandler, method));
}
