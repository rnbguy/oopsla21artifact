
-- contestants table holds the contestants numbers (for voting) and names
CREATE TABLE CONTESTANTS
(
  contestant_number integer     NOT NULL
, contestant_name   varchar(50) NOT NULL
, PRIMARY KEY
  (
    contestant_number
  )
);

-- Map of Area Codes and States for geolocation classification of incoming calls
CREATE TABLE AREA_CODE_STATE
(
  area_code smallint   NOT NULL
, state     varchar(2) NOT NULL
, PRIMARY KEY
  (
    area_code
  )
);

-- votes table holds every valid vote.
--   voters are not allowed to submit more than <x> votes, x is passed to client application
CREATE TABLE VOTES
(
  vote_id            bigint     NOT NULL 
, phone_number       bigint     NOT NULL
, state              varchar(2) NOT NULL 
, contestant_number  integer    NOT NULL REFERENCES CONTESTANTS (contestant_number)
, created            timestamp  NOT NULL
);
