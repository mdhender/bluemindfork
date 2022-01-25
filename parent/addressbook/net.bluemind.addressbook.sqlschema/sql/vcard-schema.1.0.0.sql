


create table t_addressbook_vcard (
   kind text NOT NULL,
   
   source text,
   /* 
    * 
    * identification 
    * 
    * 
    */
   
   
   formated_name text NOT NULL,
   formated_name_parameters text,
   /*  Family Names (also known as surnames), Given
      Names, Additional Names, Honorific Prefixes, and Honorific
      Suffixes. */
   familynames text, /* each value is separated with a ',' */
   givennames text,
   additionalnames text,
   honorificprefixes text,
   honoricficsuffixes text,   
   name_parameters text,
   /* 
     The nickname is the descriptive name given instead of
      or in addition to the one belonging to the object the vCard
      represents.  It can also be used to specify a familiar form of a
      proper name specified by the FN or N properties.
    */
   nickname text,
   nickname_parameters text,

   /*
     To specify the birth date of the object the vCard
      represents.
    */
   bday date,
   
   anniversary date,

   /*  To specify the components of the sex and gender identity of
      the object the vCard represents.
   */
   gender varchar(40),
   gender_text text,
   /*
    *  Delivery Addressing Properties
    * 
    */
   
   /*  ADR
   Purpose:  To specify the components of the delivery address for the
      vCard object. */
   adr_label text[],
   /* the post office box; */
   postofficebox text[],
   /*  the extended address (e.g., apartment or suite number); */ 
   extendedaddress text[],
   /*  the street address; */ 
   streetaddress text[],    
   /* the  locality (e.g., city); */
   locality text[],
   /*  the region (e.g., state or province); */
   region text[],
   /*  the postal code; */
   postalcode text[],
   /*  the country name  */
   countryname text[],  
   adr_parameters text[],
   
   /*
    *  Communications Properties
    * 
    */
   
   tel text[],
   tel_parameters text[],
   
   email text[],
   email_parameters text[],
   
   impp text[],
   impp_parameters text[],
   
   lang text[],
   lang_parameters text[],
   
   /*
    * Geographical Properties
    */
   tz text[],
   tz_parameters text[],
   geo text[],
   geo_parameters text[],
   
   /*
    * 6.6.  Organizational Properties
    */
   /* To specify the position or job of the object */
   title text,

   /* To specify the function or part played in a particular situation */ 
   role text,

   /*  To specify the organizational name and units */
   company text,
   division text,
   department text,

   member_container_uid text[],
   member_item_uid text[],
   member_cn text[],
   member_mailto text[],
   
   /*
    *  6.7 Explanatory Properties
    */
   urls text[],
   urls_parameters text[],
   categories text[],
   note text,
   /* not sure .. */
   uid text[],
   /*  To specify the calendar user address [RFC5545] to which a scheduling request */ 
   caladruri text[],

    /*
     * 6.6.6.  RELATED
     */
   spouse text,
   manager text,
   assistant text,
    
    /*
     * 6.8.  Security Properties
     */
   pem text,
   pem_parameters text,

   item_id bigint references t_container_item(id) on delete cascade
);

create index i_addressbook_vcard_item on t_addressbook_vcard (item_id);
create index idx_addressbook_vcard_email on t_addressbook_vcard using gin (email);
create index idx_member_item_uid on t_addressbook_vcard using gin (member_item_uid);
