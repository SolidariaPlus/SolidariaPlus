-- Synthetic seed of MS-17 (KAN-16 §9). Loaded only by the local and qa profiles, never in Production.
-- It holds no credential: members activate with a code a teller issues, staff sign in against the
-- directory and enrol their authenticator on the first sign-in. Every statement is idempotent and
-- keeps rows that already exist, such as a staff profile created by an earlier sign-in.

-- Members M1 to M4 of the synthetic universe, waiting for their activation code.
WITH member(sub, member_id, member_number) AS (
    VALUES ('01930000-0000-7000-8000-000000000101'::uuid, '01930000-0000-7000-8000-000000000201'::uuid, '1001'),
           ('01930000-0000-7000-8000-000000000102'::uuid, '01930000-0000-7000-8000-000000000202'::uuid, '1002'),
           ('01930000-0000-7000-8000-000000000103'::uuid, '01930000-0000-7000-8000-000000000203'::uuid, '1003'),
           ('01930000-0000-7000-8000-000000000104'::uuid, '01930000-0000-7000-8000-000000000204'::uuid, '1004')
),
new_subject AS (
    INSERT INTO subjects (sub, member_id)
    SELECT m.sub, m.member_id
    FROM member m
    WHERE NOT EXISTS (SELECT 1 FROM member_accounts a WHERE a.member_number = m.member_number)
    ON CONFLICT DO NOTHING
    RETURNING sub
)
INSERT INTO member_accounts (member_id, sub, member_number, status)
SELECT m.member_id, m.sub, m.member_number, 'PENDING_ACTIVATION'
FROM member m
JOIN new_subject n ON n.sub = m.sub
ON CONFLICT DO NOTHING;

-- Six staff subjects; their accounts are in the synthetic directory (infra/ad/staff.csv, application-local.yml).
WITH staff(sub, directory_account, roles, functions) AS (
    VALUES ('01930000-0000-7000-8000-000000000001'::uuid, 'ana.prue01', ARRAY['TELLER'], ARRAY[]::text[]),
           ('01930000-0000-7000-8000-000000000002'::uuid, 'fernanda.prue19', ARRAY['BOARD'], ARRAY['PRESIDENT']),
           ('01930000-0000-7000-8000-000000000003'::uuid, 'esteban.fict20', ARRAY['BOARD'], ARRAY['COMPLIANCE']),
           ('01930000-0000-7000-8000-000000000004'::uuid, 'daniela.prue13', ARRAY['BOARD'], ARRAY[]::text[]),
           ('01930000-0000-7000-8000-000000000005'::uuid, 'isabel.ejem21', ARRAY['ACCOUNTANT'], ARRAY[]::text[]),
           ('01930000-0000-7000-8000-000000000006'::uuid, 'sebastian.fict26', ARRAY['EXTERNAL_CONSULTANT'], ARRAY[]::text[])
),
new_subject AS (
    INSERT INTO subjects (sub)
    SELECT s.sub
    FROM staff s
    WHERE NOT EXISTS (SELECT 1 FROM staff_profiles p WHERE p.directory_account = s.directory_account)
    ON CONFLICT DO NOTHING
    RETURNING sub
)
INSERT INTO staff_profiles (sub, directory_account, roles, functions)
SELECT s.sub, s.directory_account, s.roles, s.functions
FROM staff s
JOIN new_subject n ON n.sub = s.sub
ON CONFLICT DO NOTHING;
