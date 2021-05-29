drop table if exists user_account;
create table user_account(
    user_account varchar(20) primary key,
    password varchar(128),
    mobile char(11) unique ,
    email varchar(20) unique
);
insert into user_account (user_account, password, mobile, email) VALUES ('user001', 'e10adc3949ba59abbe56e057f20f883e', '15334254228', '627330472@qq.com');

drop table if exists avg_diff;
create table avg_diff(
                         date_time datetime ,
                         plat_name varchar(20) ,
                         diff_price double,
                         PRIMARY KEY (date_time,plat_name)

);
insert into avg_diff values (datetime('now', 'localtime'),'platA',1.222);
