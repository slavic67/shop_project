
alter table public.outbox_event
    add column if not exists trace_parent VARCHAR(255);