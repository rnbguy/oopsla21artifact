DROP TABLE IF EXISTS CHECKING;
DROP TABLE IF EXISTS SAVINGS;
DROP TABLE IF EXISTS ACCOUNTS;

CREATE TABLE ACCOUNTS (
    custid      BIGINT      NOT NULL,
    name        VARCHAR(64) NOT NULL,
    PRIMARY KEY (custid)
);

CREATE TABLE SAVINGS (
    custid      BIGINT      NOT NULL,
    bal         FLOAT       NOT NULL,
    PRIMARY KEY (custid),
);

CREATE TABLE CHECKING (
    custid      BIGINT      NOT NULL,
    bal         FLOAT       NOT NULL,
    PRIMARY KEY (custid),
);
