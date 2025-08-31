truncate table transaction restart identity cascade;
truncate table monthly_fee restart identity cascade;
truncate table student_contracts restart identity cascade;
truncate table expenses restart identity cascade ;
truncate table teacher_table restart identity cascade;
truncate table teacher_contract restart identity cascade;
truncate table student_tariff restart identity cascade;

delete from users where id != 1;

truncate table category restart identity cascade;

insert into category(id, created_at, created_by, deleted, updated_at, updated_by, category_status, name)
values (1, now(), 1, false, now(), 1, 'INCOME', 'O''QUVCHIDAN_KIRIM'),
       (2, now(), 1, false, now(), 1, 'OUTCOME', 'XODIMGA_OYLIK'),
       (3, now(), 1, false, now(), 1, 'ADJUSTMENT', 'ADJUSTMENT');

truncate table teacher_table restart identity cascade;
truncate table transaction restart identity cascade ;
truncate table monthly_fee restart identity cascade;
truncate table student_contracts restart identity cascade;

