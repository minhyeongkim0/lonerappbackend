create table if not exists recommendation_posts (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    title varchar(120) not null,
    content varchar(2000) not null,
    created_at timestamptz not null default now()
);

create table if not exists recommendation_votes (
    post_id uuid not null references recommendation_posts(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    vote_type smallint not null check (vote_type in (1, -1)),
    created_at timestamptz not null default now(),
    primary key (post_id, user_id)
);

create index if not exists idx_recommendation_posts_created_at
    on recommendation_posts (created_at desc);

create index if not exists idx_recommendation_votes_post_vote_type
    on recommendation_votes (post_id, vote_type);

create table if not exists refund_requests (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    amount integer not null check (amount > 0),
    status varchar(20) not null check (status in ('pending', 'approved', 'rejected')),
    bank_name varchar(100) not null,
    account_number varchar(100) not null,
    created_at timestamptz not null default now(),
    processed_at timestamptz,
    processed_by uuid references users(id),
    rejected_reason varchar(200)
);

create index if not exists idx_refund_requests_user_created_at
    on refund_requests (user_id, created_at desc);

create index if not exists idx_refund_requests_status_created_at
    on refund_requests (status, created_at desc);
