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

goog.provide("net.bluemind.timezone");
goog.provide("net.bluemind.timezone.TimeZoneHelper");

goog.require("goog.array");
goog.require("goog.date.DateTime");
goog.require("goog.i18n.TimeZone");
goog.require("goog.structs.Map");
goog.require("net.bluemind.date.ZonedDate");
goog.require('bluemind.timezone.AfricaAbidjan');
goog.require('bluemind.timezone.AfricaAccra');
goog.require('bluemind.timezone.AfricaAddisAbaba');
goog.require('bluemind.timezone.AfricaAlgiers');
goog.require('bluemind.timezone.AfricaAsmara');
goog.require('bluemind.timezone.AfricaBamako');
goog.require('bluemind.timezone.AfricaBangui');
goog.require('bluemind.timezone.AfricaBanjul');
goog.require('bluemind.timezone.AfricaBissau');
goog.require('bluemind.timezone.AfricaBlantyre');
goog.require('bluemind.timezone.AfricaBrazzaville');
goog.require('bluemind.timezone.AfricaBujumbura');
goog.require('bluemind.timezone.AfricaCairo');
goog.require('bluemind.timezone.AfricaCasablanca');
goog.require('bluemind.timezone.AfricaCeuta');
goog.require('bluemind.timezone.AfricaConakry');
goog.require('bluemind.timezone.AfricaDakar');
goog.require('bluemind.timezone.AfricaDaresSalaam');
goog.require('bluemind.timezone.AfricaDjibouti');
goog.require('bluemind.timezone.AfricaDouala');
goog.require('bluemind.timezone.AfricaElAaiun');
goog.require('bluemind.timezone.AfricaFreetown');
goog.require('bluemind.timezone.AfricaGaborone');
goog.require('bluemind.timezone.AfricaHarare');
goog.require('bluemind.timezone.AfricaJohannesburg');
goog.require('bluemind.timezone.AfricaJuba');
goog.require('bluemind.timezone.AfricaKampala');
goog.require('bluemind.timezone.AfricaKhartoum');
goog.require('bluemind.timezone.AfricaKigali');
goog.require('bluemind.timezone.AfricaKinshasa');
goog.require('bluemind.timezone.AfricaLagos');
goog.require('bluemind.timezone.AfricaLibreville');
goog.require('bluemind.timezone.AfricaLome');
goog.require('bluemind.timezone.AfricaLuanda');
goog.require('bluemind.timezone.AfricaLubumbashi');
goog.require('bluemind.timezone.AfricaLusaka');
goog.require('bluemind.timezone.AfricaMalabo');
goog.require('bluemind.timezone.AfricaMaputo');
goog.require('bluemind.timezone.AfricaMaseru');
goog.require('bluemind.timezone.AfricaMbabane');
goog.require('bluemind.timezone.AfricaMogadishu');
goog.require('bluemind.timezone.AfricaMonrovia');
goog.require('bluemind.timezone.AfricaNairobi');
goog.require('bluemind.timezone.AfricaNdjamena');
goog.require('bluemind.timezone.AfricaNiamey');
goog.require('bluemind.timezone.AfricaNouakchott');
goog.require('bluemind.timezone.AfricaOuagadougou');
goog.require('bluemind.timezone.AfricaPortoNovo');
goog.require('bluemind.timezone.AfricaSaoTome');
goog.require('bluemind.timezone.AfricaTripoli');
goog.require('bluemind.timezone.AfricaTunis');
goog.require('bluemind.timezone.AfricaWindhoek');
goog.require('bluemind.timezone.AmericaAdak');
goog.require('bluemind.timezone.AmericaAnchorage');
goog.require('bluemind.timezone.AmericaAnguilla');
goog.require('bluemind.timezone.AmericaAntigua');
goog.require('bluemind.timezone.AmericaAraguaina');
goog.require('bluemind.timezone.AmericaArgentinaBuenosAires');
goog.require('bluemind.timezone.AmericaArgentinaCatamarca');
goog.require('bluemind.timezone.AmericaArgentinaCordoba');
goog.require('bluemind.timezone.AmericaArgentinaJujuy');
goog.require('bluemind.timezone.AmericaArgentinaLaRioja');
goog.require('bluemind.timezone.AmericaArgentinaMendoza');
goog.require('bluemind.timezone.AmericaArgentinaRioGallegos');
goog.require('bluemind.timezone.AmericaArgentinaSalta');
goog.require('bluemind.timezone.AmericaArgentinaSanJuan');
goog.require('bluemind.timezone.AmericaArgentinaSanLuis');
goog.require('bluemind.timezone.AmericaArgentinaTucuman');
goog.require('bluemind.timezone.AmericaArgentinaUshuaia');
goog.require('bluemind.timezone.AmericaAruba');
goog.require('bluemind.timezone.AmericaAsuncion');
goog.require('bluemind.timezone.AmericaAtikokan');
goog.require('bluemind.timezone.AmericaBahia');
goog.require('bluemind.timezone.AmericaBahiaBanderas');
goog.require('bluemind.timezone.AmericaBarbados');
goog.require('bluemind.timezone.AmericaBelem');
goog.require('bluemind.timezone.AmericaBelize');
goog.require('bluemind.timezone.AmericaBlancSablon');
goog.require('bluemind.timezone.AmericaBoaVista');
goog.require('bluemind.timezone.AmericaBogota');
goog.require('bluemind.timezone.AmericaBoise');
goog.require('bluemind.timezone.AmericaCambridgeBay');
goog.require('bluemind.timezone.AmericaCampoGrande');
goog.require('bluemind.timezone.AmericaCancun');
goog.require('bluemind.timezone.AmericaCaracas');
goog.require('bluemind.timezone.AmericaCayenne');
goog.require('bluemind.timezone.AmericaCayman');
goog.require('bluemind.timezone.AmericaChicago');
goog.require('bluemind.timezone.AmericaChihuahua');
goog.require('bluemind.timezone.AmericaCostaRica');
goog.require('bluemind.timezone.AmericaCreston');
goog.require('bluemind.timezone.AmericaCuiaba');
goog.require('bluemind.timezone.AmericaCuracao');
goog.require('bluemind.timezone.AmericaDanmarkshavn');
goog.require('bluemind.timezone.AmericaDawson');
goog.require('bluemind.timezone.AmericaDawsonCreek');
goog.require('bluemind.timezone.AmericaDenver');
goog.require('bluemind.timezone.AmericaDetroit');
goog.require('bluemind.timezone.AmericaDominica');
goog.require('bluemind.timezone.AmericaEdmonton');
goog.require('bluemind.timezone.AmericaEirunepe');
goog.require('bluemind.timezone.AmericaElSalvador');
goog.require('bluemind.timezone.AmericaFortaleza');
goog.require('bluemind.timezone.AmericaGlaceBay');
goog.require('bluemind.timezone.AmericaGodthab');
goog.require('bluemind.timezone.AmericaGooseBay');
goog.require('bluemind.timezone.AmericaGrandTurk');
goog.require('bluemind.timezone.AmericaGrenada');
goog.require('bluemind.timezone.AmericaGuadeloupe');
goog.require('bluemind.timezone.AmericaGuatemala');
goog.require('bluemind.timezone.AmericaGuayaquil');
goog.require('bluemind.timezone.AmericaGuyana');
goog.require('bluemind.timezone.AmericaHalifax');
goog.require('bluemind.timezone.AmericaHavana');
goog.require('bluemind.timezone.AmericaHermosillo');
goog.require('bluemind.timezone.AmericaIndianaIndianapolis');
goog.require('bluemind.timezone.AmericaIndianaKnox');
goog.require('bluemind.timezone.AmericaIndianaMarengo');
goog.require('bluemind.timezone.AmericaIndianaPetersburg');
goog.require('bluemind.timezone.AmericaIndianaTellCity');
goog.require('bluemind.timezone.AmericaIndianaVevay');
goog.require('bluemind.timezone.AmericaIndianaVincennes');
goog.require('bluemind.timezone.AmericaIndianaWinamac');
goog.require('bluemind.timezone.AmericaInuvik');
goog.require('bluemind.timezone.AmericaIqaluit');
goog.require('bluemind.timezone.AmericaJamaica');
goog.require('bluemind.timezone.AmericaJuneau');
goog.require('bluemind.timezone.AmericaKentuckyLouisville');
goog.require('bluemind.timezone.AmericaKentuckyMonticello');
goog.require('bluemind.timezone.AmericaKralendijk');
goog.require('bluemind.timezone.AmericaLaPaz');
goog.require('bluemind.timezone.AmericaLima');
goog.require('bluemind.timezone.AmericaLosAngeles');
goog.require('bluemind.timezone.AmericaLowerPrinces');
goog.require('bluemind.timezone.AmericaMaceio');
goog.require('bluemind.timezone.AmericaManagua');
goog.require('bluemind.timezone.AmericaManaus');
goog.require('bluemind.timezone.AmericaMarigot');
goog.require('bluemind.timezone.AmericaMartinique');
goog.require('bluemind.timezone.AmericaMatamoros');
goog.require('bluemind.timezone.AmericaMazatlan');
goog.require('bluemind.timezone.AmericaMenominee');
goog.require('bluemind.timezone.AmericaMerida');
goog.require('bluemind.timezone.AmericaMetlakatla');
goog.require('bluemind.timezone.AmericaMexicoCity');
goog.require('bluemind.timezone.AmericaMiquelon');
goog.require('bluemind.timezone.AmericaMoncton');
goog.require('bluemind.timezone.AmericaMonterrey');
goog.require('bluemind.timezone.AmericaMontevideo');
goog.require('bluemind.timezone.AmericaMontreal');
goog.require('bluemind.timezone.AmericaMontserrat');
goog.require('bluemind.timezone.AmericaNassau');
goog.require('bluemind.timezone.AmericaNewYork');
goog.require('bluemind.timezone.AmericaNipigon');
goog.require('bluemind.timezone.AmericaNome');
goog.require('bluemind.timezone.AmericaNoronha');
goog.require('bluemind.timezone.AmericaNorthDakotaBeulah');
goog.require('bluemind.timezone.AmericaNorthDakotaCenter');
goog.require('bluemind.timezone.AmericaNorthDakotaNewSalem');
goog.require('bluemind.timezone.AmericaOjinaga');
goog.require('bluemind.timezone.AmericaPanama');
goog.require('bluemind.timezone.AmericaPangnirtung');
goog.require('bluemind.timezone.AmericaParamaribo');
goog.require('bluemind.timezone.AmericaPhoenix');
goog.require('bluemind.timezone.AmericaPortauPrince');
goog.require('bluemind.timezone.AmericaPortofSpain');
goog.require('bluemind.timezone.AmericaPortoVelho');
goog.require('bluemind.timezone.AmericaPuertoRico');
goog.require('bluemind.timezone.AmericaRainyRiver');
goog.require('bluemind.timezone.AmericaRankinInlet');
goog.require('bluemind.timezone.AmericaRecife');
goog.require('bluemind.timezone.AmericaRegina');
goog.require('bluemind.timezone.AmericaResolute');
goog.require('bluemind.timezone.AmericaRioBranco');
goog.require('bluemind.timezone.AmericaSantaIsabel');
goog.require('bluemind.timezone.AmericaSantarem');
goog.require('bluemind.timezone.AmericaSantiago');
goog.require('bluemind.timezone.AmericaSantoDomingo');
goog.require('bluemind.timezone.AmericaSaoPaulo');
goog.require('bluemind.timezone.AmericaScoresbysund');
goog.require('bluemind.timezone.AmericaShiprock');
goog.require('bluemind.timezone.AmericaSitka');
goog.require('bluemind.timezone.AmericaStJohns');
goog.require('bluemind.timezone.AmericaStKitts');
goog.require('bluemind.timezone.AmericaStLucia');
goog.require('bluemind.timezone.AmericaStThomas');
goog.require('bluemind.timezone.AmericaStVincent');
goog.require('bluemind.timezone.AmericaSwiftCurrent');
goog.require('bluemind.timezone.AmericaTegucigalpa');
goog.require('bluemind.timezone.AmericaThule');
goog.require('bluemind.timezone.AmericaThunderBay');
goog.require('bluemind.timezone.AmericaTijuana');
goog.require('bluemind.timezone.AmericaToronto');
goog.require('bluemind.timezone.AmericaTortola');
goog.require('bluemind.timezone.AmericaVancouver');
goog.require('bluemind.timezone.AmericaWhitehorse');
goog.require('bluemind.timezone.AmericaWinnipeg');
goog.require('bluemind.timezone.AmericaYakutat');
goog.require('bluemind.timezone.AmericaYellowknife');
goog.require('bluemind.timezone.AntarcticaCasey');
goog.require('bluemind.timezone.AntarcticaDavis');
goog.require('bluemind.timezone.AntarcticaDumontDUrville');
goog.require('bluemind.timezone.AntarcticaMacquarie');
goog.require('bluemind.timezone.AntarcticaMawson');
goog.require('bluemind.timezone.AntarcticaMcMurdo');
goog.require('bluemind.timezone.AntarcticaPalmer');
goog.require('bluemind.timezone.AntarcticaRothera');
goog.require('bluemind.timezone.AntarcticaSouthPole');
goog.require('bluemind.timezone.AntarcticaSyowa');
goog.require('bluemind.timezone.AntarcticaVostok');
goog.require('bluemind.timezone.ArcticLongyearbyen');
goog.require('bluemind.timezone.AsiaAden');
goog.require('bluemind.timezone.AsiaAlmaty');
goog.require('bluemind.timezone.AsiaAmman');
goog.require('bluemind.timezone.AsiaAnadyr');
goog.require('bluemind.timezone.AsiaAqtau');
goog.require('bluemind.timezone.AsiaAqtobe');
goog.require('bluemind.timezone.AsiaAshgabat');
goog.require('bluemind.timezone.AsiaBaghdad');
goog.require('bluemind.timezone.AsiaBahrain');
goog.require('bluemind.timezone.AsiaBaku');
goog.require('bluemind.timezone.AsiaBangkok');
goog.require('bluemind.timezone.AsiaBeirut');
goog.require('bluemind.timezone.AsiaBishkek');
goog.require('bluemind.timezone.AsiaBrunei');
goog.require('bluemind.timezone.AsiaChoibalsan');
goog.require('bluemind.timezone.AsiaChongqing');
goog.require('bluemind.timezone.AsiaColombo');
goog.require('bluemind.timezone.AsiaDamascus');
goog.require('bluemind.timezone.AsiaDhaka');
goog.require('bluemind.timezone.AsiaDili');
goog.require('bluemind.timezone.AsiaDubai');
goog.require('bluemind.timezone.AsiaDushanbe');
goog.require('bluemind.timezone.AsiaGaza');
goog.require('bluemind.timezone.AsiaHarbin');
goog.require('bluemind.timezone.AsiaHebron');
goog.require('bluemind.timezone.AsiaHoChiMinh');
goog.require('bluemind.timezone.AsiaHongKong');
goog.require('bluemind.timezone.AsiaHovd');
goog.require('bluemind.timezone.AsiaIrkutsk');
goog.require('bluemind.timezone.AsiaJakarta');
goog.require('bluemind.timezone.AsiaJayapura');
goog.require('bluemind.timezone.AsiaJerusalem');
goog.require('bluemind.timezone.AsiaKabul');
goog.require('bluemind.timezone.AsiaKamchatka');
goog.require('bluemind.timezone.AsiaKarachi');
goog.require('bluemind.timezone.AsiaKashgar');
goog.require('bluemind.timezone.AsiaKathmandu');
goog.require('bluemind.timezone.AsiaKolkata');
goog.require('bluemind.timezone.AsiaKrasnoyarsk');
goog.require('bluemind.timezone.AsiaKualaLumpur');
goog.require('bluemind.timezone.AsiaKuching');
goog.require('bluemind.timezone.AsiaKuwait');
goog.require('bluemind.timezone.AsiaMacau');
goog.require('bluemind.timezone.AsiaMagadan');
goog.require('bluemind.timezone.AsiaMakassar');
goog.require('bluemind.timezone.AsiaManila');
goog.require('bluemind.timezone.AsiaMuscat');
goog.require('bluemind.timezone.AsiaNicosia');
goog.require('bluemind.timezone.AsiaNovokuznetsk');
goog.require('bluemind.timezone.AsiaNovosibirsk');
goog.require('bluemind.timezone.AsiaOmsk');
goog.require('bluemind.timezone.AsiaOral');
goog.require('bluemind.timezone.AsiaPhnomPenh');
goog.require('bluemind.timezone.AsiaPontianak');
goog.require('bluemind.timezone.AsiaPyongyang');
goog.require('bluemind.timezone.AsiaQatar');
goog.require('bluemind.timezone.AsiaQyzylorda');
goog.require('bluemind.timezone.AsiaRangoon');
goog.require('bluemind.timezone.AsiaRiyadh');
goog.require('bluemind.timezone.AsiaSakhalin');
goog.require('bluemind.timezone.AsiaSamarkand');
goog.require('bluemind.timezone.AsiaSeoul');
goog.require('bluemind.timezone.AsiaShanghai');
goog.require('bluemind.timezone.AsiaSingapore');
goog.require('bluemind.timezone.AsiaTaipei');
goog.require('bluemind.timezone.AsiaTashkent');
goog.require('bluemind.timezone.AsiaTbilisi');
goog.require('bluemind.timezone.AsiaTehran');
goog.require('bluemind.timezone.AsiaThimphu');
goog.require('bluemind.timezone.AsiaTokyo');
goog.require('bluemind.timezone.AsiaUlaanbaatar');
goog.require('bluemind.timezone.AsiaUrumqi');
goog.require('bluemind.timezone.AsiaVientiane');
goog.require('bluemind.timezone.AsiaVladivostok');
goog.require('bluemind.timezone.AsiaYakutsk');
goog.require('bluemind.timezone.AsiaYekaterinburg');
goog.require('bluemind.timezone.AsiaYerevan');
goog.require('bluemind.timezone.AtlanticAzores');
goog.require('bluemind.timezone.AtlanticBermuda');
goog.require('bluemind.timezone.AtlanticCanary');
goog.require('bluemind.timezone.AtlanticCapeVerde');
goog.require('bluemind.timezone.AtlanticFaroe');
goog.require('bluemind.timezone.AtlanticMadeira');
goog.require('bluemind.timezone.AtlanticReykjavik');
goog.require('bluemind.timezone.AtlanticSouthGeorgia');
goog.require('bluemind.timezone.AtlanticStHelena');
goog.require('bluemind.timezone.AtlanticStanley');
goog.require('bluemind.timezone.AustraliaAdelaide');
goog.require('bluemind.timezone.AustraliaBrisbane');
goog.require('bluemind.timezone.AustraliaBrokenHill');
goog.require('bluemind.timezone.AustraliaCurrie');
goog.require('bluemind.timezone.AustraliaDarwin');
goog.require('bluemind.timezone.AustraliaEucla');
goog.require('bluemind.timezone.AustraliaHobart');
goog.require('bluemind.timezone.AustraliaLindeman');
goog.require('bluemind.timezone.AustraliaLordHowe');
goog.require('bluemind.timezone.AustraliaMelbourne');
goog.require('bluemind.timezone.AustraliaPerth');
goog.require('bluemind.timezone.AustraliaSydney');
goog.require('bluemind.timezone.EuropeAmsterdam');
goog.require('bluemind.timezone.EuropeAndorra');
goog.require('bluemind.timezone.EuropeAthens');
goog.require('bluemind.timezone.EuropeBelgrade');
goog.require('bluemind.timezone.EuropeBerlin');
goog.require('bluemind.timezone.EuropeBratislava');
goog.require('bluemind.timezone.EuropeBrussels');
goog.require('bluemind.timezone.EuropeBucharest');
goog.require('bluemind.timezone.EuropeBudapest');
goog.require('bluemind.timezone.EuropeChisinau');
goog.require('bluemind.timezone.EuropeCopenhagen');
goog.require('bluemind.timezone.EuropeDublin');
goog.require('bluemind.timezone.EuropeGibraltar');
goog.require('bluemind.timezone.EuropeGuernsey');
goog.require('bluemind.timezone.EuropeHelsinki');
goog.require('bluemind.timezone.EuropeIsleofMan');
goog.require('bluemind.timezone.EuropeIstanbul');
goog.require('bluemind.timezone.EuropeJersey');
goog.require('bluemind.timezone.EuropeKaliningrad');
goog.require('bluemind.timezone.EuropeKiev');
goog.require('bluemind.timezone.EuropeLisbon');
goog.require('bluemind.timezone.EuropeLjubljana');
goog.require('bluemind.timezone.EuropeLondon');
goog.require('bluemind.timezone.EuropeLuxembourg');
goog.require('bluemind.timezone.EuropeMadrid');
goog.require('bluemind.timezone.EuropeMalta');
goog.require('bluemind.timezone.EuropeMariehamn');
goog.require('bluemind.timezone.EuropeMinsk');
goog.require('bluemind.timezone.EuropeMonaco');
goog.require('bluemind.timezone.EuropeMoscow');
goog.require('bluemind.timezone.EuropeOslo');
goog.require('bluemind.timezone.EuropeParis');
goog.require('bluemind.timezone.EuropePodgorica');
goog.require('bluemind.timezone.EuropePrague');
goog.require('bluemind.timezone.EuropeRiga');
goog.require('bluemind.timezone.EuropeRome');
goog.require('bluemind.timezone.EuropeSamara');
goog.require('bluemind.timezone.EuropeSanMarino');
goog.require('bluemind.timezone.EuropeSarajevo');
goog.require('bluemind.timezone.EuropeSimferopol');
goog.require('bluemind.timezone.EuropeSkopje');
goog.require('bluemind.timezone.EuropeSofia');
goog.require('bluemind.timezone.EuropeStockholm');
goog.require('bluemind.timezone.EuropeTallinn');
goog.require('bluemind.timezone.EuropeTirane');
goog.require('bluemind.timezone.EuropeUzhgorod');
goog.require('bluemind.timezone.EuropeVaduz');
goog.require('bluemind.timezone.EuropeVatican');
goog.require('bluemind.timezone.EuropeVienna');
goog.require('bluemind.timezone.EuropeVilnius');
goog.require('bluemind.timezone.EuropeVolgograd');
goog.require('bluemind.timezone.EuropeWarsaw');
goog.require('bluemind.timezone.EuropeZagreb');
goog.require('bluemind.timezone.EuropeZaporozhye');
goog.require('bluemind.timezone.EuropeZurich');
goog.require('bluemind.timezone.IndianAntananarivo');
goog.require('bluemind.timezone.IndianChagos');
goog.require('bluemind.timezone.IndianChristmas');
goog.require('bluemind.timezone.IndianCocos');
goog.require('bluemind.timezone.IndianComoro');
goog.require('bluemind.timezone.IndianKerguelen');
goog.require('bluemind.timezone.IndianMahe');
goog.require('bluemind.timezone.IndianMaldives');
goog.require('bluemind.timezone.IndianMauritius');
goog.require('bluemind.timezone.IndianMayotte');
goog.require('bluemind.timezone.IndianReunion');
goog.require('bluemind.timezone.PacificApia');
goog.require('bluemind.timezone.PacificAuckland');
goog.require('bluemind.timezone.PacificChatham');
goog.require('bluemind.timezone.PacificChuuk');
goog.require('bluemind.timezone.PacificEaster');
goog.require('bluemind.timezone.PacificEfate');
goog.require('bluemind.timezone.PacificEnderbury');
goog.require('bluemind.timezone.PacificFakaofo');
goog.require('bluemind.timezone.PacificFiji');
goog.require('bluemind.timezone.PacificFunafuti');
goog.require('bluemind.timezone.PacificGalapagos');
goog.require('bluemind.timezone.PacificGambier');
goog.require('bluemind.timezone.PacificGuadalcanal');
goog.require('bluemind.timezone.PacificGuam');
goog.require('bluemind.timezone.PacificHonolulu');
goog.require('bluemind.timezone.PacificJohnston');
goog.require('bluemind.timezone.PacificKiritimati');
goog.require('bluemind.timezone.PacificKosrae');
goog.require('bluemind.timezone.PacificKwajalein');
goog.require('bluemind.timezone.PacificMajuro');
goog.require('bluemind.timezone.PacificMarquesas');
goog.require('bluemind.timezone.PacificMidway');
goog.require('bluemind.timezone.PacificNauru');
goog.require('bluemind.timezone.PacificNiue');
goog.require('bluemind.timezone.PacificNorfolk');
goog.require('bluemind.timezone.PacificNoumea');
goog.require('bluemind.timezone.PacificPagoPago');
goog.require('bluemind.timezone.PacificPalau');
goog.require('bluemind.timezone.PacificPitcairn');
goog.require('bluemind.timezone.PacificPohnpei');
goog.require('bluemind.timezone.PacificPortMoresby');
goog.require('bluemind.timezone.PacificRarotonga');
goog.require('bluemind.timezone.PacificSaipan');
goog.require('bluemind.timezone.PacificTahiti');
goog.require('bluemind.timezone.PacificTarawa');
goog.require('bluemind.timezone.PacificTongatapu');
goog.require('bluemind.timezone.PacificWake');
goog.require('bluemind.timezone.PacificWallis');
goog.require('bluemind.timezone.UTC');

