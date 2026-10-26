

alter table orders
drop constraint check_status,
add constraint check_status check ( status in ('CREATED', 'PAID', 'CANCELLED','PENDING','AWAITING_PAYMENT') );