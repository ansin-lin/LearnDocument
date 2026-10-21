INSERT INTO employees (id, display_name) VALUES
(1001,'Demo-01'),(1002,'Demo-02'),(1003,'Demo-03'),(1004,'Demo-04'),(1005,'Demo-05'),
(1006,'Demo-06'),(1007,'Demo-07'),(1008,'Demo-08'),(1009,'Demo-09'),(1010,'Demo-10'),
(1011,'Demo-11'),(1012,'Demo-12'),(1013,'Demo-13'),(1014,'Demo-14'),(1015,'Demo-15'),
(1016,'Demo-16'),(1017,'Demo-17'),(1018,'Demo-18'),(1019,'Demo-19'),(1020,'Demo-20'),
(1021,'Demo-21'),(1022,'Demo-22'),(1023,'Demo-23'),(1024,'Demo-24'),(1025,'Demo-25');
INSERT INTO attendance_records (id, employee_id, business_date, start_at, end_at, break_minutes)
SELECT X, 1000 + X, DATE '2026-10-19',
       TIMESTAMP '2026-10-19 09:00:00', TIMESTAMP '2026-10-19 18:00:00', 60
FROM SYSTEM_RANGE(1, 25);
