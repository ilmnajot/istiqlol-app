truncate table transaction restart identity cascade;
truncate table monthly_fee restart identity cascade;
truncate table company restart identity cascade ;
truncate table student_contracts restart identity cascade ;

truncate table expenses restart identity cascade ;
truncate table teacher_table restart identity cascade;

truncate table student_tariff restart identity cascade;

insert into company (id, name, cash_balance, card_balance, created_by, created_at, updated_at, updated_by)
values (1, 'IFTIXOR_PRIVATE_SCHOOL', 0, 0, 1, now(), now(), 1);

insert into roles (id, created_at, created_by, deleted, updated_at, updated_by, name)
values (1, now(), 1, false, now(), 1, 'ADMIN'),
       (2, now(), 1, false, now(), 1, 'TEACHER'),
       (3, now(), 1, false, now(), 1, 'HR'),
       (4, now(), 1, false, now(), 1, 'EMPLOYEE'),
       (5, now(), 1, false, now(), 1, 'DEVELOPER'),
       (6, now(), 1, false, now(), 1, 'CASHIER'),
       (7, now(), 1, false, now(), 1, 'OWNER'),
       (8, now(), 1, false, now(), 1, 'RECEPTION');


insert into category( created_at, created_by, deleted, updated_at, updated_by, category_status, name)
values (now(), 1, false, now(), 1, 'INCOME', 'O''QUVCHIDAN_KIRIM'),
       (now(), 1, false, now(), 1, 'OUTCOME', 'XODIMGA_OYLIK'),
       (now(), 1, false, now(), 1, 'ADJUSTMENT', 'ADJUSTMENT');


truncate table student_tariff restart identity cascade ;


truncate table moliya_db.public.teacher_table restart identity cascade;
truncate table transaction restart identity cascade ;
truncate table monthly_fee restart identity cascade;