/**
 * Set system time zone
 * 
 * @static
 * @param {string} timezone
 */
net.bluemind.timezone.setSystemTimeZone = function(timezone) {
  if (goog.array.contains(net.bluemind.timezone.ALL, timezone)) {
    var data = net.bluemind.timezone.TimeZoneHelper.getRawData(timezone);
    net.bluemind.timezone.DEFAULT = goog.i18n.TimeZone.createTimeZone(data)
  }
};

/**
 * Time zone list.
 * 
 * @type {Array}
 * @private
 */
net.bluemind.timezone.ALL = [ 'Africa/Abidjan', 'Africa/Accra', 'Africa/Addis_Ababa', 'Africa/Algiers',
    'Africa/Asmara', 'Africa/Bamako', 'Africa/Bangui', 'Africa/Banjul', 'Africa/Bissau', 'Africa/Blantyre',
    'Africa/Brazzaville', 'Africa/Bujumbura', 'Africa/Cairo', 'Africa/Casablanca', 'Africa/Ceuta', 'Africa/Conakry',
    'Africa/Dakar', 'Africa/Dar_es_Salaam', 'Africa/Djibouti', 'Africa/Douala', 'Africa/El_Aaiun', 'Africa/Freetown',
    'Africa/Gaborone', 'Africa/Harare', 'Africa/Johannesburg', 'Africa/Juba', 'Africa/Kampala', 'Africa/Khartoum',
    'Africa/Kigali', 'Africa/Kinshasa', 'Africa/Lagos', 'Africa/Libreville', 'Africa/Lome', 'Africa/Luanda',
    'Africa/Lubumbashi', 'Africa/Lusaka', 'Africa/Malabo', 'Africa/Maputo', 'Africa/Maseru', 'Africa/Mbabane',
    'Africa/Mogadishu', 'Africa/Monrovia', 'Africa/Nairobi', 'Africa/Ndjamena', 'Africa/Niamey', 'Africa/Nouakchott',
    'Africa/Ouagadougou', 'Africa/Porto-Novo', 'Africa/Sao_Tome', 'Africa/Tripoli', 'Africa/Tunis', 'Africa/Windhoek',
    'America/Adak', 'America/Anchorage', 'America/Anguilla', 'America/Antigua', 'America/Araguaina',
    'America/Argentina/Buenos_Aires', 'America/Argentina/Catamarca', 'America/Argentina/Cordoba',
    'America/Argentina/Jujuy', 'America/Argentina/La_Rioja', 'America/Argentina/Mendoza',
    'America/Argentina/Rio_Gallegos', 'America/Argentina/Salta', 'America/Argentina/San_Juan',
    'America/Argentina/San_Luis', 'America/Argentina/Tucuman', 'America/Argentina/Ushuaia', 'America/Aruba',
    'America/Asuncion', 'America/Atikokan', 'America/Bahia', 'America/Bahia_Banderas', 'America/Barbados',
    'America/Belem', 'America/Belize', 'America/Blanc-Sablon', 'America/Boa_Vista', 'America/Bogota', 'America/Boise',
    'America/Cambridge_Bay', 'America/Campo_Grande', 'America/Cancun', 'America/Caracas', 'America/Cayenne',
    'America/Cayman', 'America/Chicago', 'America/Chihuahua', 'America/Costa_Rica', 'America/Cuiaba',
    'America/Curacao', 'America/Danmarkshavn', 'America/Dawson', 'America/Dawson_Creek', 'America/Denver',
    'America/Detroit', 'America/Dominica', 'America/Edmonton', 'America/Eirunepe', 'America/El_Salvador',
    'America/Fortaleza', 'America/Glace_Bay', 'America/Godthab', 'America/Goose_Bay', 'America/Grand_Turk',
    'America/Grenada', 'America/Guadeloupe', 'America/Guatemala', 'America/Guayaquil', 'America/Guyana',
    'America/Halifax', 'America/Havana', 'America/Hermosillo', 'America/Indiana/Indianapolis', 'America/Indiana/Knox',
    'America/Indiana/Marengo', 'America/Indiana/Petersburg', 'America/Indiana/Tell_City', 'America/Indiana/Vevay',
    'America/Indiana/Vincennes', 'America/Indiana/Winamac', 'America/Inuvik', 'America/Iqaluit', 'America/Jamaica',
    'America/Juneau', 'America/Kentucky/Louisville', 'America/Kentucky/Monticello', 'America/Kralendijk',
    'America/La_Paz', 'America/Lima', 'America/Los_Angeles', 'America/Lower_Princes', 'America/Maceio',
    'America/Managua', 'America/Manaus', 'America/Marigot', 'America/Martinique', 'America/Matamoros',
    'America/Mazatlan', 'America/Menominee', 'America/Merida', 'America/Metlakatla', 'America/Mexico_City',
    'America/Miquelon', 'America/Moncton', 'America/Monterrey', 'America/Montevideo', 'America/Montreal',
    'America/Montserrat', 'America/Nassau', 'America/New_York', 'America/Nipigon', 'America/Nome', 'America/Noronha',
    'America/North_Dakota/Beulah', 'America/North_Dakota/Center', 'America/North_Dakota/New_Salem', 'America/Ojinaga',
    'America/Panama', 'America/Pangnirtung', 'America/Paramaribo', 'America/Phoenix', 'America/Port-au-Prince',
    'America/Port_of_Spain', 'America/Porto_Velho', 'America/Puerto_Rico', 'America/Rainy_River',
    'America/Rankin_Inlet', 'America/Recife', 'America/Regina', 'America/Resolute', 'America/Rio_Branco',
    'America/Santa_Isabel', 'America/Santarem', 'America/Santiago', 'America/Santo_Domingo', 'America/Sao_Paulo',
    'America/Scoresbysund', 'America/Shiprock', 'America/Sitka', 'America/St_Barthelemy', 'America/St_Johns',
    'America/St_Kitts', 'America/St_Lucia', 'America/St_Thomas', 'America/St_Vincent', 'America/Swift_Current',
    'America/Tegucigalpa', 'America/Thule', 'America/Thunder_Bay', 'America/Tijuana', 'America/Toronto',
    'America/Tortola', 'America/Vancouver', 'America/Whitehorse', 'America/Winnipeg', 'America/Yakutat',
    'America/Yellowknife', 'Antarctica/Casey', 'Antarctica/Davis', 'Antarctica/DumontDUrville', 'Antarctica/Macquarie',
    'Antarctica/Mawson', 'Antarctica/McMurdo', 'Antarctica/Palmer', 'Antarctica/Rothera', 'Antarctica/South_Pole',
    'Antarctica/Syowa', 'Antarctica/Vostok', 'Arctic/Longyearbyen', 'Asia/Aden', 'Asia/Almaty', 'Asia/Amman',
    'Asia/Anadyr', 'Asia/Aqtau', 'Asia/Aqtobe', 'Asia/Ashgabat', 'Asia/Baghdad', 'Asia/Bahrain', 'Asia/Baku',
    'Asia/Bangkok', 'Asia/Beirut', 'Asia/Bishkek', 'Asia/Brunei', 'Asia/Choibalsan', 'Asia/Chongqing', 'Asia/Colombo',
    'Asia/Damascus', 'Asia/Dhaka', 'Asia/Dili', 'Asia/Dubai', 'Asia/Dushanbe', 'Asia/Gaza', 'Asia/Harbin',
    'Asia/Hebron', 'Asia/Ho_Chi_Minh', 'Asia/Hong_Kong', 'Asia/Hovd', 'Asia/Irkutsk', 'Asia/Jakarta', 'Asia/Jayapura',
    'Asia/Jerusalem', 'Asia/Kabul', 'Asia/Kamchatka', 'Asia/Karachi', 'Asia/Kashgar', 'Asia/Kathmandu', 'Asia/Kolkata',
    'Asia/Krasnoyarsk', 'Asia/Kuala_Lumpur', 'Asia/Kuching', 'Asia/Kuwait', 'Asia/Macau', 'Asia/Magadan',
    'Asia/Makassar', 'Asia/Manila', 'Asia/Muscat', 'Asia/Nicosia', 'Asia/Novokuznetsk', 'Asia/Novosibirsk',
    'Asia/Omsk', 'Asia/Oral', 'Asia/Phnom_Penh', 'Asia/Pontianak', 'Asia/Pyongyang', 'Asia/Qatar', 'Asia/Qyzylorda',
    'Asia/Rangoon', 'Asia/Riyadh', 'Asia/Sakhalin', 'Asia/Samarkand', 'Asia/Seoul', 'Asia/Shanghai', 'Asia/Singapore',
    'Asia/Taipei', 'Asia/Tashkent', 'Asia/Tbilisi', 'Asia/Tehran', 'Asia/Thimphu', 'Asia/Tokyo', 'Asia/Ulaanbaatar',
    'Asia/Urumqi', 'Asia/Vientiane', 'Asia/Vladivostok', 'Asia/Yakutsk', 'Asia/Yekaterinburg', 'Asia/Yerevan',
    'Atlantic/Azores', 'Atlantic/Bermuda', 'Atlantic/Canary', 'Atlantic/Cape_Verde', 'Atlantic/Faroe',
    'Atlantic/Madeira', 'Atlantic/Reykjavik', 'Atlantic/South_Georgia', 'Atlantic/St_Helena', 'Atlantic/Stanley',
    'Australia/Adelaide', 'Australia/Brisbane', 'Australia/Broken_Hill', 'Australia/Currie', 'Australia/Darwin',
    'Australia/Eucla', 'Australia/Hobart', 'Australia/Lindeman', 'Australia/Lord_Howe', 'Australia/Melbourne',
    'Australia/Perth', 'Australia/Sydney', 'Europe/Amsterdam', 'Europe/Andorra', 'Europe/Athens', 'Europe/Belgrade',
    'Europe/Berlin', 'Europe/Bratislava', 'Europe/Brussels', 'Europe/Bucharest', 'Europe/Budapest', 'Europe/Chisinau',
    'Europe/Copenhagen', 'Europe/Dublin', 'Europe/Gibraltar', 'Europe/Guernsey', 'Europe/Helsinki',
    'Europe/Isle_of_Man', 'Europe/Istanbul', 'Europe/Jersey', 'Europe/Kaliningrad', 'Europe/Kiev', 'Europe/Lisbon',
    'Europe/Ljubljana', 'Europe/London', 'Europe/Luxembourg', 'Europe/Madrid', 'Europe/Malta', 'Europe/Mariehamn',
    'Europe/Minsk', 'Europe/Monaco', 'Europe/Moscow', 'Europe/Oslo', 'Europe/Paris', 'Europe/Podgorica',
    'Europe/Prague', 'Europe/Riga', 'Europe/Rome', 'Europe/Samara', 'Europe/San_Marino', 'Europe/Sarajevo',
    'Europe/Simferopol', 'Europe/Skopje', 'Europe/Sofia', 'Europe/Stockholm', 'Europe/Tallinn', 'Europe/Tirane',
    'Europe/Uzhgorod', 'Europe/Vaduz', 'Europe/Vatican', 'Europe/Vienna', 'Europe/Vilnius', 'Europe/Volgograd',
    'Europe/Warsaw', 'Europe/Zagreb', 'Europe/Zaporozhye', 'Europe/Zurich', 'Indian/Antananarivo', 'Indian/Chagos',
    'Indian/Christmas', 'Indian/Cocos', 'Indian/Comoro', 'Indian/Kerguelen', 'Indian/Mahe', 'Indian/Maldives',
    'Indian/Mauritius', 'Indian/Mayotte', 'Indian/Reunion', 'Pacific/Apia', 'Pacific/Auckland', 'Pacific/Chatham',
    'Pacific/Chuuk', 'Pacific/Easter', 'Pacific/Efate', 'Pacific/Enderbury', 'Pacific/Fakaofo', 'Pacific/Fiji',
    'Pacific/Funafuti', 'Pacific/Galapagos', 'Pacific/Gambier', 'Pacific/Guadalcanal', 'Pacific/Guam',
    'Pacific/Honolulu', 'Pacific/Johnston', 'Pacific/Kiritimati', 'Pacific/Kosrae', 'Pacific/Kwajalein',
    'Pacific/Majuro', 'Pacific/Marquesas', 'Pacific/Midway', 'Pacific/Nauru', 'Pacific/Niue', 'Pacific/Norfolk',
    'Pacific/Noumea', 'Pacific/Pago_Pago', 'Pacific/Palau', 'Pacific/Pitcairn', 'Pacific/Pohnpei',
    'Pacific/Port_Moresby', 'Pacific/Rarotonga', 'Pacific/Saipan', 'Pacific/Tahiti', 'Pacific/Tarawa',
    'Pacific/Tongatapu', 'Pacific/Wake', 'Pacific/Wallis', 'UTC' ];

