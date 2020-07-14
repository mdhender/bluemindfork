/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.proxy.http.impl.vertx;

import java.util.Base64;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;

public final class FaviconHandler implements Handler<HttpServerRequest> {

	private static final String icon = "AAABAAEAICAAAAEAIACoEAAAFgAAACgAAAAgAAAAQAAAAAEAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvzP25r+39ua/tvbmvyz97cQA1celAAYGBQABAQEAzcCfAP7uxQD25r8k9ua/qvbmv8H25r899ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/AP///wD///8A////AP///wD25r8A9ua/APbmvwD25r8A9ua/APbmvxj25r+P9ua/9/bmv//25r//9ua/8/3txIDUxqQMAAAAAAAAAADMv54H/u7FdPbmv+325r//9ua///bmv/z25r+b9ua/IPbmvwD25r8A9ua/APbmvwD25r8A////AP///wD///8A////APbmvwD25r8A9ua/APbmvwX25r9q9ua/5Pbmv//25r//9+fA//npwf/358D//ezE/+jat9M9OTFZMy8oUuLUs8r97cX/9+fA//npwf/358D/9ua///bmv//25r/q9ua/dfbmvwn25r8A9ua/APbmvwD///8A////AP///wD///8A9ua/APbmvwD25r9H9ua/yPbmv//25r//9ua///3txf/+7sXA7+C6v/rqxP/cz7D/vq6H/8+uaP/Rrmb/vqyD/9jLrP/56sT/8OC6yPzsxLj/7sb/9ua///bmv//25r//9ua/0Pbmv0725r8A9ua/AP///wD///8A////AP///wD25r8p9ua/p/bmv//25r//9ua///rqwv//9cvd2cypYV1XSQAZGBcCcGdSo8Wqb//bs13/6Lxg/+i8Yf/etF3/yKts/31yWbMaGhgLT0o/AM/Dolb/9MvV++vD//bmv//25r//9ua///bmv6725r8u////AP///wD///8A////APbmv+T25r//9ua///bmv///88ny7d65hH93ZA4ICAkAAAAAJlNDIZLAm07n6Lxg/+W6Yf/rv2T/7MBl/+W6Yf/pvWH/x6FR615MJp0AAAAvAAIEAHRtWwjn2bR6//PK7ffnwP/25r//9ua///bmv+f///8A////AP///wD///8A9ua/8Pbmv//35r/8++zEp6megyggHxwAAAAADzQqFXKwj0ve78Nm/+m+ZP/ovWP/8cRn/7iWTtCwkEvI78Nm/+m+ZP/ovWP/8MRn/7qYUOZAMxp+AAAAFhcXFQCgln0i+uvDoPfnv/z25r//9ua/8P///wD///8A////AP///wD25r/t9ua///7xyOuvpIkHAAAAABwWCVCPdD3E5rxi/+7CZv/lu2L/8cRn/9KrWudtWS96CAYDCwICAQVhTylty6ZX3/HEZ//mu2L/7MBl/+u/ZP+afUHOJB0NXAAAAAClmoAE/vHI6vbmv//25r/t////AP///wD///8A////APbmv+325r////PK6pKJcgoAAAAXyKJV6/THaP/lumL/7cFl/+i9Y/6Rdj6hGRQLHwAAAAAAAAAAAAAAAAAAAAARDgcWhGw4k+O6Yvnuwmb/5Lpi//PGaP/Qqln3AQAAIod/agn/88rp9ua///bmv+3///8A////AP///wD///8A9ua/7fbmv///88rqlYx1CgEAABzRqVnu6L1j/+e8Y/+zkkzJOzAZRAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMCcVOKiJSL3mvGL/6L1j/9WuW/gDAAAni4JtCf/zyun25r//9ua/7f///wD///8A////AP///wD25r/t9ua////zyuqVjHUKAQAAG86nWOzrv2T/4bdg/yIbDz4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFBAJKtqxXfjrv2T/06xa9gMAACaLgm0J//PK6fbmv//25r/t////AP///wD///8A////APbmv+325r////PK6pWMdQoBAAAbzqdY7Ou/ZP/ftl//JB0QQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAYFAst2LBc+Ou/ZP/TrFr2AwAAJouCbQn/88rp9ua///bmv+3///8A////AP///wD///8A9ua/7fbmv///88rpkolyBwAAABvOp1js679k/9+2X/8lHhBBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABkVCy7YsFz4679k/9OsWvYCAAAmiH9qB//zyuj25r//9ua/7f///wD///8A////AP///wD25r/t9ua////xyO6pn4QQAAAAD82mV+zrv2T/37Zf/yUeEEEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGRULLtiwXPjrv2T/0qtZ9gAAABudk3sM//LJ7Pbmv//25r/t////AP///wD///8A////APbmv/T25r//+OjA//Tlv81rY05nx6JT6Oq/ZP/ftl//JR4QQQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAZFQsu2LBc+Ou/ZP/MpVXxY1pHavDhvMb46MH/9ua///bmv/T///8A////AP///wD///8A9ua/x/bmv//25r//++vF/+bVrv7XsF/057xh/9+2X/8jHQ89AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABgUCirYsFz457xi/9qzYP3i0qr/++zF//bmv//25r//9ua/zP///wD///8A////AP///wD25r8M9ua/efbmv+756cP/4NCq+tixYPbnvGH/4bdg/yUeEFoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFhIKR9qyXfrnvGL/3LVh/97Op//46cP/9ua/8vbmv3/25r8P////AP///wD///8A////APbmvwD25r8A+urCJP/1zKXm1q762bFf+OS6YP/nvGP/u5hQ5kQ3HX8AAAAaAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFDkuGHOxkEze5rxi/+S5YP/dtmH/5tWt///1zav87MQp9uW+APbmvwD///8A////AP///wD///8A9ua/APnpwgDe0K0AcWpYAElDNk/at2v25bhc/+O5YP/swGX/679k/5t+Qs4rIxJgAAAADQAAAAAAAAAAAAAACSMcD1aQdT3F57xj/+7CZv/juWD/5bld/9q0Z/hJQzVZaGJRANDDogD56sMA9ua/AP///wD///8A////AP///wD25r8A/O7FAJyReQBHQjcAZ2FSD+rZs+/kx4f/4rhh/+q+Yv/lu2L/8MNm/+K4YP95YzSzDQoGPQcFAzVsWC+o27Ne/fHEZ//mu2L/679j/+K5YP/lxoL/49Or7E1IPQkyLycAg3tnAP3vxwD25r8A////AP///wD///8A////APbmvwD25b8A++rDAP/wxwD/8McY+OjC8PXnw//izqL1sJFPx+i9Yv/rv2T/5rti//HEZ//BnVLvuphP6/DDZ//nvGP/6b5k/+3BZP+wkEzL28ia8vXnw//46cPt/+/GEv/uxgD66sIA9ua/APbmvwD///8A////AP///wD///8A9ua/APbmvwD25r8A9ua/APbmvxj25r/w+OjA//nsxvBAPTQZQjQXVcSfVNfxxWf/57xj/+m+ZP/qv2T/5rxi//HFZ//Mp1fgUEAfYyUjHRHv4b7m+urC//bmv+325r8S9+e/APbmvwD25r8A9ua/AP///wD///8A////AP///wD25r8A9ua/APbmvwD25r8A9ua/GPbmv/D25r///e7F9MzAoBgaGhgAAAAAD29aLoDasl3w8MRn/+/DZv/gt2D3fGQ0jgcEABcREREAtquPCP7wx+z25r//9ua/7fbmvxL25r8A9ua/APbmvwD25r8A////AP///wD///8A////APbmvwD25r8A9ua/APbmvwD25r8Z9ua/8vbmv//25b7+/u7Gwe3euD95cl8AAwQGABcRBiiafUGrpYZGtiAZCjMAAQMAbWdWAOXXsjX/8Mi19ua//fbmv//25r/u9ua/EvbmvwD25r8A9ua/APbmvwD///8A////AP///wD///8A9ua/APbmvwD25r8A9ua/APbmvxX25r/u9ua///bmv//25r//++rC/f/0y57VyKYiU09EAAAAAAAAAAAARkM5AM7BoRv/9cuU/OvE+fbmv//25r//9ua///bmv/L25r8R9ua/APbmvwD25r8A9ua/AP///wD///8A////AP///wD25r8A9ua/APbmvwD25r8A9ua/Afbmvzb25r+z9ua///bmv//25r//9ua///7uxe3/78d7vLCTCbmtkAX97cRy/+/G6Pbmv//25r//9ua///bmv//25r/G9ua/SfbmvwL25r8A9ua/APbmvwD25r8A////AP///wD///8A////APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvwD25r9P9ua/y/bmv//25r//9ua///fnwP//78fT//DHz/fnwP/25r//9ua///bmv//25r/Y9ua/YPbmvwP25r8A9ua/APbmvwD25r8A9ua/APbmvwD///8A////AP///wD///8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8G9ua/afbmv9/25r//9ua///bmv//25r//9ua///bmv//25r/p9ua/ePbmvw725r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/AP///wD///8A////AP///wD25r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/FPbmv4T25r/x9ua///bmv//25r/29ua/kPbmvxz25r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A////AP///wD///8A////APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvyb25r+l9ua/rPbmvy725r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvwD25r8A9ua/APbmvwD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A////AP///wD///8A///////P8///A+D//gGAf/gAAB/gcA4HwOAHg8PAA8PHA8DjxgfgY8Yf+GPGP/xjxj/8Y8Y//GPGP/xjwj/8Q8A//APwP/wP+B/4H/4H4H/+AYB//gAAf/4wDH/+OBx//h54f/4H4H//A8D//8AD///wD///+B////5///////8=";
	private static final Buffer iconBuffer = icon();

	@Override
	public void handle(HttpServerRequest event) {
		event.response().putHeader(HttpHeaders.CONTENT_TYPE, "image/x-icon");
		event.response().end(iconBuffer);
	}

	private static Buffer icon() {
		return Buffer.buffer(Base64.getDecoder().decode(icon));
	}

}
