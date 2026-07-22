-- Core identity/auth table. One row per person who can log in, regardless of role.
create table users (
    id            uuid          primary key,
    full_name     varchar(120)  not null,
    email         varchar(180)  not null,
    mobile        varchar(20)   not null,
    password_hash varchar(100)  not null,
    role          varchar(20)   not null,
    status        varchar(20)   not null,
    created_at    timestamptz   not null,
    updated_at    timestamptz   not null,
    constraint uq_users_email unique (email)
);

-- Role-specific fields for students, kept out of the users table to avoid a wide,
-- mostly-null row. One student profile per user.
create table student_profiles (
    id               uuid         primary key,
    user_id          uuid         not null,
    admission_number varchar(40)  not null,
    student_class    varchar(20)  not null,
    section          varchar(10)  not null,
    roll_number      varchar(20)  not null,
    parent_mobile    varchar(20)  not null,   -- used later to verify a parent-child link
    student_mobile   varchar(20),
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    constraint uq_student_user      unique (user_id),
    constraint uq_student_admission unique (admission_number),
    constraint fk_student_user      foreign key (user_id) references users (id)
);

-- Role-specific fields for teachers.
create table teacher_profiles (
    id          uuid         primary key,
    user_id     uuid         not null,
    employee_id varchar(40)  not null,
    department  varchar(80)  not null,
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null,
    constraint uq_teacher_user     unique (user_id),
    constraint uq_teacher_employee unique (employee_id),
    constraint fk_teacher_user     foreign key (user_id) references users (id)
);

-- Parent-child linking (phase 3) matches on admission_number + parent_mobile,
-- so index the mobile we will look up by.
create index idx_student_parent_mobile on student_profiles (parent_mobile);
