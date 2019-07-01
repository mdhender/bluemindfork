/**
 * BEGIN LICENSE
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

/**
 * @fileoverview timezone detector.
 */

goog.provide('bluemind.timezone.Detector');

/**
 * @constructor
 */
bluemind.timezone.Detector = function() {

};
goog.addSingletonGetter(bluemind.timezone.Detector);

/**
 * @param {string} timezone timezone identifier.
 * @return {Object} timezone data.
 */
bluemind.timezone.Detector.prototype.get = function(timezone) {
  var ret;
  switch (timezone) {
    case 'Africa/Abidjan':
      goog.require('bluemind.timezone.AfricaAbidjan');
      ret = bluemind.timezone.AfricaAbidjan;
      break;
    case 'Africa/Accra':
      goog.require('bluemind.timezone.AfricaAccra');
      ret = bluemind.timezone.AfricaAccra;
      break;
    case 'Africa/Addis_Ababa':
      goog.require('bluemind.timezone.AfricaAddisAbaba');
      ret = bluemind.timezone.AfricaAddisAbaba;
      break;
    case 'Africa/Algiers':
      goog.require('bluemind.timezone.AfricaAlgiers');
      ret = bluemind.timezone.AfricaAlgiers;
      break;
    case 'Africa/Asmara':
      goog.require('bluemind.timezone.AfricaAsmara');
      ret = bluemind.timezone.AfricaAsmara;
      break;
    case 'Africa/Bamako':
      goog.require('bluemind.timezone.AfricaBamako');
      ret = bluemind.timezone.AfricaBamako;
      break;
    case 'Africa/Bangui':
      goog.require('bluemind.timezone.AfricaBangui');
      ret = bluemind.timezone.AfricaBangui;
      break;
    case 'Africa/Banjul':
      goog.require('bluemind.timezone.AfricaBanjul');
      ret = bluemind.timezone.AfricaBanjul;
      break;
    case 'Africa/Bissau':
      goog.require('bluemind.timezone.AfricaBissau');
      ret = bluemind.timezone.AfricaBissau;
      break;
    case 'Africa/Blantyre':
      goog.require('bluemind.timezone.AfricaBlantyre');
      ret = bluemind.timezone.AfricaBlantyre;
      break;
    case 'Africa/Brazzaville':
      goog.require('bluemind.timezone.AfricaBrazzaville');
      ret = bluemind.timezone.AfricaBrazzaville;
      break;
    case 'Africa/Bujumbura':
      goog.require('bluemind.timezone.AfricaBujumbura');
      ret = bluemind.timezone.AfricaBujumbura;
      break;
    case 'Africa/Cairo':
      goog.require('bluemind.timezone.AfricaCairo');
      ret = bluemind.timezone.AfricaCairo;
      break;
    case 'Africa/Casablanca':
      goog.require('bluemind.timezone.AfricaCasablanca');
      ret = bluemind.timezone.AfricaCasablanca;
      break;
    case 'Africa/Ceuta':
      goog.require('bluemind.timezone.AfricaCeuta');
      ret = bluemind.timezone.AfricaCeuta;
      break;
    case 'Africa/Conakry':
      goog.require('bluemind.timezone.AfricaConakry');
      ret = bluemind.timezone.AfricaConakry;
      break;
    case 'Africa/Dakar':
      goog.require('bluemind.timezone.AfricaDakar');
      ret = bluemind.timezone.AfricaDakar;
      break;
    case 'Africa/Dar_es_Salaam':
      goog.require('bluemind.timezone.AfricaDaresSalaam');
      ret = bluemind.timezone.AfricaDaresSalaam;
      break;
    case 'Africa/Djibouti':
      goog.require('bluemind.timezone.AfricaDjibouti');
      ret = bluemind.timezone.AfricaDjibouti;
      break;
    case 'Africa/Douala':
      goog.require('bluemind.timezone.AfricaDouala');
      ret = bluemind.timezone.AfricaDouala;
      break;
    case 'Africa/El_Aaiun':
      goog.require('bluemind.timezone.AfricaElAaiun');
      ret = bluemind.timezone.AfricaElAaiun;
      break;
    case 'Africa/Freetown':
      goog.require('bluemind.timezone.AfricaFreetown');
      ret = bluemind.timezone.AfricaFreetown;
      break;
    case 'Africa/Gaborone':
      goog.require('bluemind.timezone.AfricaGaborone');
      ret = bluemind.timezone.AfricaGaborone;
      break;
    case 'Africa/Harare':
      goog.require('bluemind.timezone.AfricaHarare');
      ret = bluemind.timezone.AfricaHarare;
      break;
    case 'Africa/Johannesburg':
      goog.require('bluemind.timezone.AfricaJohannesburg');
      ret = bluemind.timezone.AfricaJohannesburg;
      break;
    case 'Africa/Juba':
      goog.require('bluemind.timezone.AfricaJuba');
      ret = bluemind.timezone.AfricaJuba;
      break;
    case 'Africa/Kampala':
      goog.require('bluemind.timezone.AfricaKampala');
      ret = bluemind.timezone.AfricaKampala;
      break;
    case 'Africa/Khartoum':
      goog.require('bluemind.timezone.AfricaKhartoum');
      ret = bluemind.timezone.AfricaKhartoum;
      break;
    case 'Africa/Kigali':
      goog.require('bluemind.timezone.AfricaKigali');
      ret = bluemind.timezone.AfricaKigali;
      break;
    case 'Africa/Kinshasa':
      goog.require('bluemind.timezone.AfricaKinshasa');
      ret = bluemind.timezone.AfricaKinshasa;
      break;
    case 'Africa/Lagos':
      goog.require('bluemind.timezone.AfricaLagos');
      ret = bluemind.timezone.AfricaLagos;
      break;
    case 'Africa/Libreville':
      goog.require('bluemind.timezone.AfricaLibreville');
      ret = bluemind.timezone.AfricaLibreville;
      break;
    case 'Africa/Lome':
      goog.require('bluemind.timezone.AfricaLome');
      ret = bluemind.timezone.AfricaLome;
      break;
    case 'Africa/Luanda':
      goog.require('bluemind.timezone.AfricaLuanda');
      ret = bluemind.timezone.AfricaLuanda;
      break;
    case 'Africa/Lubumbashi':
      goog.require('bluemind.timezone.AfricaLubumbashi');
      ret = bluemind.timezone.AfricaLubumbashi;
      break;
    case 'Africa/Lusaka':
      goog.require('bluemind.timezone.AfricaLusaka');
      ret = bluemind.timezone.AfricaLusaka;
      break;
    case 'Africa/Malabo':
      goog.require('bluemind.timezone.AfricaMalabo');
      ret = bluemind.timezone.AfricaMalabo;
      break;
    case 'Africa/Maputo':
      goog.require('bluemind.timezone.AfricaMaputo');
      ret = bluemind.timezone.AfricaMaputo;
      break;
    case 'Africa/Maseru':
      goog.require('bluemind.timezone.AfricaMaseru');
      ret = bluemind.timezone.AfricaMaseru;
      break;
    case 'Africa/Mbabane':
      goog.require('bluemind.timezone.AfricaMbabane');
      ret = bluemind.timezone.AfricaMbabane;
      break;
    case 'Africa/Mogadishu':
      goog.require('bluemind.timezone.AfricaMogadishu');
      ret = bluemind.timezone.AfricaMogadishu;
      break;
    case 'Africa/Monrovia':
      goog.require('bluemind.timezone.AfricaMonrovia');
      ret = bluemind.timezone.AfricaMonrovia;
      break;
    case 'Africa/Nairobi':
      goog.require('bluemind.timezone.AfricaNairobi');
      ret = bluemind.timezone.AfricaNairobi;
      break;
    case 'Africa/Ndjamena':
      goog.require('bluemind.timezone.AfricaNdjamena');
      ret = bluemind.timezone.AfricaNdjamena;
      break;
    case 'Africa/Niamey':
      goog.require('bluemind.timezone.AfricaNiamey');
      ret = bluemind.timezone.AfricaNiamey;
      break;
    case 'Africa/Nouakchott':
      goog.require('bluemind.timezone.AfricaNouakchott');
      ret = bluemind.timezone.AfricaNouakchott;
      break;
    case 'Africa/Ouagadougou':
      goog.require('bluemind.timezone.AfricaOuagadougou');
      ret = bluemind.timezone.AfricaOuagadougou;
      break;
    case 'Africa/Porto-Novo':
      goog.require('bluemind.timezone.AfricaPortoNovo');
      ret = bluemind.timezone.AfricaPortoNovo;
      break;
    case 'Africa/Sao_Tome':
      goog.require('bluemind.timezone.AfricaSaoTome');
      ret = bluemind.timezone.AfricaSaoTome;
      break;
    case 'Africa/Tripoli':
      goog.require('bluemind.timezone.AfricaTripoli');
      ret = bluemind.timezone.AfricaTripoli;
      break;
    case 'Africa/Tunis':
      goog.require('bluemind.timezone.AfricaTunis');
      ret = bluemind.timezone.AfricaTunis;
      break;
    case 'Africa/Windhoek':
      goog.require('bluemind.timezone.AfricaWindhoek');
      ret = bluemind.timezone.AfricaWindhoek;
      break;
    case 'America/Adak':
      goog.require('bluemind.timezone.AmericaAdak');
      ret = bluemind.timezone.AmericaAdak;
      break;
    case 'America/Anchorage':
      goog.require('bluemind.timezone.AmericaAnchorage');
      ret = bluemind.timezone.AmericaAnchorage;
      break;
    case 'America/Anguilla':
      goog.require('bluemind.timezone.AmericaAnguilla');
      ret = bluemind.timezone.AmericaAnguilla;
      break;
    case 'America/Antigua':
      goog.require('bluemind.timezone.AmericaAntigua');
      ret = bluemind.timezone.AmericaAntigua;
      break;
    case 'America/Araguaina':
      goog.require('bluemind.timezone.AmericaAraguaina');
      ret = bluemind.timezone.AmericaAraguaina;
      break;
    case 'America/Argentina/Buenos_Aires':
      goog.require('bluemind.timezone.AmericaArgentinaBuenosAires');
      ret = bluemind.timezone.AmericaArgentinaBuenosAires;
      break;
    case 'America/Argentina/Catamarca':
      goog.require('bluemind.timezone.AmericaArgentinaCatamarca');
      ret = bluemind.timezone.AmericaArgentinaCatamarca;
      break;
    case 'America/Argentina/Cordoba':
      goog.require('bluemind.timezone.AmericaArgentinaCordoba');
      ret = bluemind.timezone.AmericaArgentinaCordoba;
      break;
    case 'America/Argentina/Jujuy':
      goog.require('bluemind.timezone.AmericaArgentinaJujuy');
      ret = bluemind.timezone.AmericaArgentinaJujuy;
      break;
    case 'America/Argentina/La_Rioja':
      goog.require('bluemind.timezone.AmericaArgentinaLaRioja');
      ret = bluemind.timezone.AmericaArgentinaLaRioja;
      break;
    case 'America/Argentina/Mendoza':
      goog.require('bluemind.timezone.AmericaArgentinaMendoza');
      ret = bluemind.timezone.AmericaArgentinaMendoza;
      break;
    case 'America/Argentina/Rio_Gallegos':
      goog.require('bluemind.timezone.AmericaArgentinaRioGallegos');
      ret = bluemind.timezone.AmericaArgentinaRioGallegos;
      break;
    case 'America/Argentina/Salta':
      goog.require('bluemind.timezone.AmericaArgentinaSalta');
      ret = bluemind.timezone.AmericaArgentinaSalta;
      break;
    case 'America/Argentina/San_Juan':
      goog.require('bluemind.timezone.AmericaArgentinaSanJuan');
      ret = bluemind.timezone.AmericaArgentinaSanJuan;
      break;
    case 'America/Argentina/San_Luis':
      goog.require('bluemind.timezone.AmericaArgentinaSanLuis');
      ret = bluemind.timezone.AmericaArgentinaSanLuis;
      break;
    case 'America/Argentina/Tucuman':
      goog.require('bluemind.timezone.AmericaArgentinaTucuman');
      ret = bluemind.timezone.AmericaArgentinaTucuman;
      break;
    case 'America/Argentina/Ushuaia':
      goog.require('bluemind.timezone.AmericaArgentinaUshuaia');
      ret = bluemind.timezone.AmericaArgentinaUshuaia;
      break;
    case 'America/Aruba':
      goog.require('bluemind.timezone.AmericaAruba');
      ret = bluemind.timezone.AmericaAruba;
      break;
    case 'America/Asuncion':
      goog.require('bluemind.timezone.AmericaAsuncion');
      ret = bluemind.timezone.AmericaAsuncion;
      break;
    case 'America/Atikokan':
      goog.require('bluemind.timezone.AmericaAtikokan');
      ret = bluemind.timezone.AmericaAtikokan;
      break;
    case 'America/Bahia':
      goog.require('bluemind.timezone.AmericaBahia');
      ret = bluemind.timezone.AmericaBahia;
      break;
    case 'America/Bahia_Banderas':
      goog.require('bluemind.timezone.AmericaBahiaBanderas');
      ret = bluemind.timezone.AmericaBahiaBanderas;
      break;
    case 'America/Barbados':
      goog.require('bluemind.timezone.AmericaBarbados');
      ret = bluemind.timezone.AmericaBarbados;
      break;
    case 'America/Belem':
      goog.require('bluemind.timezone.AmericaBelem');
      ret = bluemind.timezone.AmericaBelem;
      break;
    case 'America/Belize':
      goog.require('bluemind.timezone.AmericaBelize');
      ret = bluemind.timezone.AmericaBelize;
      break;
    case 'America/Blanc-Sablon':
      goog.require('bluemind.timezone.AmericaBlancSablon');
      ret = bluemind.timezone.AmericaBlancSablon;
      break;
    case 'America/Boa_Vista':
      goog.require('bluemind.timezone.AmericaBoaVista');
      ret = bluemind.timezone.AmericaBoaVista;
      break;
    case 'America/Bogota':
      goog.require('bluemind.timezone.AmericaBogota');
      ret = bluemind.timezone.AmericaBogota;
      break;
    case 'America/Boise':
      goog.require('bluemind.timezone.AmericaBoise');
      ret = bluemind.timezone.AmericaBoise;
      break;
    case 'America/Cambridge_Bay':
      goog.require('bluemind.timezone.AmericaCambridgeBay');
      ret = bluemind.timezone.AmericaCambridgeBay;
      break;
    case 'America/Campo_Grande':
      goog.require('bluemind.timezone.AmericaCampoGrande');
      ret = bluemind.timezone.AmericaCampoGrande;
      break;
    case 'America/Cancun':
      goog.require('bluemind.timezone.AmericaCancun');
      ret = bluemind.timezone.AmericaCancun;
      break;
    case 'America/Caracas':
      goog.require('bluemind.timezone.AmericaCaracas');
      ret = bluemind.timezone.AmericaCaracas;
      break;
    case 'America/Cayenne':
      goog.require('bluemind.timezone.AmericaCayenne');
      ret = bluemind.timezone.AmericaCayenne;
      break;
    case 'America/Cayman':
      goog.require('bluemind.timezone.AmericaCayman');
      ret = bluemind.timezone.AmericaCayman;
      break;
    case 'America/Chicago':
      goog.require('bluemind.timezone.AmericaChicago');
      ret = bluemind.timezone.AmericaChicago;
      break;
    case 'America/Chihuahua':
      goog.require('bluemind.timezone.AmericaChihuahua');
      ret = bluemind.timezone.AmericaChihuahua;
      break;
    case 'America/Costa_Rica':
      goog.require('bluemind.timezone.AmericaCostaRica');
      ret = bluemind.timezone.AmericaCostaRica;
      break;
    case 'America/Creston':
      goog.require('bluemind.timezone.AmericaCreston');
      ret = bluemind.timezone.AmericaCreston;
      break;
    case 'America/Cuiaba':
      goog.require('bluemind.timezone.AmericaCuiaba');
      ret = bluemind.timezone.AmericaCuiaba;
      break;
    case 'America/Curacao':
      goog.require('bluemind.timezone.AmericaCuracao');
      ret = bluemind.timezone.AmericaCuracao;
      break;
    case 'America/Danmarkshavn':
      goog.require('bluemind.timezone.AmericaDanmarkshavn');
      ret = bluemind.timezone.AmericaDanmarkshavn;
      break;
    case 'America/Dawson':
      goog.require('bluemind.timezone.AmericaDawson');
      ret = bluemind.timezone.AmericaDawson;
      break;
    case 'America/Dawson_Creek':
      goog.require('bluemind.timezone.AmericaDawsonCreek');
      ret = bluemind.timezone.AmericaDawsonCreek;
      break;
    case 'America/Denver':
      goog.require('bluemind.timezone.AmericaDenver');
      ret = bluemind.timezone.AmericaDenver;
      break;
    case 'America/Detroit':
      goog.require('bluemind.timezone.AmericaDetroit');
      ret = bluemind.timezone.AmericaDetroit;
      break;
    case 'America/Dominica':
      goog.require('bluemind.timezone.AmericaDominica');
      ret = bluemind.timezone.AmericaDominica;
      break;
    case 'America/Edmonton':
      goog.require('bluemind.timezone.AmericaEdmonton');
      ret = bluemind.timezone.AmericaEdmonton;
      break;
    case 'America/Eirunepe':
      goog.require('bluemind.timezone.AmericaEirunepe');
      ret = bluemind.timezone.AmericaEirunepe;
      break;
    case 'America/El_Salvador':
      goog.require('bluemind.timezone.AmericaElSalvador');
      ret = bluemind.timezone.AmericaElSalvador;
      break;
    case 'America/Fortaleza':
      goog.require('bluemind.timezone.AmericaFortaleza');
      ret = bluemind.timezone.AmericaFortaleza;
      break;
    case 'America/Glace_Bay':
      goog.require('bluemind.timezone.AmericaGlaceBay');
      ret = bluemind.timezone.AmericaGlaceBay;
      break;
    case 'America/Godthab':
      goog.require('bluemind.timezone.AmericaGodthab');
      ret = bluemind.timezone.AmericaGodthab;
      break;
    case 'America/Goose_Bay':
      goog.require('bluemind.timezone.AmericaGooseBay');
      ret = bluemind.timezone.AmericaGooseBay;
      break;
    case 'America/Grand_Turk':
      goog.require('bluemind.timezone.AmericaGrandTurk');
      ret = bluemind.timezone.AmericaGrandTurk;
      break;
    case 'America/Grenada':
      goog.require('bluemind.timezone.AmericaGrenada');
      ret = bluemind.timezone.AmericaGrenada;
      break;
    case 'America/Guadeloupe':
      goog.require('bluemind.timezone.AmericaGuadeloupe');
      ret = bluemind.timezone.AmericaGuadeloupe;
      break;
    case 'America/Guatemala':
      goog.require('bluemind.timezone.AmericaGuatemala');
      ret = bluemind.timezone.AmericaGuatemala;
      break;
    case 'America/Guayaquil':
      goog.require('bluemind.timezone.AmericaGuayaquil');
      ret = bluemind.timezone.AmericaGuayaquil;
      break;
    case 'America/Guyana':
      goog.require('bluemind.timezone.AmericaGuyana');
      ret = bluemind.timezone.AmericaGuyana;
      break;
    case 'America/Halifax':
      goog.require('bluemind.timezone.AmericaHalifax');
      ret = bluemind.timezone.AmericaHalifax;
      break;
    case 'America/Havana':
      goog.require('bluemind.timezone.AmericaHavana');
      ret = bluemind.timezone.AmericaHavana;
      break;
    case 'America/Hermosillo':
      goog.require('bluemind.timezone.AmericaHermosillo');
      ret = bluemind.timezone.AmericaHermosillo;
      break;
    case 'America/Indiana/Indianapolis':
      goog.require('bluemind.timezone.AmericaIndianaIndianapolis');
      ret = bluemind.timezone.AmericaIndianaIndianapolis;
      break;
    case 'America/Indiana/Knox':
      goog.require('bluemind.timezone.AmericaIndianaKnox');
      ret = bluemind.timezone.AmericaIndianaKnox;
      break;
    case 'America/Indiana/Marengo':
      goog.require('bluemind.timezone.AmericaIndianaMarengo');
      ret = bluemind.timezone.AmericaIndianaMarengo;
      break;
    case 'America/Indiana/Petersburg':
      goog.require('bluemind.timezone.AmericaIndianaPetersburg');
      ret = bluemind.timezone.AmericaIndianaPetersburg;
      break;
    case 'America/Indiana/Tell_City':
      goog.require('bluemind.timezone.AmericaIndianaTellCity');
      ret = bluemind.timezone.AmericaIndianaTellCity;
      break;
    case 'America/Indiana/Vevay':
      goog.require('bluemind.timezone.AmericaIndianaVevay');
      ret = bluemind.timezone.AmericaIndianaVevay;
      break;
    case 'America/Indiana/Vincennes':
      goog.require('bluemind.timezone.AmericaIndianaVincennes');
      ret = bluemind.timezone.AmericaIndianaVincennes;
      break;
    case 'America/Indiana/Winamac':
      goog.require('bluemind.timezone.AmericaIndianaWinamac');
      ret = bluemind.timezone.AmericaIndianaWinamac;
      break;
    case 'America/Inuvik':
      goog.require('bluemind.timezone.AmericaInuvik');
      ret = bluemind.timezone.AmericaInuvik;
      break;
    case 'America/Iqaluit':
      goog.require('bluemind.timezone.AmericaIqaluit');
      ret = bluemind.timezone.AmericaIqaluit;
      break;
    case 'America/Jamaica':
      goog.require('bluemind.timezone.AmericaJamaica');
      ret = bluemind.timezone.AmericaJamaica;
      break;
    case 'America/Juneau':
      goog.require('bluemind.timezone.AmericaJuneau');
      ret = bluemind.timezone.AmericaJuneau;
      break;
    case 'America/Kentucky/Louisville':
      goog.require('bluemind.timezone.AmericaKentuckyLouisville');
      ret = bluemind.timezone.AmericaKentuckyLouisville;
      break;
    case 'America/Kentucky/Monticello':
      goog.require('bluemind.timezone.AmericaKentuckyMonticello');
      ret = bluemind.timezone.AmericaKentuckyMonticello;
      break;
    case 'America/Kralendijk':
      goog.require('bluemind.timezone.AmericaKralendijk');
      ret = bluemind.timezone.AmericaKralendijk;
      break;
    case 'America/La_Paz':
      goog.require('bluemind.timezone.AmericaLaPaz');
      ret = bluemind.timezone.AmericaLaPaz;
      break;
    case 'America/Lima':
      goog.require('bluemind.timezone.AmericaLima');
      ret = bluemind.timezone.AmericaLima;
      break;
    case 'America/Los_Angeles':
      goog.require('bluemind.timezone.AmericaLosAngeles');
      ret = bluemind.timezone.AmericaLosAngeles;
      break;
    case 'America/Lower_Princes':
      goog.require('bluemind.timezone.AmericaLowerPrinces');
      ret = bluemind.timezone.AmericaLowerPrinces;
      break;
    case 'America/Maceio':
      goog.require('bluemind.timezone.AmericaMaceio');
      ret = bluemind.timezone.AmericaMaceio;
      break;
    case 'America/Managua':
      goog.require('bluemind.timezone.AmericaManagua');
      ret = bluemind.timezone.AmericaManagua;
      break;
    case 'America/Manaus':
      goog.require('bluemind.timezone.AmericaManaus');
      ret = bluemind.timezone.AmericaManaus;
      break;
    case 'America/Marigot':
      goog.require('bluemind.timezone.AmericaMarigot');
      ret = bluemind.timezone.AmericaMarigot;
      break;
    case 'America/Martinique':
      goog.require('bluemind.timezone.AmericaMartinique');
      ret = bluemind.timezone.AmericaMartinique;
      break;
    case 'America/Matamoros':
      goog.require('bluemind.timezone.AmericaMatamoros');
      ret = bluemind.timezone.AmericaMatamoros;
      break;
    case 'America/Mazatlan':
      goog.require('bluemind.timezone.AmericaMazatlan');
      ret = bluemind.timezone.AmericaMazatlan;
      break;
    case 'America/Menominee':
      goog.require('bluemind.timezone.AmericaMenominee');
      ret = bluemind.timezone.AmericaMenominee;
      break;
    case 'America/Merida':
      goog.require('bluemind.timezone.AmericaMerida');
      ret = bluemind.timezone.AmericaMerida;
      break;
    case 'America/Metlakatla':
      goog.require('bluemind.timezone.AmericaMetlakatla');
      ret = bluemind.timezone.AmericaMetlakatla;
      break;
    case 'America/Mexico_City':
      goog.require('bluemind.timezone.AmericaMexicoCity');
      ret = bluemind.timezone.AmericaMexicoCity;
      break;
    case 'America/Miquelon':
      goog.require('bluemind.timezone.AmericaMiquelon');
      ret = bluemind.timezone.AmericaMiquelon;
      break;
    case 'America/Moncton':
      goog.require('bluemind.timezone.AmericaMoncton');
      ret = bluemind.timezone.AmericaMoncton;
      break;
    case 'America/Monterrey':
      goog.require('bluemind.timezone.AmericaMonterrey');
      ret = bluemind.timezone.AmericaMonterrey;
      break;
    case 'America/Montevideo':
      goog.require('bluemind.timezone.AmericaMontevideo');
      ret = bluemind.timezone.AmericaMontevideo;
      break;
    case 'America/Montreal':
      goog.require('bluemind.timezone.AmericaMontreal');
      ret = bluemind.timezone.AmericaMontreal;
      break;
    case 'America/Montserrat':
      goog.require('bluemind.timezone.AmericaMontserrat');
      ret = bluemind.timezone.AmericaMontserrat;
      break;
    case 'America/Nassau':
      goog.require('bluemind.timezone.AmericaNassau');
      ret = bluemind.timezone.AmericaNassau;
      break;
    case 'America/New_York':
      goog.require('bluemind.timezone.AmericaNewYork');
      ret = bluemind.timezone.AmericaNewYork;
      break;
    case 'America/Nipigon':
      goog.require('bluemind.timezone.AmericaNipigon');
      ret = bluemind.timezone.AmericaNipigon;
      break;
    case 'America/Nome':
      goog.require('bluemind.timezone.AmericaNome');
      ret = bluemind.timezone.AmericaNome;
      break;
    case 'America/Noronha':
      goog.require('bluemind.timezone.AmericaNoronha');
      ret = bluemind.timezone.AmericaNoronha;
      break;
    case 'America/North_Dakota/Beulah':
      goog.require('bluemind.timezone.AmericaNorthDakotaBeulah');
      ret = bluemind.timezone.AmericaNorthDakotaBeulah;
      break;
    case 'America/North_Dakota/Center':
      goog.require('bluemind.timezone.AmericaNorthDakotaCenter');
      ret = bluemind.timezone.AmericaNorthDakotaCenter;
      break;
    case 'America/North_Dakota/New_Salem':
      goog.require('bluemind.timezone.AmericaNorthDakotaNewSalem');
      ret = bluemind.timezone.AmericaNorthDakotaNewSalem;
      break;
    case 'America/Ojinaga':
      goog.require('bluemind.timezone.AmericaOjinaga');
      ret = bluemind.timezone.AmericaOjinaga;
      break;
    case 'America/Panama':
      goog.require('bluemind.timezone.AmericaPanama');
      ret = bluemind.timezone.AmericaPanama;
      break;
    case 'America/Pangnirtung':
      goog.require('bluemind.timezone.AmericaPangnirtung');
      ret = bluemind.timezone.AmericaPangnirtung;
      break;
    case 'America/Paramaribo':
      goog.require('bluemind.timezone.AmericaParamaribo');
      ret = bluemind.timezone.AmericaParamaribo;
      break;
    case 'America/Phoenix':
      goog.require('bluemind.timezone.AmericaPhoenix');
      ret = bluemind.timezone.AmericaPhoenix;
      break;
    case 'America/Port-au-Prince':
      goog.require('bluemind.timezone.AmericaPortauPrince');
      ret = bluemind.timezone.AmericaPortauPrince;
      break;
    case 'America/Port_of_Spain':
      goog.require('bluemind.timezone.AmericaPortofSpain');
      ret = bluemind.timezone.AmericaPortofSpain;
      break;
    case 'America/Porto_Velho':
      goog.require('bluemind.timezone.AmericaPortoVelho');
      ret = bluemind.timezone.AmericaPortoVelho;
      break;
    case 'America/Puerto_Rico':
      goog.require('bluemind.timezone.AmericaPuertoRico');
      ret = bluemind.timezone.AmericaPuertoRico;
      break;
    case 'America/Rainy_River':
      goog.require('bluemind.timezone.AmericaRainyRiver');
      ret = bluemind.timezone.AmericaRainyRiver;
      break;
    case 'America/Rankin_Inlet':
      goog.require('bluemind.timezone.AmericaRankinInlet');
      ret = bluemind.timezone.AmericaRankinInlet;
      break;
    case 'America/Recife':
      goog.require('bluemind.timezone.AmericaRecife');
      ret = bluemind.timezone.AmericaRecife;
      break;
    case 'America/Regina':
      goog.require('bluemind.timezone.AmericaRegina');
      ret = bluemind.timezone.AmericaRegina;
      break;
    case 'America/Resolute':
      goog.require('bluemind.timezone.AmericaResolute');
      ret = bluemind.timezone.AmericaResolute;
      break;
    case 'America/Rio_Branco':
      goog.require('bluemind.timezone.AmericaRioBranco');
      ret = bluemind.timezone.AmericaRioBranco;
      break;
    case 'America/Santa_Isabel':
      goog.require('bluemind.timezone.AmericaSantaIsabel');
      ret = bluemind.timezone.AmericaSantaIsabel;
      break;
    case 'America/Santarem':
      goog.require('bluemind.timezone.AmericaSantarem');
      ret = bluemind.timezone.AmericaSantarem;
      break;
    case 'America/Santiago':
      goog.require('bluemind.timezone.AmericaSantiago');
      ret = bluemind.timezone.AmericaSantiago;
      break;
    case 'America/Santo_Domingo':
      goog.require('bluemind.timezone.AmericaSantoDomingo');
      ret = bluemind.timezone.AmericaSantoDomingo;
      break;
    case 'America/Sao_Paulo':
      goog.require('bluemind.timezone.AmericaSaoPaulo');
      ret = bluemind.timezone.AmericaSaoPaulo;
      break;
    case 'America/Scoresbysund':
      goog.require('bluemind.timezone.AmericaScoresbysund');
      ret = bluemind.timezone.AmericaScoresbysund;
      break;
    case 'America/Shiprock':
      goog.require('bluemind.timezone.AmericaShiprock');
      ret = bluemind.timezone.AmericaShiprock;
      break;
    case 'America/Sitka':
      goog.require('bluemind.timezone.AmericaSitka');
      ret = bluemind.timezone.AmericaSitka;
      break;
    case 'America/St_Barthelemy':
      goog.require('bluemind.timezone.AmericaStBarthelemy');
      ret = bluemind.timezone.AmericaStBarthelemy;
      break;
    case 'America/St_Johns':
      goog.require('bluemind.timezone.AmericaStJohns');
      ret = bluemind.timezone.AmericaStJohns;
      break;
    case 'America/St_Kitts':
      goog.require('bluemind.timezone.AmericaStKitts');
      ret = bluemind.timezone.AmericaStKitts;
      break;
    case 'America/St_Lucia':
      goog.require('bluemind.timezone.AmericaStLucia');
      ret = bluemind.timezone.AmericaStLucia;
      break;
    case 'America/St_Thomas':
      goog.require('bluemind.timezone.AmericaStThomas');
      ret = bluemind.timezone.AmericaStThomas;
      break;
    case 'America/St_Vincent':
      goog.require('bluemind.timezone.AmericaStVincent');
      ret = bluemind.timezone.AmericaStVincent;
      break;
    case 'America/Swift_Current':
      goog.require('bluemind.timezone.AmericaSwiftCurrent');
      ret = bluemind.timezone.AmericaSwiftCurrent;
      break;
    case 'America/Tegucigalpa':
      goog.require('bluemind.timezone.AmericaTegucigalpa');
      ret = bluemind.timezone.AmericaTegucigalpa;
      break;
    case 'America/Thule':
      goog.require('bluemind.timezone.AmericaThule');
      ret = bluemind.timezone.AmericaThule;
      break;
    case 'America/Thunder_Bay':
      goog.require('bluemind.timezone.AmericaThunderBay');
      ret = bluemind.timezone.AmericaThunderBay;
      break;
    case 'America/Tijuana':
      goog.require('bluemind.timezone.AmericaTijuana');
      ret = bluemind.timezone.AmericaTijuana;
      break;
    case 'America/Toronto':
      goog.require('bluemind.timezone.AmericaToronto');
      ret = bluemind.timezone.AmericaToronto;
      break;
    case 'America/Tortola':
      goog.require('bluemind.timezone.AmericaTortola');
      ret = bluemind.timezone.AmericaTortola;
      break;
    case 'America/Vancouver':
      goog.require('bluemind.timezone.AmericaVancouver');
      ret = bluemind.timezone.AmericaVancouver;
      break;
    case 'America/Whitehorse':
      goog.require('bluemind.timezone.AmericaWhitehorse');
      ret = bluemind.timezone.AmericaWhitehorse;
      break;
    case 'America/Winnipeg':
      goog.require('bluemind.timezone.AmericaWinnipeg');
      ret = bluemind.timezone.AmericaWinnipeg;
      break;
    case 'America/Yakutat':
      goog.require('bluemind.timezone.AmericaYakutat');
      ret = bluemind.timezone.AmericaYakutat;
      break;
    case 'America/Yellowknife':
      goog.require('bluemind.timezone.AmericaYellowknife');
      ret = bluemind.timezone.AmericaYellowknife;
      break;
    case 'Antarctica/Casey':
      goog.require('bluemind.timezone.AntarcticaCasey');
      ret = bluemind.timezone.AntarcticaCasey;
      break;
    case 'Antarctica/Davis':
      goog.require('bluemind.timezone.AntarcticaDavis');
      ret = bluemind.timezone.AntarcticaDavis;
      break;
    case 'Antarctica/DumontDUrville':
      goog.require('bluemind.timezone.AntarcticaDumontDUrville');
      ret = bluemind.timezone.AntarcticaDumontDUrville;
      break;
    case 'Antarctica/Macquarie':
      goog.require('bluemind.timezone.AntarcticaMacquarie');
      ret = bluemind.timezone.AntarcticaMacquarie;
      break;
    case 'Antarctica/Mawson':
      goog.require('bluemind.timezone.AntarcticaMawson');
      ret = bluemind.timezone.AntarcticaMawson;
      break;
    case 'Antarctica/McMurdo':
      goog.require('bluemind.timezone.AntarcticaMcMurdo');
      ret = bluemind.timezone.AntarcticaMcMurdo;
      break;
    case 'Antarctica/Palmer':
      goog.require('bluemind.timezone.AntarcticaPalmer');
      ret = bluemind.timezone.AntarcticaPalmer;
      break;
    case 'Antarctica/Rothera':
      goog.require('bluemind.timezone.AntarcticaRothera');
      ret = bluemind.timezone.AntarcticaRothera;
      break;
    case 'Antarctica/South_Pole':
      goog.require('bluemind.timezone.AntarcticaSouthPole');
      ret = bluemind.timezone.AntarcticaSouthPole;
      break;
    case 'Antarctica/Syowa':
      goog.require('bluemind.timezone.AntarcticaSyowa');
      ret = bluemind.timezone.AntarcticaSyowa;
      break;
    case 'Antarctica/Vostok':
      goog.require('bluemind.timezone.AntarcticaVostok');
      ret = bluemind.timezone.AntarcticaVostok;
      break;
    case 'Arctic/Longyearbyen':
      goog.require('bluemind.timezone.ArcticLongyearbyen');
      ret = bluemind.timezone.ArcticLongyearbyen;
      break;
    case 'Asia/Aden':
      goog.require('bluemind.timezone.AsiaAden');
      ret = bluemind.timezone.AsiaAden;
      break;
    case 'Asia/Almaty':
      goog.require('bluemind.timezone.AsiaAlmaty');
      ret = bluemind.timezone.AsiaAlmaty;
      break;
    case 'Asia/Amman':
      goog.require('bluemind.timezone.AsiaAmman');
      ret = bluemind.timezone.AsiaAmman;
      break;
    case 'Asia/Anadyr':
      goog.require('bluemind.timezone.AsiaAnadyr');
      ret = bluemind.timezone.AsiaAnadyr;
      break;
    case 'Asia/Aqtau':
      goog.require('bluemind.timezone.AsiaAqtau');
      ret = bluemind.timezone.AsiaAqtau;
      break;
    case 'Asia/Aqtobe':
      goog.require('bluemind.timezone.AsiaAqtobe');
      ret = bluemind.timezone.AsiaAqtobe;
      break;
    case 'Asia/Ashgabat':
      goog.require('bluemind.timezone.AsiaAshgabat');
      ret = bluemind.timezone.AsiaAshgabat;
      break;
    case 'Asia/Baghdad':
      goog.require('bluemind.timezone.AsiaBaghdad');
      ret = bluemind.timezone.AsiaBaghdad;
      break;
    case 'Asia/Bahrain':
      goog.require('bluemind.timezone.AsiaBahrain');
      ret = bluemind.timezone.AsiaBahrain;
      break;
    case 'Asia/Baku':
      goog.require('bluemind.timezone.AsiaBaku');
      ret = bluemind.timezone.AsiaBaku;
      break;
    case 'Asia/Bangkok':
      goog.require('bluemind.timezone.AsiaBangkok');
      ret = bluemind.timezone.AsiaBangkok;
      break;
    case 'Asia/Beirut':
      goog.require('bluemind.timezone.AsiaBeirut');
      ret = bluemind.timezone.AsiaBeirut;
      break;
    case 'Asia/Bishkek':
      goog.require('bluemind.timezone.AsiaBishkek');
      ret = bluemind.timezone.AsiaBishkek;
      break;
    case 'Asia/Brunei':
      goog.require('bluemind.timezone.AsiaBrunei');
      ret = bluemind.timezone.AsiaBrunei;
      break;
    case 'Asia/Choibalsan':
      goog.require('bluemind.timezone.AsiaChoibalsan');
      ret = bluemind.timezone.AsiaChoibalsan;
      break;
    case 'Asia/Chongqing':
      goog.require('bluemind.timezone.AsiaChongqing');
      ret = bluemind.timezone.AsiaChongqing;
      break;
    case 'Asia/Colombo':
      goog.require('bluemind.timezone.AsiaColombo');
      ret = bluemind.timezone.AsiaColombo;
      break;
    case 'Asia/Damascus':
      goog.require('bluemind.timezone.AsiaDamascus');
      ret = bluemind.timezone.AsiaDamascus;
      break;
    case 'Asia/Dhaka':
      goog.require('bluemind.timezone.AsiaDhaka');
      ret = bluemind.timezone.AsiaDhaka;
      break;
    case 'Asia/Dili':
      goog.require('bluemind.timezone.AsiaDili');
      ret = bluemind.timezone.AsiaDili;
      break;
    case 'Asia/Dubai':
      goog.require('bluemind.timezone.AsiaDubai');
      ret = bluemind.timezone.AsiaDubai;
      break;
    case 'Asia/Dushanbe':
      goog.require('bluemind.timezone.AsiaDushanbe');
      ret = bluemind.timezone.AsiaDushanbe;
      break;
    case 'Asia/Gaza':
      goog.require('bluemind.timezone.AsiaGaza');
      ret = bluemind.timezone.AsiaGaza;
      break;
    case 'Asia/Harbin':
      goog.require('bluemind.timezone.AsiaHarbin');
      ret = bluemind.timezone.AsiaHarbin;
      break;
    case 'Asia/Hebron':
      goog.require('bluemind.timezone.AsiaHebron');
      ret = bluemind.timezone.AsiaHebron;
      break;
    case 'Asia/Ho_Chi_Minh':
      goog.require('bluemind.timezone.AsiaHoChiMinh');
      ret = bluemind.timezone.AsiaHoChiMinh;
      break;
    case 'Asia/Hong_Kong':
      goog.require('bluemind.timezone.AsiaHongKong');
      ret = bluemind.timezone.AsiaHongKong;
      break;
    case 'Asia/Hovd':
      goog.require('bluemind.timezone.AsiaHovd');
      ret = bluemind.timezone.AsiaHovd;
      break;
    case 'Asia/Irkutsk':
      goog.require('bluemind.timezone.AsiaIrkutsk');
      ret = bluemind.timezone.AsiaIrkutsk;
      break;
    case 'Asia/Jakarta':
      goog.require('bluemind.timezone.AsiaJakarta');
      ret = bluemind.timezone.AsiaJakarta;
      break;
    case 'Asia/Jayapura':
      goog.require('bluemind.timezone.AsiaJayapura');
      ret = bluemind.timezone.AsiaJayapura;
      break;
    case 'Asia/Jerusalem':
      goog.require('bluemind.timezone.AsiaJerusalem');
      ret = bluemind.timezone.AsiaJerusalem;
      break;
    case 'Asia/Kabul':
      goog.require('bluemind.timezone.AsiaKabul');
      ret = bluemind.timezone.AsiaKabul;
      break;
    case 'Asia/Kamchatka':
      goog.require('bluemind.timezone.AsiaKamchatka');
      ret = bluemind.timezone.AsiaKamchatka;
      break;
    case 'Asia/Karachi':
      goog.require('bluemind.timezone.AsiaKarachi');
      ret = bluemind.timezone.AsiaKarachi;
      break;
    case 'Asia/Kashgar':
      goog.require('bluemind.timezone.AsiaKashgar');
      ret = bluemind.timezone.AsiaKashgar;
      break;
    case 'Asia/Kathmandu':
      goog.require('bluemind.timezone.AsiaKathmandu');
      ret = bluemind.timezone.AsiaKathmandu;
      break;
    case 'Asia/Kolkata':
      goog.require('bluemind.timezone.AsiaKolkata');
      ret = bluemind.timezone.AsiaKolkata;
      break;
    case 'Asia/Krasnoyarsk':
      goog.require('bluemind.timezone.AsiaKrasnoyarsk');
      ret = bluemind.timezone.AsiaKrasnoyarsk;
      break;
    case 'Asia/Kuala_Lumpur':
      goog.require('bluemind.timezone.AsiaKualaLumpur');
      ret = bluemind.timezone.AsiaKualaLumpur;
      break;
    case 'Asia/Kuching':
      goog.require('bluemind.timezone.AsiaKuching');
      ret = bluemind.timezone.AsiaKuching;
      break;
    case 'Asia/Kuwait':
      goog.require('bluemind.timezone.AsiaKuwait');
      ret = bluemind.timezone.AsiaKuwait;
      break;
    case 'Asia/Macau':
      goog.require('bluemind.timezone.AsiaMacau');
      ret = bluemind.timezone.AsiaMacau;
      break;
    case 'Asia/Magadan':
      goog.require('bluemind.timezone.AsiaMagadan');
      ret = bluemind.timezone.AsiaMagadan;
      break;
    case 'Asia/Makassar':
      goog.require('bluemind.timezone.AsiaMakassar');
      ret = bluemind.timezone.AsiaMakassar;
      break;
    case 'Asia/Manila':
      goog.require('bluemind.timezone.AsiaManila');
      ret = bluemind.timezone.AsiaManila;
      break;
    case 'Asia/Muscat':
      goog.require('bluemind.timezone.AsiaMuscat');
      ret = bluemind.timezone.AsiaMuscat;
      break;
    case 'Asia/Nicosia':
      goog.require('bluemind.timezone.AsiaNicosia');
      ret = bluemind.timezone.AsiaNicosia;
      break;
    case 'Asia/Novokuznetsk':
      goog.require('bluemind.timezone.AsiaNovokuznetsk');
      ret = bluemind.timezone.AsiaNovokuznetsk;
      break;
    case 'Asia/Novosibirsk':
      goog.require('bluemind.timezone.AsiaNovosibirsk');
      ret = bluemind.timezone.AsiaNovosibirsk;
      break;
    case 'Asia/Omsk':
      goog.require('bluemind.timezone.AsiaOmsk');
      ret = bluemind.timezone.AsiaOmsk;
      break;
    case 'Asia/Oral':
      goog.require('bluemind.timezone.AsiaOral');
      ret = bluemind.timezone.AsiaOral;
      break;
    case 'Asia/Phnom_Penh':
      goog.require('bluemind.timezone.AsiaPhnomPenh');
      ret = bluemind.timezone.AsiaPhnomPenh;
      break;
    case 'Asia/Pontianak':
      goog.require('bluemind.timezone.AsiaPontianak');
      ret = bluemind.timezone.AsiaPontianak;
      break;
    case 'Asia/Pyongyang':
      goog.require('bluemind.timezone.AsiaPyongyang');
      ret = bluemind.timezone.AsiaPyongyang;
      break;
    case 'Asia/Qatar':
      goog.require('bluemind.timezone.AsiaQatar');
      ret = bluemind.timezone.AsiaQatar;
      break;
    case 'Asia/Qyzylorda':
      goog.require('bluemind.timezone.AsiaQyzylorda');
      ret = bluemind.timezone.AsiaQyzylorda;
      break;
    case 'Asia/Rangoon':
      goog.require('bluemind.timezone.AsiaRangoon');
      ret = bluemind.timezone.AsiaRangoon;
      break;
    case 'Asia/Riyadh':
      goog.require('bluemind.timezone.AsiaRiyadh');
      ret = bluemind.timezone.AsiaRiyadh;
      break;
    case 'Asia/Sakhalin':
      goog.require('bluemind.timezone.AsiaSakhalin');
      ret = bluemind.timezone.AsiaSakhalin;
      break;
    case 'Asia/Samarkand':
      goog.require('bluemind.timezone.AsiaSamarkand');
      ret = bluemind.timezone.AsiaSamarkand;
      break;
    case 'Asia/Seoul':
      goog.require('bluemind.timezone.AsiaSeoul');
      ret = bluemind.timezone.AsiaSeoul;
      break;
    case 'Asia/Shanghai':
      goog.require('bluemind.timezone.AsiaShanghai');
      ret = bluemind.timezone.AsiaShanghai;
      break;
    case 'Asia/Singapore':
      goog.require('bluemind.timezone.AsiaSingapore');
      ret = bluemind.timezone.AsiaSingapore;
      break;
    case 'Asia/Taipei':
      goog.require('bluemind.timezone.AsiaTaipei');
      ret = bluemind.timezone.AsiaTaipei;
      break;
    case 'Asia/Tashkent':
      goog.require('bluemind.timezone.AsiaTashkent');
      ret = bluemind.timezone.AsiaTashkent;
      break;
    case 'Asia/Tbilisi':
      goog.require('bluemind.timezone.AsiaTbilisi');
      ret = bluemind.timezone.AsiaTbilisi;
      break;
    case 'Asia/Tehran':
      goog.require('bluemind.timezone.AsiaTehran');
      ret = bluemind.timezone.AsiaTehran;
      break;
    case 'Asia/Thimphu':
      goog.require('bluemind.timezone.AsiaThimphu');
      ret = bluemind.timezone.AsiaThimphu;
      break;
    case 'Asia/Tokyo':
      goog.require('bluemind.timezone.AsiaTokyo');
      ret = bluemind.timezone.AsiaTokyo;
      break;
    case 'Asia/Ulaanbaatar':
      goog.require('bluemind.timezone.AsiaUlaanbaatar');
      ret = bluemind.timezone.AsiaUlaanbaatar;
      break;
    case 'Asia/Urumqi':
      goog.require('bluemind.timezone.AsiaUrumqi');
      ret = bluemind.timezone.AsiaUrumqi;
      break;
    case 'Asia/Vientiane':
      goog.require('bluemind.timezone.AsiaVientiane');
      ret = bluemind.timezone.AsiaVientiane;
      break;
    case 'Asia/Vladivostok':
      goog.require('bluemind.timezone.AsiaVladivostok');
      ret = bluemind.timezone.AsiaVladivostok;
      break;
    case 'Asia/Yakutsk':
      goog.require('bluemind.timezone.AsiaYakutsk');
      ret = bluemind.timezone.AsiaYakutsk;
      break;
    case 'Asia/Yekaterinburg':
      goog.require('bluemind.timezone.AsiaYekaterinburg');
      ret = bluemind.timezone.AsiaYekaterinburg;
      break;
    case 'Asia/Yerevan':
      goog.require('bluemind.timezone.AsiaYerevan');
      ret = bluemind.timezone.AsiaYerevan;
      break;
    case 'Atlantic/Azores':
      goog.require('bluemind.timezone.AtlanticAzores');
      ret = bluemind.timezone.AtlanticAzores;
      break;
    case 'Atlantic/Bermuda':
      goog.require('bluemind.timezone.AtlanticBermuda');
      ret = bluemind.timezone.AtlanticBermuda;
      break;
    case 'Atlantic/Canary':
      goog.require('bluemind.timezone.AtlanticCanary');
      ret = bluemind.timezone.AtlanticCanary;
      break;
    case 'Atlantic/Cape_Verde':
      goog.require('bluemind.timezone.AtlanticCapeVerde');
      ret = bluemind.timezone.AtlanticCapeVerde;
      break;
    case 'Atlantic/Faroe':
      goog.require('bluemind.timezone.AtlanticFaroe');
      ret = bluemind.timezone.AtlanticFaroe;
      break;
    case 'Atlantic/Madeira':
      goog.require('bluemind.timezone.AtlanticMadeira');
      ret = bluemind.timezone.AtlanticMadeira;
      break;
    case 'Atlantic/Reykjavik':
      goog.require('bluemind.timezone.AtlanticReykjavik');
      ret = bluemind.timezone.AtlanticReykjavik;
      break;
    case 'Atlantic/South_Georgia':
      goog.require('bluemind.timezone.AtlanticSouthGeorgia');
      ret = bluemind.timezone.AtlanticSouthGeorgia;
      break;
    case 'Atlantic/St_Helena':
      goog.require('bluemind.timezone.AtlanticStHelena');
      ret = bluemind.timezone.AtlanticStHelena;
      break;
    case 'Atlantic/Stanley':
      goog.require('bluemind.timezone.AtlanticStanley');
      ret = bluemind.timezone.AtlanticStanley;
      break;
    case 'Australia/Adelaide':
      goog.require('bluemind.timezone.AustraliaAdelaide');
      ret = bluemind.timezone.AustraliaAdelaide;
      break;
    case 'Australia/Brisbane':
      goog.require('bluemind.timezone.AustraliaBrisbane');
      ret = bluemind.timezone.AustraliaBrisbane;
      break;
    case 'Australia/Broken_Hill':
      goog.require('bluemind.timezone.AustraliaBrokenHill');
      ret = bluemind.timezone.AustraliaBrokenHill;
      break;
    case 'Australia/Currie':
      goog.require('bluemind.timezone.AustraliaCurrie');
      ret = bluemind.timezone.AustraliaCurrie;
      break;
    case 'Australia/Darwin':
      goog.require('bluemind.timezone.AustraliaDarwin');
      ret = bluemind.timezone.AustraliaDarwin;
      break;
    case 'Australia/Eucla':
      goog.require('bluemind.timezone.AustraliaEucla');
      ret = bluemind.timezone.AustraliaEucla;
      break;
    case 'Australia/Hobart':
      goog.require('bluemind.timezone.AustraliaHobart');
      ret = bluemind.timezone.AustraliaHobart;
      break;
    case 'Australia/Lindeman':
      goog.require('bluemind.timezone.AustraliaLindeman');
      ret = bluemind.timezone.AustraliaLindeman;
      break;
    case 'Australia/Lord_Howe':
      goog.require('bluemind.timezone.AustraliaLordHowe');
      ret = bluemind.timezone.AustraliaLordHowe;
      break;
    case 'Australia/Melbourne':
      goog.require('bluemind.timezone.AustraliaMelbourne');
      ret = bluemind.timezone.AustraliaMelbourne;
      break;
    case 'Australia/Perth':
      goog.require('bluemind.timezone.AustraliaPerth');
      ret = bluemind.timezone.AustraliaPerth;
      break;
    case 'Australia/Sydney':
      goog.require('bluemind.timezone.AustraliaSydney');
      ret = bluemind.timezone.AustraliaSydney;
      break;
    case 'Europe/Amsterdam':
      goog.require('bluemind.timezone.EuropeAmsterdam');
      ret = bluemind.timezone.EuropeAmsterdam;
      break;
    case 'Europe/Andorra':
      goog.require('bluemind.timezone.EuropeAndorra');
      ret = bluemind.timezone.EuropeAndorra;
      break;
    case 'Europe/Athens':
      goog.require('bluemind.timezone.EuropeAthens');
      ret = bluemind.timezone.EuropeAthens;
      break;
    case 'Europe/Belgrade':
      goog.require('bluemind.timezone.EuropeBelgrade');
      ret = bluemind.timezone.EuropeBelgrade;
      break;
    case 'Europe/Berlin':
      goog.require('bluemind.timezone.EuropeBerlin');
      ret = bluemind.timezone.EuropeBerlin;
      break;
    case 'Europe/Bratislava':
      goog.require('bluemind.timezone.EuropeBratislava');
      ret = bluemind.timezone.EuropeBratislava;
      break;
    case 'Europe/Brussels':
      goog.require('bluemind.timezone.EuropeBrussels');
      ret = bluemind.timezone.EuropeBrussels;
      break;
    case 'Europe/Bucharest':
      goog.require('bluemind.timezone.EuropeBucharest');
      ret = bluemind.timezone.EuropeBucharest;
      break;
    case 'Europe/Budapest':
      goog.require('bluemind.timezone.EuropeBudapest');
      ret = bluemind.timezone.EuropeBudapest;
      break;
    case 'Europe/Chisinau':
      goog.require('bluemind.timezone.EuropeChisinau');
      ret = bluemind.timezone.EuropeChisinau;
      break;
    case 'Europe/Copenhagen':
      goog.require('bluemind.timezone.EuropeCopenhagen');
      ret = bluemind.timezone.EuropeCopenhagen;
      break;
    case 'Europe/Dublin':
      goog.require('bluemind.timezone.EuropeDublin');
      ret = bluemind.timezone.EuropeDublin;
      break;
    case 'Europe/Gibraltar':
      goog.require('bluemind.timezone.EuropeGibraltar');
      ret = bluemind.timezone.EuropeGibraltar;
      break;
    case 'Europe/Guernsey':
      goog.require('bluemind.timezone.EuropeGuernsey');
      ret = bluemind.timezone.EuropeGuernsey;
      break;
    case 'Europe/Helsinki':
      goog.require('bluemind.timezone.EuropeHelsinki');
      ret = bluemind.timezone.EuropeHelsinki;
      break;
    case 'Europe/Isle_of_Man':
      goog.require('bluemind.timezone.EuropeIsleofMan');
      ret = bluemind.timezone.EuropeIsleofMan;
      break;
    case 'Europe/Istanbul':
      goog.require('bluemind.timezone.EuropeIstanbul');
      ret = bluemind.timezone.EuropeIstanbul;
      break;
    case 'Europe/Jersey':
      goog.require('bluemind.timezone.EuropeJersey');
      ret = bluemind.timezone.EuropeJersey;
      break;
    case 'Europe/Kaliningrad':
      goog.require('bluemind.timezone.EuropeKaliningrad');
      ret = bluemind.timezone.EuropeKaliningrad;
      break;
    case 'Europe/Kiev':
      goog.require('bluemind.timezone.EuropeKiev');
      ret = bluemind.timezone.EuropeKiev;
      break;
    case 'Europe/Lisbon':
      goog.require('bluemind.timezone.EuropeLisbon');
      ret = bluemind.timezone.EuropeLisbon;
      break;
    case 'Europe/Ljubljana':
      goog.require('bluemind.timezone.EuropeLjubljana');
      ret = bluemind.timezone.EuropeLjubljana;
      break;
    case 'Europe/London':
      goog.require('bluemind.timezone.EuropeLondon');
      ret = bluemind.timezone.EuropeLondon;
      break;
    case 'Europe/Luxembourg':
      goog.require('bluemind.timezone.EuropeLuxembourg');
      ret = bluemind.timezone.EuropeLuxembourg;
      break;
    case 'Europe/Madrid':
      goog.require('bluemind.timezone.EuropeMadrid');
      ret = bluemind.timezone.EuropeMadrid;
      break;
    case 'Europe/Malta':
      goog.require('bluemind.timezone.EuropeMalta');
      ret = bluemind.timezone.EuropeMalta;
      break;
    case 'Europe/Mariehamn':
      goog.require('bluemind.timezone.EuropeMariehamn');
      ret = bluemind.timezone.EuropeMariehamn;
      break;
    case 'Europe/Minsk':
      goog.require('bluemind.timezone.EuropeMinsk');
      ret = bluemind.timezone.EuropeMinsk;
      break;
    case 'Europe/Monaco':
      goog.require('bluemind.timezone.EuropeMonaco');
      ret = bluemind.timezone.EuropeMonaco;
      break;
    case 'Europe/Moscow':
      goog.require('bluemind.timezone.EuropeMoscow');
      ret = bluemind.timezone.EuropeMoscow;
      break;
    case 'Europe/Oslo':
      goog.require('bluemind.timezone.EuropeOslo');
      ret = bluemind.timezone.EuropeOslo;
      break;
    case 'Europe/Paris':
      goog.require('bluemind.timezone.EuropeParis');
      ret = bluemind.timezone.EuropeParis;
      break;
    case 'Europe/Podgorica':
      goog.require('bluemind.timezone.EuropePodgorica');
      ret = bluemind.timezone.EuropePodgorica;
      break;
    case 'Europe/Prague':
      goog.require('bluemind.timezone.EuropePrague');
      ret = bluemind.timezone.EuropePrague;
      break;
    case 'Europe/Riga':
      goog.require('bluemind.timezone.EuropeRiga');
      ret = bluemind.timezone.EuropeRiga;
      break;
    case 'Europe/Rome':
      goog.require('bluemind.timezone.EuropeRome');
      ret = bluemind.timezone.EuropeRome;
      break;
    case 'Europe/Samara':
      goog.require('bluemind.timezone.EuropeSamara');
      ret = bluemind.timezone.EuropeSamara;
      break;
    case 'Europe/San_Marino':
      goog.require('bluemind.timezone.EuropeSanMarino');
      ret = bluemind.timezone.EuropeSanMarino;
      break;
    case 'Europe/Sarajevo':
      goog.require('bluemind.timezone.EuropeSarajevo');
      ret = bluemind.timezone.EuropeSarajevo;
      break;
    case 'Europe/Simferopol':
      goog.require('bluemind.timezone.EuropeSimferopol');
      ret = bluemind.timezone.EuropeSimferopol;
      break;
    case 'Europe/Skopje':
      goog.require('bluemind.timezone.EuropeSkopje');
      ret = bluemind.timezone.EuropeSkopje;
      break;
    case 'Europe/Sofia':
      goog.require('bluemind.timezone.EuropeSofia');
      ret = bluemind.timezone.EuropeSofia;
      break;
    case 'Europe/Stockholm':
      goog.require('bluemind.timezone.EuropeStockholm');
      ret = bluemind.timezone.EuropeStockholm;
      break;
    case 'Europe/Tallinn':
      goog.require('bluemind.timezone.EuropeTallinn');
      ret = bluemind.timezone.EuropeTallinn;
      break;
    case 'Europe/Tirane':
      goog.require('bluemind.timezone.EuropeTirane');
      ret = bluemind.timezone.EuropeTirane;
      break;
    case 'Europe/Uzhgorod':
      goog.require('bluemind.timezone.EuropeUzhgorod');
      ret = bluemind.timezone.EuropeUzhgorod;
      break;
    case 'Europe/Vaduz':
      goog.require('bluemind.timezone.EuropeVaduz');
      ret = bluemind.timezone.EuropeVaduz;
      break;
    case 'Europe/Vatican':
      goog.require('bluemind.timezone.EuropeVatican');
      ret = bluemind.timezone.EuropeVatican;
      break;
    case 'Europe/Vienna':
      goog.require('bluemind.timezone.EuropeVienna');
      ret = bluemind.timezone.EuropeVienna;
      break;
    case 'Europe/Vilnius':
      goog.require('bluemind.timezone.EuropeVilnius');
      ret = bluemind.timezone.EuropeVilnius;
      break;
    case 'Europe/Volgograd':
      goog.require('bluemind.timezone.EuropeVolgograd');
      ret = bluemind.timezone.EuropeVolgograd;
      break;
    case 'Europe/Warsaw':
      goog.require('bluemind.timezone.EuropeWarsaw');
      ret = bluemind.timezone.EuropeWarsaw;
      break;
    case 'Europe/Zagreb':
      goog.require('bluemind.timezone.EuropeZagreb');
      ret = bluemind.timezone.EuropeZagreb;
      break;
    case 'Europe/Zaporozhye':
      goog.require('bluemind.timezone.EuropeZaporozhye');
      ret = bluemind.timezone.EuropeZaporozhye;
      break;
    case 'Europe/Zurich':
      goog.require('bluemind.timezone.EuropeZurich');
      ret = bluemind.timezone.EuropeZurich;
      break;
    case 'Indian/Antananarivo':
      goog.require('bluemind.timezone.IndianAntananarivo');
      ret = bluemind.timezone.IndianAntananarivo;
      break;
    case 'Indian/Chagos':
      goog.require('bluemind.timezone.IndianChagos');
      ret = bluemind.timezone.IndianChagos;
      break;
    case 'Indian/Christmas':
      goog.require('bluemind.timezone.IndianChristmas');
      ret = bluemind.timezone.IndianChristmas;
      break;
    case 'Indian/Cocos':
      goog.require('bluemind.timezone.IndianCocos');
      ret = bluemind.timezone.IndianCocos;
      break;
    case 'Indian/Comoro':
      goog.require('bluemind.timezone.IndianComoro');
      ret = bluemind.timezone.IndianComoro;
      break;
    case 'Indian/Kerguelen':
      goog.require('bluemind.timezone.IndianKerguelen');
      ret = bluemind.timezone.IndianKerguelen;
      break;
    case 'Indian/Mahe':
      goog.require('bluemind.timezone.IndianMahe');
      ret = bluemind.timezone.IndianMahe;
      break;
    case 'Indian/Maldives':
      goog.require('bluemind.timezone.IndianMaldives');
      ret = bluemind.timezone.IndianMaldives;
      break;
    case 'Indian/Mauritius':
      goog.require('bluemind.timezone.IndianMauritius');
      ret = bluemind.timezone.IndianMauritius;
      break;
    case 'Indian/Mayotte':
      goog.require('bluemind.timezone.IndianMayotte');
      ret = bluemind.timezone.IndianMayotte;
      break;
    case 'Indian/Reunion':
      goog.require('bluemind.timezone.IndianReunion');
      ret = bluemind.timezone.IndianReunion;
      break;
    case 'Pacific/Apia':
      goog.require('bluemind.timezone.PacificApia');
      ret = bluemind.timezone.PacificApia;
      break;
    case 'Pacific/Auckland':
      goog.require('bluemind.timezone.PacificAuckland');
      ret = bluemind.timezone.PacificAuckland;
      break;
    case 'Pacific/Chatham':
      goog.require('bluemind.timezone.PacificChatham');
      ret = bluemind.timezone.PacificChatham;
      break;
    case 'Pacific/Chuuk':
      goog.require('bluemind.timezone.PacificChuuk');
      ret = bluemind.timezone.PacificChuuk;
      break;
    case 'Pacific/Easter':
      goog.require('bluemind.timezone.PacificEaster');
      ret = bluemind.timezone.PacificEaster;
      break;
    case 'Pacific/Efate':
      goog.require('bluemind.timezone.PacificEfate');
      ret = bluemind.timezone.PacificEfate;
      break;
    case 'Pacific/Enderbury':
      goog.require('bluemind.timezone.PacificEnderbury');
      ret = bluemind.timezone.PacificEnderbury;
      break;
    case 'Pacific/Fakaofo':
      goog.require('bluemind.timezone.PacificFakaofo');
      ret = bluemind.timezone.PacificFakaofo;
      break;
    case 'Pacific/Fiji':
      goog.require('bluemind.timezone.PacificFiji');
      ret = bluemind.timezone.PacificFiji;
      break;
    case 'Pacific/Funafuti':
      goog.require('bluemind.timezone.PacificFunafuti');
      ret = bluemind.timezone.PacificFunafuti;
      break;
    case 'Pacific/Galapagos':
      goog.require('bluemind.timezone.PacificGalapagos');
      ret = bluemind.timezone.PacificGalapagos;
      break;
    case 'Pacific/Gambier':
      goog.require('bluemind.timezone.PacificGambier');
      ret = bluemind.timezone.PacificGambier;
      break;
    case 'Pacific/Guadalcanal':
      goog.require('bluemind.timezone.PacificGuadalcanal');
      ret = bluemind.timezone.PacificGuadalcanal;
      break;
    case 'Pacific/Guam':
      goog.require('bluemind.timezone.PacificGuam');
      ret = bluemind.timezone.PacificGuam;
      break;
    case 'Pacific/Honolulu':
      goog.require('bluemind.timezone.PacificHonolulu');
      ret = bluemind.timezone.PacificHonolulu;
      break;
    case 'Pacific/Johnston':
      goog.require('bluemind.timezone.PacificJohnston');
      ret = bluemind.timezone.PacificJohnston;
      break;
    case 'Pacific/Kiritimati':
      goog.require('bluemind.timezone.PacificKiritimati');
      ret = bluemind.timezone.PacificKiritimati;
      break;
    case 'Pacific/Kosrae':
      goog.require('bluemind.timezone.PacificKosrae');
      ret = bluemind.timezone.PacificKosrae;
      break;
    case 'Pacific/Kwajalein':
      goog.require('bluemind.timezone.PacificKwajalein');
      ret = bluemind.timezone.PacificKwajalein;
      break;
    case 'Pacific/Majuro':
      goog.require('bluemind.timezone.PacificMajuro');
      ret = bluemind.timezone.PacificMajuro;
      break;
    case 'Pacific/Marquesas':
      goog.require('bluemind.timezone.PacificMarquesas');
      ret = bluemind.timezone.PacificMarquesas;
      break;
    case 'Pacific/Midway':
      goog.require('bluemind.timezone.PacificMidway');
      ret = bluemind.timezone.PacificMidway;
      break;
    case 'Pacific/Nauru':
      goog.require('bluemind.timezone.PacificNauru');
      ret = bluemind.timezone.PacificNauru;
      break;
    case 'Pacific/Niue':
      goog.require('bluemind.timezone.PacificNiue');
      ret = bluemind.timezone.PacificNiue;
      break;
    case 'Pacific/Norfolk':
      goog.require('bluemind.timezone.PacificNorfolk');
      ret = bluemind.timezone.PacificNorfolk;
      break;
    case 'Pacific/Noumea':
      goog.require('bluemind.timezone.PacificNoumea');
      ret = bluemind.timezone.PacificNoumea;
      break;
    case 'Pacific/Pago_Pago':
      goog.require('bluemind.timezone.PacificPagoPago');
      ret = bluemind.timezone.PacificPagoPago;
      break;
    case 'Pacific/Palau':
      goog.require('bluemind.timezone.PacificPalau');
      ret = bluemind.timezone.PacificPalau;
      break;
    case 'Pacific/Pitcairn':
      goog.require('bluemind.timezone.PacificPitcairn');
      ret = bluemind.timezone.PacificPitcairn;
      break;
    case 'Pacific/Pohnpei':
      goog.require('bluemind.timezone.PacificPohnpei');
      ret = bluemind.timezone.PacificPohnpei;
      break;
    case 'Pacific/Port_Moresby':
      goog.require('bluemind.timezone.PacificPortMoresby');
      ret = bluemind.timezone.PacificPortMoresby;
      break;
    case 'Pacific/Rarotonga':
      goog.require('bluemind.timezone.PacificRarotonga');
      ret = bluemind.timezone.PacificRarotonga;
      break;
    case 'Pacific/Saipan':
      goog.require('bluemind.timezone.PacificSaipan');
      ret = bluemind.timezone.PacificSaipan;
      break;
    case 'Pacific/Tahiti':
      goog.require('bluemind.timezone.PacificTahiti');
      ret = bluemind.timezone.PacificTahiti;
      break;
    case 'Pacific/Tarawa':
      goog.require('bluemind.timezone.PacificTarawa');
      ret = bluemind.timezone.PacificTarawa;
      break;
    case 'Pacific/Tongatapu':
      goog.require('bluemind.timezone.PacificTongatapu');
      ret = bluemind.timezone.PacificTongatapu;
      break;
    case 'Pacific/Wake':
      goog.require('bluemind.timezone.PacificWake');
      ret = bluemind.timezone.PacificWake;
      break;
    case 'Pacific/Wallis':
      goog.require('bluemind.timezone.PacificWallis');
      ret = bluemind.timezone.PacificWallis;
      break;
    case 'UTC':
      goog.require('bluemind.timezone.UTC');
      ret = bluemind.timezone.UTC;
      break;
    default:
      // FIXME: default timezone
      goog.require('bluemind.timezone.EuropeParis');
      ret = bluemind.timezone.EuropeParis;
      break;
  }
  return ret;
};