/**
 * @param {string=} opt_timezone Context timezone.
 * @constructor
 */
net.bluemind.timezone.TimeZoneHelper = function(opt_timezone) {
  if (opt_timezone) {
    net.bluemind.timezone.setSystemTimeZone(opt_timezone)
  }
  this.timeZones_ = new goog.structs.Map();
};

/**
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.timezone.TimeZoneHelper.prototype.timeZones_;

/**
 * @return {goog.i18n.TimeZone}
 */
net.bluemind.timezone.TimeZoneHelper.prototype.getUTC = function() {
  return net.bluemind.timezone.UTC;
};

/**
 * @param {string} tz
 * @return {goog.i18n.TimeZone}
 */
net.bluemind.timezone.TimeZoneHelper.prototype.getTimeZone = function(tz) {
  if (!this.timeZones_.containsKey(tz)) {
    var data = net.bluemind.timezone.TimeZoneHelper.getRawData(tz);
    if (data) {
      var timeZone = goog.i18n.TimeZone.createTimeZone(data);
      this.timeZones_.set(tz, timeZone);
      return timeZone
    }
    return this.getDefaultTimeZone();
  }
  return this.timeZones_.get(tz);
};

/**
 * @return {goog.i18n.TimeZone}
 */
net.bluemind.timezone.TimeZoneHelper.prototype.getDefaultTimeZone = function() {
  return net.bluemind.timezone.DEFAULT;
};

