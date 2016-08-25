create table REG_MANIFEST_UPDATE_STATUSES (
    url varchar(1000) primary key,
    last_access_attempt datetime not null,
    last_access_flag_state integer not null,
    last_access_notices_json varchar(1000000) not null
);
