create table MANIFEST_STATUS (
    url varchar(1000) primary key,
    last_access_attempt datetime not null,
    last_access_successful boolean not null,
    last_access_error_message varchar(1000) default null
);
