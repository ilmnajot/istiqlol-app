alter table student_contracts
  drop column contract_number;


select student_contracts.contract_number
from student_contracts
where student_contracts.contract_number is not null;