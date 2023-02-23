create table IF NOT EXISTS t_smime_revocations (
  serial_number text not null,
  revocation_date timestamp not null,
  revocation_reason text,
  
  /*
   * CRL data
   */
  url text,
  last_update timestamp,
  next_update timestamp,
  
  issuer text not null,
  ca_item_id bigint not null references t_smime_cacerts(item_id) on delete cascade,
  PRIMARY KEY(serial_number, ca_item_id)
  );

create index i_smime_revocations_next_update_idx on t_smime_revocations(next_update);
create index i_smime_revocations_ca_item_id_idx on t_smime_revocations(ca_item_id);
create index i_smime_revocations_issuer_idx on t_smime_revocations(issuer);