/**
 * @return {string}
 */
net.bluemind.timezone.TimeZoneHelper.prototype.getDefaultId = function() {
  return net.bluemind.timezone.DEFAULT.getTimeZoneId();
};

/**
 * Get all timezone names
 * 
 * @return {Array.<string>}
 */
net.bluemind.timezone.TimeZoneHelper.prototype.all = function() {
  return net.bluemind.timezone.ALL;
};

/**
 * Convert a date to another timezone
 * 
 * @param {net.bluemind.date.ZonedDate} date Date to convert
 * @param {string | goog.i18n.TimeZone=} opt_timezone Timezone to convert to
 * @return {net.bluemind.date.ZonedDate}
 */
net.bluemind.timezone.TimeZoneHelper.prototype.convert = function(date, opt_timezone) {
  var timezone = opt_timezone || this.getDefaultTimeZone();
  if (goog.isString(timezone)) {
    timezone = this.getTimeZone(timezone);
  }

  if (!(date instanceof goog.date.DateTime) || this.equals(timezone, date.getTimeZone())) {
    return date;
  }
  return new net.bluemind.date.DateTime(date, timezone);
};

/**
 * @param {goog.i18n.TimeZone} timeZone1
 * @param {goog.i18n.TimeZone} timeZone2
 */
net.bluemind.timezone.TimeZoneHelper.prototype.equals = function(timeZone1, timeZone2) {
  return timeZone1.getTimeZoneId() == timeZone2.getTimeZoneId();
};

/**
 * Return timezone raw data FIXME: Tz datas should be get from network, not
 * embended into the script. Or modules ?
 * 
 * @param {string} timeZone timezone identifier.
 * @return {Object} timezone data.
 */
