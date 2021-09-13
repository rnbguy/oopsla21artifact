use sqlparser::dialect;
use sqlparser::parser::Parser;

fn main() {
    let sql = "CREATE TABLE ipblocks (
        ipb_id int identity NOT NULL,
        ipb_address varbinary(1024) NOT NULL,
        ipb_user int NOT NULL,
        ipb_by int NOT NULL,
        ipb_by_text varbinary(255) NOT NULL,
        ipb_reason varbinary(1024) NOT NULL,
        ipb_timestamp binary(14) NOT NULL,
        ipb_auto tinyint NOT NULL,
        ipb_anon_only tinyint NOT NULL,
        ipb_create_account tinyint NOT NULL ,
        ipb_enable_autoblock tinyint NOT NULL ,
        ipb_expiry varbinary(14) NOT NULL,
        ipb_range_start varbinary(1024) NOT NULL,
        ipb_range_end varbinary(1024) NOT NULL,
        ipb_deleted tinyint NOT NULL ,
        ipb_block_email tinyint NOT NULL ,
        ipb_allow_usertalk tinyint NOT NULL ,
        PRIMARY KEY (ipb_id),
        UNIQUE (ipb_address,ipb_user,ipb_auto,ipb_anon_only)
      );";

    // INSERT INTO animals (name,) VALUES ("cow",);

    let parsed_sql = Parser::parse_sql(&dialect::AnsiDialect {}, sql)
        .or_else(|_| Parser::parse_sql(&dialect::GenericDialect {}, sql))
        .or_else(|_| Parser::parse_sql(&dialect::MsSqlDialect {}, sql))
        .or_else(|_| Parser::parse_sql(&dialect::MySqlDialect {}, sql))
        .or_else(|_| Parser::parse_sql(&dialect::PostgreSqlDialect {}, sql));

    println!("{:?}", parsed_sql);

    let sql = "CREATE TABLE page_restrictions (
  pr_page int NOT NULL,
  pr_type varbinary NOT NULL,
  pr_level varbinary NOT NULL,
  pr_cascade tinyint NOT NULL,
  pr_user int DEFAULT NULL,
  pr_expiry varbinary DEFAULT NULL,
  pr_id int NOT NULL,
  PRIMARY KEY (pr_id),
  UNIQUE (pr_page,pr_type)
);";

    let parsed_sql = Parser::parse_sql(&dialect::AnsiDialect {}, sql)
        .or_else(|_| Parser::parse_sql(&dialect::GenericDialect {}, sql))
        .or_else(|_| Parser::parse_sql(&dialect::MsSqlDialect {}, sql))
        .or_else(|_| Parser::parse_sql(&dialect::MySqlDialect {}, sql))
        .or_else(|_| Parser::parse_sql(&dialect::PostgreSqlDialect {}, sql));

    println!("{:?}", parsed_sql);
}
