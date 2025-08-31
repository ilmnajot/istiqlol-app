
alter table student_contracts add column "uniqueId" varchar(255);

alter table student_contracts rename column "uniqueId" to unique_id;
