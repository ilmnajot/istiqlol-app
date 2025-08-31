insert into company (id, name, cash_balance, card_balance, created_by, created_at, updated_at, updated_by)
values (1, 'IFTIXOR_PRIVATE_SCHOOL', 10000000, 10000000, 1, now(), now(), 1);


insert into roles (id, created_at, created_by, deleted, updated_at, updated_by, name)
values (1, now(), 1, false, now(), 1, 'ADMIN'),
       (2, now(), 1, false, now(), 1, 'TEACHER'),
       (3, now(), 1, false, now(), 1, 'HR'),
       (4, now(), 1, false, now(), 1, 'EMPLOYEE'),
       (5, now(), 1, false, now(), 1, 'DEVELOPER'),
       (6, now(), 1, false, now(), 1, 'CASHIER'),
       (7, now(), 1, false, now(), 1, 'OWNER'),
       (8, now(), 1, false, now(), 1, 'RECEPTION');


insert into category(id, created_at, created_by, deleted, updated_at, updated_by, category_status, name)
values (1, now(), 1, false, now(), 1, 'INCOME', 'O''QUVCHIDAN_KIRIM'),
       (2, now(), 1, false, now(), 1, 'OUTCOME', 'XODIMGA_OYLIK'),
       (2, now(), 1, false, now(), 1, 'ADJUSTMENT', 'ADJUSTMENT');
