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