net.bluemind.timezone.TimeZoneHelper.getRawData = function(timeZone) {
  var ret;
  // handle Etc/GMT*
  if (timeZone == 'Etc/GMT' || timeZone == 'Etc/UTC') {
    timeZone = 'UTC';
  }
  if (goog.string.startsWith(timeZone, 'Etc/GMT')) {
    var t = goog.string.removeAt(timeZone, 0, 'Etc/GMT'.length);

    var offset = goog.string.parseInt(t);
    return {
      'transitions' : [ -17259887, -offset * 60 ],
      'names' : new Array(),
      'id' : timeZone,
      'std_offset' : 0
    };
  }

  // not Etc/GMT
  switch (timeZone) {
  case 'Africa/Abidjan':
    ret = bluemind.timezone.AfricaAbidjan;
    break;
  case 'Africa/Accra':
    ret = bluemind.timezone.AfricaAccra;
    break;
  case 'Africa/Addis_Ababa':
    ret = bluemind.timezone.AfricaAddisAbaba;
    break;
  case 'Africa/Algiers':
    ret = bluemind.timezone.AfricaAlgiers;
    break;
  case 'Africa/Asmara':
    ret = bluemind.timezone.AfricaAsmara;
    break;
  case 'Africa/Bamako':
    ret = bluemind.timezone.AfricaBamako;
    break;
  case 'Africa/Bangui':
    ret = bluemind.timezone.AfricaBangui;
    break;
  case 'Africa/Banjul':
    ret = bluemind.timezone.AfricaBanjul;
    break;
  case 'Africa/Bissau':
    ret = bluemind.timezone.AfricaBissau;
    break;
  case 'Africa/Blantyre':
    ret = bluemind.timezone.AfricaBlantyre;
    break;
  case 'Africa/Brazzaville':
    ret = bluemind.timezone.AfricaBrazzaville;
    break;
  case 'Africa/Bujumbura':
    ret = bluemind.timezone.AfricaBujumbura;
    break;
  case 'Africa/Cairo':
    ret = bluemind.timezone.AfricaCairo;
    break;
  case 'Africa/Casablanca':
    ret = bluemind.timezone.AfricaCasablanca;
    break;
  case 'Africa/Ceuta':
    ret = bluemind.timezone.AfricaCeuta;
    break;
  case 'Africa/Conakry':
    ret = bluemind.timezone.AfricaConakry;
    break;
  case 'Africa/Dakar':
    ret = bluemind.timezone.AfricaDakar;
    break;
  case 'Africa/Dar_es_Salaam':
    ret = bluemind.timezone.AfricaDaresSalaam;
    break;
  case 'Africa/Djibouti':
    ret = bluemind.timezone.AfricaDjibouti;
    break;
  case 'Africa/Douala':
    ret = bluemind.timezone.AfricaDouala;
    break;
  case 'Africa/El_Aaiun':
    ret = bluemind.timezone.AfricaElAaiun;
    break;
  case 'Africa/Freetown':
    ret = bluemind.timezone.AfricaFreetown;
    break;
  case 'Africa/Gaborone':
    ret = bluemind.timezone.AfricaGaborone;
    break;
  case 'Africa/Harare':
    ret = bluemind.timezone.AfricaHarare;
    break;
  case 'Africa/Johannesburg':
    ret = bluemind.timezone.AfricaJohannesburg;
    break;
  case 'Africa/Juba':
    ret = bluemind.timezone.AfricaJuba;
    break;
  case 'Africa/Kampala':
    ret = bluemind.timezone.AfricaKampala;
    break;
  case 'Africa/Khartoum':
    ret = bluemind.timezone.AfricaKhartoum;
    break;
  case 'Africa/Kigali':
    ret = bluemind.timezone.AfricaKigali;
    break;
  case 'Africa/Kinshasa':
    ret = bluemind.timezone.AfricaKinshasa;
    break;
  case 'Africa/Lagos':
    ret = bluemind.timezone.AfricaLagos;
    break;
  case 'Africa/Libreville':
    ret = bluemind.timezone.AfricaLibreville;
    break;
  case 'Africa/Lome':
    ret = bluemind.timezone.AfricaLome;
    break;
  case 'Africa/Luanda':
    ret = bluemind.timezone.AfricaLuanda;
    break;
  case 'Africa/Lubumbashi':
    ret = bluemind.timezone.AfricaLubumbashi;
    break;
  case 'Africa/Lusaka':
    ret = bluemind.timezone.AfricaLusaka;
    break;
  case 'Africa/Malabo':
    ret = bluemind.timezone.AfricaMalabo;
    break;
  case 'Africa/Maputo':
    ret = bluemind.timezone.AfricaMaputo;
    break;
  case 'Africa/Maseru':
    ret = bluemind.timezone.AfricaMaseru;
    break;
  case 'Africa/Mbabane':
    ret = bluemind.timezone.AfricaMbabane;
    break;
  case 'Africa/Mogadishu':
    ret = bluemind.timezone.AfricaMogadishu;
    break;
  case 'Africa/Monrovia':
    ret = bluemind.timezone.AfricaMonrovia;
    break;
  case 'Africa/Nairobi':
    ret = bluemind.timezone.AfricaNairobi;
    break;
  case 'Africa/Ndjamena':
    ret = bluemind.timezone.AfricaNdjamena;
    break;
  case 'Africa/Niamey':
    ret = bluemind.timezone.AfricaNiamey;
    break;
  case 'Africa/Nouakchott':
    ret = bluemind.timezone.AfricaNouakchott;
    break;
  case 'Africa/Ouagadougou':
    ret = bluemind.timezone.AfricaOuagadougou;
    break;
  case 'Africa/Porto-Novo':
    ret = bluemind.timezone.AfricaPortoNovo;
    break;
  case 'Africa/Sao_Tome':
    ret = bluemind.timezone.AfricaSaoTome;
    break;
  case 'Africa/Tripoli':
    ret = bluemind.timezone.AfricaTripoli;
    break;
  case 'Africa/Tunis':
    ret = bluemind.timezone.AfricaTunis;
    break;
  case 'Africa/Windhoek':
    ret = bluemind.timezone.AfricaWindhoek;
    break;
  case 'America/Adak':
    ret = bluemind.timezone.AmericaAdak;
    break;
  case 'America/Anchorage':
    ret = bluemind.timezone.AmericaAnchorage;
    break;
  case 'America/Anguilla':
    ret = bluemind.timezone.AmericaAnguilla;
    break;
  case 'America/Antigua':
    ret = bluemind.timezone.AmericaAntigua;
    break;
  case 'America/Araguaina':
    ret = bluemind.timezone.AmericaAraguaina;
    break;
  case 'America/Argentina/Buenos_Aires':
    ret = bluemind.timezone.AmericaArgentinaBuenosAires;
    break;
  case 'America/Argentina/Catamarca':
    ret = bluemind.timezone.AmericaArgentinaCatamarca;
    break;
  case 'America/Argentina/Cordoba':
    ret = bluemind.timezone.AmericaArgentinaCordoba;
    break;
  case 'America/Argentina/Jujuy':
    ret = bluemind.timezone.AmericaArgentinaJujuy;
    break;
  case 'America/Argentina/La_Rioja':
    ret = bluemind.timezone.AmericaArgentinaLaRioja;
    break;
  case 'America/Argentina/Mendoza':
    ret = bluemind.timezone.AmericaArgentinaMendoza;
    break;
  case 'America/Argentina/Rio_Gallegos':
    ret = bluemind.timezone.AmericaArgentinaRioGallegos;
    break;
  case 'America/Argentina/Salta':
    ret = bluemind.timezone.AmericaArgentinaSalta;
    break;
  case 'America/Argentina/San_Juan':
    ret = bluemind.timezone.AmericaArgentinaSanJuan;
    break;
  case 'America/Argentina/San_Luis':
    ret = bluemind.timezone.AmericaArgentinaSanLuis;
    break;
  case 'America/Argentina/Tucuman':
    ret = bluemind.timezone.AmericaArgentinaTucuman;
    break;
  case 'America/Argentina/Ushuaia':
    ret = bluemind.timezone.AmericaArgentinaUshuaia;
    break;
  case 'America/Aruba':
    ret = bluemind.timezone.AmericaAruba;
    break;
  case 'America/Asuncion':
    ret = bluemind.timezone.AmericaAsuncion;
    break;
  case 'America/Atikokan':
    ret = bluemind.timezone.AmericaAtikokan;
    break;
  case 'America/Bahia':
    ret = bluemind.timezone.AmericaBahia;
    break;
  case 'America/Bahia_Banderas':
    ret = bluemind.timezone.AmericaBahiaBanderas;
    break;
  case 'America/Barbados':
    ret = bluemind.timezone.AmericaBarbados;
    break;
  case 'America/Belem':
    ret = bluemind.timezone.AmericaBelem;
    break;
  case 'America/Belize':
    ret = bluemind.timezone.AmericaBelize;
    break;
  case 'America/Blanc-Sablon':
    ret = bluemind.timezone.AmericaBlancSablon;
    break;
  case 'America/Boa_Vista':
    ret = bluemind.timezone.AmericaBoaVista;
    break;
  case 'America/Bogota':
    ret = bluemind.timezone.AmericaBogota;
    break;
  case 'America/Boise':
    ret = bluemind.timezone.AmericaBoise;
    break;
  case 'America/Cambridge_Bay':
    ret = bluemind.timezone.AmericaCambridgeBay;
    break;
  case 'America/Campo_Grande':
    ret = bluemind.timezone.AmericaCampoGrande;
    break;
  case 'America/Cancun':
    ret = bluemind.timezone.AmericaCancun;
    break;
  case 'America/Caracas':
    ret = bluemind.timezone.AmericaCaracas;
    break;
  case 'America/Cayenne':
    ret = bluemind.timezone.AmericaCayenne;
    break;
  case 'America/Cayman':
    ret = bluemind.timezone.AmericaCayman;
    break;
  case 'America/Chicago':
    ret = bluemind.timezone.AmericaChicago;
    break;
  case 'America/Chihuahua':
    ret = bluemind.timezone.AmericaChihuahua;
    break;
  case 'America/Costa_Rica':
    ret = bluemind.timezone.AmericaCostaRica;
    break;
  case 'America/Creston':
    ret = bluemind.timezone.AmericaCreston;
    break;
  case 'America/Cuiaba':
    ret = bluemind.timezone.AmericaCuiaba;
    break;
  case 'America/Curacao':
    ret = bluemind.timezone.AmericaCuracao;
    break;
  case 'America/Danmarkshavn':
    ret = bluemind.timezone.AmericaDanmarkshavn;
    break;
  case 'America/Dawson':
    ret = bluemind.timezone.AmericaDawson;
    break;
  case 'America/Dawson_Creek':
    ret = bluemind.timezone.AmericaDawsonCreek;
    break;
  case 'America/Denver':
    ret = bluemind.timezone.AmericaDenver;
    break;
  case 'America/Detroit':
    ret = bluemind.timezone.AmericaDetroit;
    break;
  case 'America/Dominica':
    ret = bluemind.timezone.AmericaDominica;
    break;
  case 'America/Edmonton':
    ret = bluemind.timezone.AmericaEdmonton;
    break;
  case 'America/Eirunepe':
    ret = bluemind.timezone.AmericaEirunepe;
    break;
  case 'America/El_Salvador':
    ret = bluemind.timezone.AmericaElSalvador;
    break;
  case 'America/Fortaleza':
    ret = bluemind.timezone.AmericaFortaleza;
    break;
  case 'America/Glace_Bay':
    ret = bluemind.timezone.AmericaGlaceBay;
    break;
  case 'America/Godthab':
    ret = bluemind.timezone.AmericaGodthab;
    break;
  case 'America/Goose_Bay':
    ret = bluemind.timezone.AmericaGooseBay;
    break;
  case 'America/Grand_Turk':
    ret = bluemind.timezone.AmericaGrandTurk;
    break;
  case 'America/Grenada':
    ret = bluemind.timezone.AmericaGrenada;
    break;
  case 'America/Guadeloupe':
    ret = bluemind.timezone.AmericaGuadeloupe;
    break;
  case 'America/Guatemala':
    ret = bluemind.timezone.AmericaGuatemala;
    break;
  case 'America/Guayaquil':
    ret = bluemind.timezone.AmericaGuayaquil;
    break;
  case 'America/Guyana':
    ret = bluemind.timezone.AmericaGuyana;
    break;
  case 'America/Halifax':
    ret = bluemind.timezone.AmericaHalifax;
    break;
  case 'America/Havana':
    ret = bluemind.timezone.AmericaHavana;
    break;
  case 'America/Hermosillo':
    ret = bluemind.timezone.AmericaHermosillo;
    break;
  case 'America/Indiana/Indianapolis':
    ret = bluemind.timezone.AmericaIndianaIndianapolis;
    break;
  case 'America/Indiana/Knox':
    ret = bluemind.timezone.AmericaIndianaKnox;
    break;
  case 'America/Indiana/Marengo':
    ret = bluemind.timezone.AmericaIndianaMarengo;
    break;
  case 'America/Indiana/Petersburg':
    ret = bluemind.timezone.AmericaIndianaPetersburg;
    break;
  case 'America/Indiana/Tell_City':
    ret = bluemind.timezone.AmericaIndianaTellCity;
    break;
  case 'America/Indiana/Vevay':
    ret = bluemind.timezone.AmericaIndianaVevay;
    break;
  case 'America/Indiana/Vincennes':
    ret = bluemind.timezone.AmericaIndianaVincennes;
    break;
  case 'America/Indiana/Winamac':
    ret = bluemind.timezone.AmericaIndianaWinamac;
    break;
  case 'America/Inuvik':
    ret = bluemind.timezone.AmericaInuvik;
    break;
  case 'America/Iqaluit':
    ret = bluemind.timezone.AmericaIqaluit;
    break;
  case 'America/Jamaica':
    ret = bluemind.timezone.AmericaJamaica;
    break;
  case 'America/Juneau':
    ret = bluemind.timezone.AmericaJuneau;
    break;
  case 'America/Kentucky/Louisville':
    ret = bluemind.timezone.AmericaKentuckyLouisville;
    break;
  case 'America/Kentucky/Monticello':
    ret = bluemind.timezone.AmericaKentuckyMonticello;
    break;
  case 'America/Kralendijk':
    ret = bluemind.timezone.AmericaKralendijk;
    break;
  case 'America/La_Paz':
    ret = bluemind.timezone.AmericaLaPaz;
    break;
  case 'America/Lima':
    ret = bluemind.timezone.AmericaLima;
    break;
  case 'America/Los_Angeles':
    ret = bluemind.timezone.AmericaLosAngeles;
    break;
  case 'America/Lower_Princes':
    ret = bluemind.timezone.AmericaLowerPrinces;
    break;
  case 'America/Maceio':
    ret = bluemind.timezone.AmericaMaceio;
    break;
  case 'America/Managua':
    ret = bluemind.timezone.AmericaManagua;
    break;
  case 'America/Manaus':
    ret = bluemind.timezone.AmericaManaus;
    break;
  case 'America/Marigot':
    ret = bluemind.timezone.AmericaMarigot;
    break;
  case 'America/Martinique':
    ret = bluemind.timezone.AmericaMartinique;
    break;
  case 'America/Matamoros':
    ret = bluemind.timezone.AmericaMatamoros;
    break;
  case 'America/Mazatlan':
    ret = bluemind.timezone.AmericaMazatlan;
    break;
  case 'America/Menominee':
    ret = bluemind.timezone.AmericaMenominee;
    break;
  case 'America/Merida':
    ret = bluemind.timezone.AmericaMerida;
    break;
  case 'America/Metlakatla':
    ret = bluemind.timezone.AmericaMetlakatla;
    break;
  case 'America/Mexico_City':
    ret = bluemind.timezone.AmericaMexicoCity;
    break;
  case 'America/Miquelon':
    ret = bluemind.timezone.AmericaMiquelon;
    break;
  case 'America/Moncton':
    ret = bluemind.timezone.AmericaMoncton;
    break;
  case 'America/Monterrey':
    ret = bluemind.timezone.AmericaMonterrey;
    break;
  case 'America/Montevideo':
    ret = bluemind.timezone.AmericaMontevideo;
    break;
  case 'America/Montreal':
    ret = bluemind.timezone.AmericaMontreal;
    break;
  case 'America/Montserrat':
    ret = bluemind.timezone.AmericaMontserrat;
    break;
  case 'America/Nassau':
    ret = bluemind.timezone.AmericaNassau;
    break;
  case 'America/New_York':
    ret = bluemind.timezone.AmericaNewYork;
    break;
  case 'America/Nipigon':
    ret = bluemind.timezone.AmericaNipigon;
    break;
  case 'America/Nome':
    ret = bluemind.timezone.AmericaNome;
    break;
  case 'America/Noronha':
    ret = bluemind.timezone.AmericaNoronha;
    break;
  case 'America/North_Dakota/Beulah':
    ret = bluemind.timezone.AmericaNorthDakotaBeulah;
    break;
  case 'America/North_Dakota/Center':
    ret = bluemind.timezone.AmericaNorthDakotaCenter;
    break;
  case 'America/North_Dakota/New_Salem':
    ret = bluemind.timezone.AmericaNorthDakotaNewSalem;
    break;
  case 'America/Ojinaga':
    ret = bluemind.timezone.AmericaOjinaga;
    break;
  case 'America/Panama':
    ret = bluemind.timezone.AmericaPanama;
    break;
  case 'America/Pangnirtung':
    ret = bluemind.timezone.AmericaPangnirtung;
    break;
  case 'America/Paramaribo':
    ret = bluemind.timezone.AmericaParamaribo;
    break;
  case 'America/Phoenix':
    ret = bluemind.timezone.AmericaPhoenix;
    break;
  case 'America/Port-au-Prince':
    ret = bluemind.timezone.AmericaPortauPrince;
    break;
  case 'America/Port_of_Spain':
    ret = bluemind.timezone.AmericaPortofSpain;
    break;
  case 'America/Porto_Velho':
    ret = bluemind.timezone.AmericaPortoVelho;
    break;
  case 'America/Puerto_Rico':
    ret = bluemind.timezone.AmericaPuertoRico;
    break;
  case 'America/Rainy_River':
    ret = bluemind.timezone.AmericaRainyRiver;
    break;
  case 'America/Rankin_Inlet':
    ret = bluemind.timezone.AmericaRankinInlet;
    break;
  case 'America/Recife':
    ret = bluemind.timezone.AmericaRecife;
    break;
  case 'America/Regina':
    ret = bluemind.timezone.AmericaRegina;
    break;
  case 'America/Resolute':
    ret = bluemind.timezone.AmericaResolute;
    break;
  case 'America/Rio_Branco':
    ret = bluemind.timezone.AmericaRioBranco;
    break;
  case 'America/Santa_Isabel':
    ret = bluemind.timezone.AmericaSantaIsabel;
    break;
  case 'America/Santarem':
    ret = bluemind.timezone.AmericaSantarem;
    break;
  case 'America/Santiago':
    ret = bluemind.timezone.AmericaSantiago;
    break;
  case 'America/Santo_Domingo':
    ret = bluemind.timezone.AmericaSantoDomingo;
    break;
  case 'America/Sao_Paulo':
    ret = bluemind.timezone.AmericaSaoPaulo;
    break;
  case 'America/Scoresbysund':
    ret = bluemind.timezone.AmericaScoresbysund;
    break;
  case 'America/Shiprock':
    ret = bluemind.timezone.AmericaShiprock;
    break;
  case 'America/Sitka':
    ret = bluemind.timezone.AmericaSitka;
    break;
  case 'America/St_Barthelemy':
    ret = bluemind.timezone.AmericaStBarthelemy;
    break;
  case 'America/St_Johns':
    ret = bluemind.timezone.AmericaStJohns;
    break;
  case 'America/St_Kitts':
    ret = bluemind.timezone.AmericaStKitts;
    break;
  case 'America/St_Lucia':
    ret = bluemind.timezone.AmericaStLucia;
    break;
  case 'America/St_Thomas':
    ret = bluemind.timezone.AmericaStThomas;
    break;
  case 'America/St_Vincent':
    ret = bluemind.timezone.AmericaStVincent;
    break;
  case 'America/Swift_Current':
    ret = bluemind.timezone.AmericaSwiftCurrent;
    break;
  case 'America/Tegucigalpa':
    ret = bluemind.timezone.AmericaTegucigalpa;
    break;
  case 'America/Thule':
    ret = bluemind.timezone.AmericaThule;
    break;
  case 'America/Thunder_Bay':
    ret = bluemind.timezone.AmericaThunderBay;
    break;
  case 'America/Tijuana':
    ret = bluemind.timezone.AmericaTijuana;
    break;
  case 'America/Toronto':
    ret = bluemind.timezone.AmericaToronto;
    break;
  case 'America/Tortola':
    ret = bluemind.timezone.AmericaTortola;
    break;
  case 'America/Vancouver':
    ret = bluemind.timezone.AmericaVancouver;
    break;
  case 'America/Whitehorse':
    ret = bluemind.timezone.AmericaWhitehorse;
    break;
  case 'America/Winnipeg':
    ret = bluemind.timezone.AmericaWinnipeg;
    break;
  case 'America/Yakutat':
    ret = bluemind.timezone.AmericaYakutat;
    break;
  case 'America/Yellowknife':
    ret = bluemind.timezone.AmericaYellowknife;
    break;
  case 'Antarctica/Casey':
    ret = bluemind.timezone.AntarcticaCasey;
    break;
  case 'Antarctica/Davis':
    ret = bluemind.timezone.AntarcticaDavis;
    break;
  case 'Antarctica/DumontDUrville':
    ret = bluemind.timezone.AntarcticaDumontDUrville;
    break;
  case 'Antarctica/Macquarie':
    ret = bluemind.timezone.AntarcticaMacquarie;
    break;
  case 'Antarctica/Mawson':
    ret = bluemind.timezone.AntarcticaMawson;
    break;
  case 'Antarctica/McMurdo':
    ret = bluemind.timezone.AntarcticaMcMurdo;
    break;
  case 'Antarctica/Palmer':
    ret = bluemind.timezone.AntarcticaPalmer;
    break;
  case 'Antarctica/Rothera':
    ret = bluemind.timezone.AntarcticaRothera;
    break;
  case 'Antarctica/South_Pole':
    ret = bluemind.timezone.AntarcticaSouthPole;
    break;
  case 'Antarctica/Syowa':
    ret = bluemind.timezone.AntarcticaSyowa;
    break;
  case 'Antarctica/Vostok':
    ret = bluemind.timezone.AntarcticaVostok;
    break;
  case 'Arctic/Longyearbyen':
    ret = bluemind.timezone.ArcticLongyearbyen;
    break;
  case 'Asia/Aden':
    ret = bluemind.timezone.AsiaAden;
    break;
  case 'Asia/Almaty':
    ret = bluemind.timezone.AsiaAlmaty;
    break;
  case 'Asia/Amman':
    ret = bluemind.timezone.AsiaAmman;
    break;
  case 'Asia/Anadyr':
    ret = bluemind.timezone.AsiaAnadyr;
    break;
  case 'Asia/Aqtau':
    ret = bluemind.timezone.AsiaAqtau;
    break;
  case 'Asia/Aqtobe':
    ret = bluemind.timezone.AsiaAqtobe;
    break;
  case 'Asia/Ashgabat':
    ret = bluemind.timezone.AsiaAshgabat;
    break;
  case 'Asia/Baghdad':
    ret = bluemind.timezone.AsiaBaghdad;
    break;
  case 'Asia/Bahrain':
    ret = bluemind.timezone.AsiaBahrain;
    break;
  case 'Asia/Baku':
    ret = bluemind.timezone.AsiaBaku;
    break;
  case 'Asia/Bangkok':
    ret = bluemind.timezone.AsiaBangkok;
    break;
  case 'Asia/Beirut':
    ret = bluemind.timezone.AsiaBeirut;
    break;
  case 'Asia/Bishkek':
    ret = bluemind.timezone.AsiaBishkek;
    break;
  case 'Asia/Brunei':
    ret = bluemind.timezone.AsiaBrunei;
    break;
  case 'Asia/Choibalsan':
    ret = bluemind.timezone.AsiaChoibalsan;
    break;
  case 'Asia/Chongqing':
    ret = bluemind.timezone.AsiaChongqing;
    break;
  case 'Asia/Colombo':
    ret = bluemind.timezone.AsiaColombo;
    break;
  case 'Asia/Damascus':
    ret = bluemind.timezone.AsiaDamascus;
    break;
  case 'Asia/Dhaka':
    ret = bluemind.timezone.AsiaDhaka;
    break;
  case 'Asia/Dili':
    ret = bluemind.timezone.AsiaDili;
    break;
  case 'Asia/Dubai':
    ret = bluemind.timezone.AsiaDubai;
    break;
  case 'Asia/Dushanbe':
    ret = bluemind.timezone.AsiaDushanbe;
    break;
  case 'Asia/Gaza':
    ret = bluemind.timezone.AsiaGaza;
    break;
  case 'Asia/Harbin':
    ret = bluemind.timezone.AsiaHarbin;
    break;
  case 'Asia/Hebron':
    ret = bluemind.timezone.AsiaHebron;
    break;
  case 'Asia/Ho_Chi_Minh':
    ret = bluemind.timezone.AsiaHoChiMinh;
    break;
  case 'Asia/Hong_Kong':
    ret = bluemind.timezone.AsiaHongKong;
    break;
  case 'Asia/Hovd':
    ret = bluemind.timezone.AsiaHovd;
    break;
  case 'Asia/Irkutsk':
    ret = bluemind.timezone.AsiaIrkutsk;
    break;
  case 'Asia/Jakarta':
    ret = bluemind.timezone.AsiaJakarta;
    break;
  case 'Asia/Jayapura':
    ret = bluemind.timezone.AsiaJayapura;
    break;
  case 'Asia/Jerusalem':
    ret = bluemind.timezone.AsiaJerusalem;
    break;
  case 'Asia/Kabul':
    ret = bluemind.timezone.AsiaKabul;
    break;
  case 'Asia/Kamchatka':
    ret = bluemind.timezone.AsiaKamchatka;
    break;
  case 'Asia/Karachi':
    ret = bluemind.timezone.AsiaKarachi;
    break;
  case 'Asia/Kashgar':
    ret = bluemind.timezone.AsiaKashgar;
    break;
  case 'Asia/Kathmandu':
    ret = bluemind.timezone.AsiaKathmandu;
    break;
  case 'Asia/Kolkata':
    ret = bluemind.timezone.AsiaKolkata;
    break;
  case 'Asia/Krasnoyarsk':
    ret = bluemind.timezone.AsiaKrasnoyarsk;
    break;
  case 'Asia/Kuala_Lumpur':
    ret = bluemind.timezone.AsiaKualaLumpur;
    break;
  case 'Asia/Kuching':
    ret = bluemind.timezone.AsiaKuching;
    break;
  case 'Asia/Kuwait':
    ret = bluemind.timezone.AsiaKuwait;
    break;
  case 'Asia/Macau':
    ret = bluemind.timezone.AsiaMacau;
    break;
  case 'Asia/Magadan':
    ret = bluemind.timezone.AsiaMagadan;
    break;
  case 'Asia/Makassar':
    ret = bluemind.timezone.AsiaMakassar;
    break;
  case 'Asia/Manila':
    ret = bluemind.timezone.AsiaManila;
    break;
  case 'Asia/Muscat':
    ret = bluemind.timezone.AsiaMuscat;
    break;
  case 'Asia/Nicosia':
    ret = bluemind.timezone.AsiaNicosia;
    break;
  case 'Asia/Novokuznetsk':
    ret = bluemind.timezone.AsiaNovokuznetsk;
    break;
  case 'Asia/Novosibirsk':
    ret = bluemind.timezone.AsiaNovosibirsk;
    break;
  case 'Asia/Omsk':
    ret = bluemind.timezone.AsiaOmsk;
    break;
  case 'Asia/Oral':
    ret = bluemind.timezone.AsiaOral;
    break;
  case 'Asia/Phnom_Penh':
    ret = bluemind.timezone.AsiaPhnomPenh;
    break;
  case 'Asia/Pontianak':
    ret = bluemind.timezone.AsiaPontianak;
    break;
  case 'Asia/Pyongyang':
    ret = bluemind.timezone.AsiaPyongyang;
    break;
  case 'Asia/Qatar':
    ret = bluemind.timezone.AsiaQatar;
    break;
  case 'Asia/Qyzylorda':
    ret = bluemind.timezone.AsiaQyzylorda;
    break;
  case 'Asia/Rangoon':
    ret = bluemind.timezone.AsiaRangoon;
    break;
  case 'Asia/Riyadh':
    ret = bluemind.timezone.AsiaRiyadh;
    break;
  case 'Asia/Sakhalin':
    ret = bluemind.timezone.AsiaSakhalin;
    break;
  case 'Asia/Samarkand':
    ret = bluemind.timezone.AsiaSamarkand;
    break;
  case 'Asia/Seoul':
    ret = bluemind.timezone.AsiaSeoul;
    break;
  case 'Asia/Shanghai':
    ret = bluemind.timezone.AsiaShanghai;
    break;
  case 'Asia/Singapore':
    ret = bluemind.timezone.AsiaSingapore;
    break;
  case 'Asia/Taipei':
    ret = bluemind.timezone.AsiaTaipei;
    break;
  case 'Asia/Tashkent':
    ret = bluemind.timezone.AsiaTashkent;
    break;
  case 'Asia/Tbilisi':
    ret = bluemind.timezone.AsiaTbilisi;
    break;
  case 'Asia/Tehran':
    ret = bluemind.timezone.AsiaTehran;
    break;
  case 'Asia/Thimphu':
    ret = bluemind.timezone.AsiaThimphu;
    break;
  case 'Asia/Tokyo':
    ret = bluemind.timezone.AsiaTokyo;
    break;
  case 'Asia/Ulaanbaatar':
    ret = bluemind.timezone.AsiaUlaanbaatar;
    break;
  case 'Asia/Urumqi':
    ret = bluemind.timezone.AsiaUrumqi;
    break;
  case 'Asia/Vientiane':
    ret = bluemind.timezone.AsiaVientiane;
    break;
  case 'Asia/Vladivostok':
    ret = bluemind.timezone.AsiaVladivostok;
    break;
  case 'Asia/Yakutsk':
    ret = bluemind.timezone.AsiaYakutsk;
    break;
  case 'Asia/Yekaterinburg':
    ret = bluemind.timezone.AsiaYekaterinburg;
    break;
  case 'Asia/Yerevan':
    ret = bluemind.timezone.AsiaYerevan;
    break;
  case 'Atlantic/Azores':
    ret = bluemind.timezone.AtlanticAzores;
    break;
  case 'Atlantic/Bermuda':
    ret = bluemind.timezone.AtlanticBermuda;
    break;
  case 'Atlantic/Canary':
    ret = bluemind.timezone.AtlanticCanary;
    break;
  case 'Atlantic/Cape_Verde':
    ret = bluemind.timezone.AtlanticCapeVerde;
    break;
  case 'Atlantic/Faroe':
    ret = bluemind.timezone.AtlanticFaroe;
    break;
  case 'Atlantic/Madeira':
    ret = bluemind.timezone.AtlanticMadeira;
    break;
  case 'Atlantic/Reykjavik':
    ret = bluemind.timezone.AtlanticReykjavik;
    break;
  case 'Atlantic/South_Georgia':
    ret = bluemind.timezone.AtlanticSouthGeorgia;
    break;
  case 'Atlantic/St_Helena':
    ret = bluemind.timezone.AtlanticStHelena;
    break;
  case 'Atlantic/Stanley':
    ret = bluemind.timezone.AtlanticStanley;
    break;
  case 'Australia/Adelaide':
    ret = bluemind.timezone.AustraliaAdelaide;
    break;
  case 'Australia/Brisbane':
    ret = bluemind.timezone.AustraliaBrisbane;
    break;
  case 'Australia/Broken_Hill':
    ret = bluemind.timezone.AustraliaBrokenHill;
    break;
  case 'Australia/Currie':
    ret = bluemind.timezone.AustraliaCurrie;
    break;
  case 'Australia/Darwin':
    ret = bluemind.timezone.AustraliaDarwin;
    break;
  case 'Australia/Eucla':
    ret = bluemind.timezone.AustraliaEucla;
    break;
  case 'Australia/Hobart':
    ret = bluemind.timezone.AustraliaHobart;
    break;
  case 'Australia/Lindeman':
    ret = bluemind.timezone.AustraliaLindeman;
    break;
  case 'Australia/Lord_Howe':
    ret = bluemind.timezone.AustraliaLordHowe;
    break;
  case 'Australia/Melbourne':
    ret = bluemind.timezone.AustraliaMelbourne;
    break;
  case 'Australia/Perth':
    ret = bluemind.timezone.AustraliaPerth;
    break;
  case 'Australia/Sydney':
    ret = bluemind.timezone.AustraliaSydney;
    break;
  case 'Europe/Amsterdam':
    ret = bluemind.timezone.EuropeAmsterdam;
    break;
  case 'Europe/Andorra':
    ret = bluemind.timezone.EuropeAndorra;
    break;
  case 'Europe/Athens':
    ret = bluemind.timezone.EuropeAthens;
    break;
  case 'Europe/Belgrade':
    ret = bluemind.timezone.EuropeBelgrade;
    break;
  case 'Europe/Berlin':
    ret = bluemind.timezone.EuropeBerlin;
    break;
  case 'Europe/Bratislava':
    ret = bluemind.timezone.EuropeBratislava;
    break;
  case 'Europe/Brussels':
    ret = bluemind.timezone.EuropeBrussels;
    break;
  case 'Europe/Bucharest':
    ret = bluemind.timezone.EuropeBucharest;
    break;
  case 'Europe/Budapest':
    ret = bluemind.timezone.EuropeBudapest;
    break;
  case 'Europe/Chisinau':
    ret = bluemind.timezone.EuropeChisinau;
    break;
  case 'Europe/Copenhagen':
    ret = bluemind.timezone.EuropeCopenhagen;
    break;
  case 'Europe/Dublin':
    ret = bluemind.timezone.EuropeDublin;
    break;
  case 'Europe/Gibraltar':
    ret = bluemind.timezone.EuropeGibraltar;
    break;
  case 'Europe/Guernsey':
    ret = bluemind.timezone.EuropeGuernsey;
    break;
  case 'Europe/Helsinki':
    ret = bluemind.timezone.EuropeHelsinki;
    break;
  case 'Europe/Isle_of_Man':
    ret = bluemind.timezone.EuropeIsleofMan;
    break;
  case 'Europe/Istanbul':
    ret = bluemind.timezone.EuropeIstanbul;
    break;
  case 'Europe/Jersey':
    ret = bluemind.timezone.EuropeJersey;
    break;
  case 'Europe/Kaliningrad':
    ret = bluemind.timezone.EuropeKaliningrad;
    break;
  case 'Europe/Kiev':
    ret = bluemind.timezone.EuropeKiev;
    break;
  case 'Europe/Lisbon':
    ret = bluemind.timezone.EuropeLisbon;
    break;
  case 'Europe/Ljubljana':
    ret = bluemind.timezone.EuropeLjubljana;
    break;
  case 'Europe/London':
    ret = bluemind.timezone.EuropeLondon;
    break;
  case 'Europe/Luxembourg':
    ret = bluemind.timezone.EuropeLuxembourg;
    break;
  case 'Europe/Madrid':
    ret = bluemind.timezone.EuropeMadrid;
    break;
  case 'Europe/Malta':
    ret = bluemind.timezone.EuropeMalta;
    break;
  case 'Europe/Mariehamn':
    ret = bluemind.timezone.EuropeMariehamn;
    break;
  case 'Europe/Minsk':
    ret = bluemind.timezone.EuropeMinsk;
    break;
  case 'Europe/Monaco':
    ret = bluemind.timezone.EuropeMonaco;
    break;
  case 'Europe/Moscow':
    ret = bluemind.timezone.EuropeMoscow;
    break;
  case 'Europe/Oslo':
    ret = bluemind.timezone.EuropeOslo;
    break;
  case 'Europe/Paris':
    ret = bluemind.timezone.EuropeParis;
    break;
  case 'Europe/Podgorica':
    ret = bluemind.timezone.EuropePodgorica;
    break;
  case 'Europe/Prague':
    ret = bluemind.timezone.EuropePrague;
    break;
  case 'Europe/Riga':
    ret = bluemind.timezone.EuropeRiga;
    break;
  case 'Europe/Rome':
    ret = bluemind.timezone.EuropeRome;
    break;
  case 'Europe/Samara':
    ret = bluemind.timezone.EuropeSamara;
    break;
  case 'Europe/San_Marino':
    ret = bluemind.timezone.EuropeSanMarino;
    break;
  case 'Europe/Sarajevo':
    ret = bluemind.timezone.EuropeSarajevo;
    break;
  case 'Europe/Simferopol':
    ret = bluemind.timezone.EuropeSimferopol;
    break;
  case 'Europe/Skopje':
    ret = bluemind.timezone.EuropeSkopje;
    break;
  case 'Europe/Sofia':
    ret = bluemind.timezone.EuropeSofia;
    break;
  case 'Europe/Stockholm':
    ret = bluemind.timezone.EuropeStockholm;
    break;
  case 'Europe/Tallinn':
    ret = bluemind.timezone.EuropeTallinn;
    break;
  case 'Europe/Tirane':
    ret = bluemind.timezone.EuropeTirane;
    break;
  case 'Europe/Uzhgorod':
    ret = bluemind.timezone.EuropeUzhgorod;
    break;
  case 'Europe/Vaduz':
    ret = bluemind.timezone.EuropeVaduz;
    break;
  case 'Europe/Vatican':
    ret = bluemind.timezone.EuropeVatican;
    break;
  case 'Europe/Vienna':
    ret = bluemind.timezone.EuropeVienna;
    break;
  case 'Europe/Vilnius':
    ret = bluemind.timezone.EuropeVilnius;
    break;
  case 'Europe/Volgograd':
    ret = bluemind.timezone.EuropeVolgograd;
    break;
  case 'Europe/Warsaw':
    ret = bluemind.timezone.EuropeWarsaw;
    break;
  case 'Europe/Zagreb':
    ret = bluemind.timezone.EuropeZagreb;
    break;
  case 'Europe/Zaporozhye':
    ret = bluemind.timezone.EuropeZaporozhye;
    break;
  case 'Europe/Zurich':
    ret = bluemind.timezone.EuropeZurich;
    break;
  case 'Indian/Antananarivo':
    ret = bluemind.timezone.IndianAntananarivo;
    break;
  case 'Indian/Chagos':
    ret = bluemind.timezone.IndianChagos;
    break;
  case 'Indian/Christmas':
    ret = bluemind.timezone.IndianChristmas;
    break;
  case 'Indian/Cocos':
    ret = bluemind.timezone.IndianCocos;
    break;
  case 'Indian/Comoro':
    ret = bluemind.timezone.IndianComoro;
    break;
  case 'Indian/Kerguelen':
    ret = bluemind.timezone.IndianKerguelen;
    break;
  case 'Indian/Mahe':
    ret = bluemind.timezone.IndianMahe;
    break;
  case 'Indian/Maldives':
    ret = bluemind.timezone.IndianMaldives;
    break;
  case 'Indian/Mauritius':
    ret = bluemind.timezone.IndianMauritius;
    break;
  case 'Indian/Mayotte':
    ret = bluemind.timezone.IndianMayotte;
    break;
  case 'Indian/Reunion':
    ret = bluemind.timezone.IndianReunion;
    break;
  case 'Pacific/Apia':
    ret = bluemind.timezone.PacificApia;
    break;
  case 'Pacific/Auckland':
    ret = bluemind.timezone.PacificAuckland;
    break;
  case 'Pacific/Chatham':
    ret = bluemind.timezone.PacificChatham;
    break;
  case 'Pacific/Chuuk':
    ret = bluemind.timezone.PacificChuuk;
    break;
  case 'Pacific/Easter':
    ret = bluemind.timezone.PacificEaster;
    break;
  case 'Pacific/Efate':
    ret = bluemind.timezone.PacificEfate;
    break;
  case 'Pacific/Enderbury':
    ret = bluemind.timezone.PacificEnderbury;
    break;
  case 'Pacific/Fakaofo':
    ret = bluemind.timezone.PacificFakaofo;
    break;
  case 'Pacific/Fiji':
    ret = bluemind.timezone.PacificFiji;
    break;
  case 'Pacific/Funafuti':
    ret = bluemind.timezone.PacificFunafuti;
    break;
  case 'Pacific/Galapagos':
    ret = bluemind.timezone.PacificGalapagos;
    break;
  case 'Pacific/Gambier':
    ret = bluemind.timezone.PacificGambier;
    break;
  case 'Pacific/Guadalcanal':
    ret = bluemind.timezone.PacificGuadalcanal;
    break;
  case 'Pacific/Guam':
    ret = bluemind.timezone.PacificGuam;
    break;
  case 'Pacific/Honolulu':
    ret = bluemind.timezone.PacificHonolulu;
    break;
  case 'Pacific/Johnston':
    ret = bluemind.timezone.PacificJohnston;
    break;
  case 'Pacific/Kiritimati':
    ret = bluemind.timezone.PacificKiritimati;
    break;
  case 'Pacific/Kosrae':
    ret = bluemind.timezone.PacificKosrae;
    break;
  case 'Pacific/Kwajalein':
    ret = bluemind.timezone.PacificKwajalein;
    break;
  case 'Pacific/Majuro':
    ret = bluemind.timezone.PacificMajuro;
    break;
  case 'Pacific/Marquesas':
    ret = bluemind.timezone.PacificMarquesas;
    break;
  case 'Pacific/Midway':
    ret = bluemind.timezone.PacificMidway;
    break;
  case 'Pacific/Nauru':
    ret = bluemind.timezone.PacificNauru;
    break;
  case 'Pacific/Niue':
    ret = bluemind.timezone.PacificNiue;
    break;
  case 'Pacific/Norfolk':
    ret = bluemind.timezone.PacificNorfolk;
    break;
  case 'Pacific/Noumea':
    ret = bluemind.timezone.PacificNoumea;
    break;
  case 'Pacific/Pago_Pago':
    ret = bluemind.timezone.PacificPagoPago;
    break;
  case 'Pacific/Palau':
    ret = bluemind.timezone.PacificPalau;
    break;
  case 'Pacific/Pitcairn':
    ret = bluemind.timezone.PacificPitcairn;
    break;
  case 'Pacific/Pohnpei':
    ret = bluemind.timezone.PacificPohnpei;
    break;
  case 'Pacific/Port_Moresby':
    ret = bluemind.timezone.PacificPortMoresby;
    break;
  case 'Pacific/Rarotonga':
    ret = bluemind.timezone.PacificRarotonga;
    break;
  case 'Pacific/Saipan':
    ret = bluemind.timezone.PacificSaipan;
    break;
  case 'Pacific/Tahiti':
    ret = bluemind.timezone.PacificTahiti;
    break;
  case 'Pacific/Tarawa':
    ret = bluemind.timezone.PacificTarawa;
    break;
  case 'Pacific/Tongatapu':
    ret = bluemind.timezone.PacificTongatapu;
    break;
  case 'Pacific/Wake':
    ret = bluemind.timezone.PacificWake;
    break;
  case 'Pacific/Wallis':
    ret = bluemind.timezone.PacificWallis;
    break;
  case 'UTC':
    ret = bluemind.timezone.UTC;
    break;
  default:
    return null;
  }
  return ret;
};

/**
 * Default timezone
 * 
 * @type {goog.i18n.TimeZone}
 */
net.bluemind.timezone.DEFAULT = goog.i18n.TimeZone.createTimeZone(net.bluemind.timezone.TimeZoneHelper
    .getRawData('Europe/Paris'));

/**
 * UTC timezone
 * 
 * @type {goog.i18n.TimeZone}
 */
net.bluemind.timezone.UTC = goog.i18n.TimeZone.createTimeZone(net.bluemind.timezone.TimeZoneHelper.getRawData('UTC'));
