from pathlib import Path
import csv
import sqlite3
import bcrypt

ROOT = Path(__file__).resolve().parent
DB_PATH = ROOT / 'company.db'
SCHEMA_PATH = ROOT / 'DatabaseSetUp.sqlite3-query'

DATA = {
    'employment_status.csv': {
        'headers': ['id', 'name'],
        'rows': [
            [1, 'Active'],
            [2, 'On Leave'],
            [3, 'Terminated'],
        ]
    },
    'pay_type.csv': {
        'headers': ['id', 'name'],
        'rows': [
            [1, 'Salary'],
            [2, 'Hourly'],
        ]
    },
    'medical_plan.csv': {
        'headers': ['id', 'name'],
        'rows': [
            [1, 'Basic'],
            [2, 'Premium'],
            [3, 'Family'],
        ]
    },
    'department.csv': {
        'headers': ['department_id', 'name'],
        'rows': [
            [1, 'Engineering'],
            [2, 'HR'],
            [3, 'Sales'],
            [4, 'Administration'],
        ]
    },
    'job.csv': {
        'headers': ['job_id', 'department_id', 'title', 'base_salary'],
        'rows': [
            [1, 1, 'Software Engineer', 85000.0],
            [2, 1, 'QA Engineer', 70000.0],
            [3, 2, 'HR Manager', 75000.0],
            [4, 3, 'Sales Associate', 52000.0],
            [5, 4, 'Office Admin', 56000.0],
        ]
    },
    'address.csv': {
        'headers': ['address_id', 'line1', 'line2', 'city', 'state', 'zip'],
        'rows': [
            [1, '123 Main St', '', 'Chicago', 'IL', '60601'],
            [2, '456 Oak Ave', 'Apt 2', 'Houston', 'TX', '77001'],
            [3, '789 Pine Rd', '', 'Seattle', 'WA', '98101'],
            [4, '321 Maple Ln', '', 'Denver', 'CO', '80203'],
            [5, '654 Elm St', 'Suite 110', 'Boston', 'MA', '02110'],
            [6, '987 Cedar Blvd', '', 'Atlanta', 'GA', '30303'],
            [7, '250 Lakeview Dr', 'Unit 5', 'Minneapolis', 'MN', '55401'],
            [8, '111 River Rd', '', 'Phoenix', 'AZ', '85001'],
            [9, '500 Ocean Blvd', 'Apt 7', 'Miami', 'FL', '33101'],
            [10, '210 Hill St', '', 'Portland', 'OR', '97201'],
            [11, '77 Broadway', 'Floor 4', 'New York', 'NY', '10007'],
            [12, '19 Sunset Way', '', 'San Diego', 'CA', '92101'],
        ]
    },
    'employee.csv': {
        'headers': ['employee_id', 'job_id', 'first_name', 'last_name', 'date_hired', 'date_of_birth', 'email', 'status_id', 'pay_type_id', 'medical_plan_id', 'address_id', 'image_url'],
        'rows': [
            [1, 1, 'John', 'Doe', '2024-01-01', '1997-05-10', 'john.doe@email.com', 1, 1, 2, 1, ''],
            [2, 2, 'Jane', 'Smith', '2023-06-15', '1995-09-20', 'jane.smith@email.com', 1, 1, 1, 2, ''],
            [3, 3, 'Olivia', 'Brown', '2022-08-01', '1988-11-04', 'olivia.brown@email.com', 1, 1, 3, 3, ''],
            [4, 4, 'Noah', 'Davis', '2023-03-05', '1993-12-15', 'noah.davis@email.com', 1, 2, 1, 4, ''],
            [5, 5, 'Emma', 'Wilson', '2021-07-22', '1986-04-30', 'emma.wilson@email.com', 1, 1, 2, 5, ''],
            [6, 1, 'Liam', 'Moore', '2024-02-14', '1998-07-09', 'liam.moore@email.com', 1, 1, 1, 6, ''],
            [7, 2, 'Ava', 'Taylor', '2023-11-10', '1996-10-02', 'ava.taylor@email.com', 1, 2, 3, 7, ''],
            [8, 4, 'Mason', 'Anderson', '2022-05-18', '1992-03-24', 'mason.anderson@email.com', 1, 2, 1, 8, ''],
            [9, 1, 'Isabella', 'Thomas', '2024-01-28', '1999-02-11', 'isabella.thomas@email.com', 1, 1, 3, 9, ''],
            [10, 5, 'Lucas', 'Jackson', '2023-10-12', '1987-06-12', 'lucas.jackson@email.com', 1, 1, 2, 10, ''],
            [11, 3, 'Mia', 'White', '2022-12-03', '1990-01-08', 'mia.white@email.com', 2, 1, 1, 11, ''],
            [12, 4, 'Ethan', 'Harris', '2021-09-27', '1989-08-21', 'ethan.harris@email.com', 1, 2, 3, 12, ''],
        ]
    },
    'dependent.csv': {
        'headers': ['dependent_id', 'employee_id', 'name', 'relationship', 'date_of_birth'],
        'rows': [
            [1, 1, 'Anna Doe', 'Child', '2015-04-10'],
            [2, 2, 'Tom Smith', 'Spouse', '1997-02-14'],
            [3, 3, 'Mia Brown', 'Child', '2018-09-05'],
            [4, 5, 'Noah Wilson', 'Spouse', '1988-11-12'],
        ]
    },
    'employee_job_history.csv': {
        'headers': ['id', 'employee_id', 'job_id', 'start_date', 'end_date'],
        'rows': [
            [1, 1, 1, '2024-01-01', None],
            [2, 2, 2, '2023-06-15', None],
            [3, 3, 3, '2022-08-01', None],
            [4, 5, 5, '2021-07-22', None],
        ]
    },
    'time_entry.csv': {
        'headers': ['id', 'employee_id', 'clock_in', 'clock_out'],
        'rows': [
            [1, 1, '2024-05-01 09:00:00', '2024-05-01 17:00:00'],
            [2, 2, '2024-05-01 08:30:00', '2024-05-01 16:30:00'],
            [3, 3, '2024-05-01 10:00:00', '2024-05-01 18:00:00'],
            [4, 5, '2024-05-01 08:00:00', '2024-05-01 16:00:00'],
        ]
    },
}

USERS = {
    'headers': ['user_id', 'username', 'password_hash', 'role', 'employee_id'],
    'rows': []
}

USER_RECORDS = [
    (1, 'HR0001', 'password', 'HR', 3),
    (2, 'ADMIN001', 'admin123', 'ADMIN', 5),
    (3, 'EMP0001', 'password', 'EMPLOYEE', 1),
]

SCHEMA_SQL = '''-- database: company.db

PRAGMA foreign_keys = ON;

CREATE TABLE employment_status (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE pay_type (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE medical_plan (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE department (
  department_id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE job (
  job_id INTEGER PRIMARY KEY,
  department_id INTEGER NOT NULL,
  title TEXT NOT NULL,
  base_salary REAL,
  FOREIGN KEY (department_id) REFERENCES department(department_id)
);

CREATE TABLE address (
  address_id INTEGER PRIMARY KEY,
  line1 TEXT NOT NULL,
  line2 TEXT,
  city TEXT,
  state TEXT,
  zip TEXT
);

CREATE TABLE employee (
  employee_id INTEGER PRIMARY KEY,
  job_id INTEGER,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  date_hired DATE NOT NULL,
  date_of_birth DATE NOT NULL,
  email TEXT UNIQUE NOT NULL,
  status_id INTEGER NOT NULL,
  pay_type_id INTEGER NOT NULL,
  medical_plan_id INTEGER,
  address_id INTEGER,
  image_url TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME,
  FOREIGN KEY (job_id) REFERENCES job(job_id),
  FOREIGN KEY (status_id) REFERENCES employment_status(id),
  FOREIGN KEY (pay_type_id) REFERENCES pay_type(id),
  FOREIGN KEY (medical_plan_id) REFERENCES medical_plan(id),
  FOREIGN KEY (address_id) REFERENCES address(address_id)
);

CREATE TABLE dependent (
  dependent_id INTEGER PRIMARY KEY,
  employee_id INTEGER NOT NULL,
  name TEXT NOT NULL,
  relationship TEXT,
  date_of_birth DATE,
  FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
);

CREATE TABLE employee_job_history (
  id INTEGER PRIMARY KEY,
  employee_id INTEGER NOT NULL,
  job_id INTEGER NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE,
  FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
  FOREIGN KEY (job_id) REFERENCES job(job_id)
);

CREATE TABLE time_entry (
  id INTEGER PRIMARY KEY,
  employee_id INTEGER NOT NULL,
  clock_in DATETIME NOT NULL,
  clock_out DATETIME,
  FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
  CHECK (clock_out IS NULL OR clock_out > clock_in)
);

CREATE INDEX idx_employee_job ON employee(job_id);
CREATE INDEX idx_employee_status ON employee(status_id);
CREATE INDEX idx_time_entry_employee ON time_entry(employee_id);
CREATE INDEX idx_time_entry_date ON time_entry(clock_in);
'''

USERS_TABLE_SQL = '''CREATE TABLE IF NOT EXISTS users (
  user_id INTEGER PRIMARY KEY,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('EMPLOYEE', 'HR', 'ADMIN')),
  employee_id INTEGER,
  FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
);
'''


def write_csv(filename, headers, rows):
    path = ROOT / filename
    with path.open('w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(headers)
        for row in rows:
            writer.writerow(['' if value is None else value for value in row])


def build_user_rows():
    rows = []
    for user_id, username, password, role, employee_id in USER_RECORDS:
        hashed = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt(rounds=12)).decode('utf-8')
        rows.append([user_id, username, hashed, role, employee_id])
    return rows


def ensure_schema():
    if SCHEMA_PATH.exists():
        SCHEMA_PATH.unlink()
    SCHEMA_PATH.write_text(SCHEMA_SQL, encoding='utf-8')


def rebuild_db():
    if DB_PATH.exists():
        DB_PATH.unlink()
    conn = sqlite3.connect(DB_PATH)
    conn.execute('PRAGMA foreign_keys = ON;')
    cur = conn.cursor()
    cur.executescript(SCHEMA_SQL)
    cur.executescript(USERS_TABLE_SQL)

    for filename, source in DATA.items():
        row_values = []
        for row in source['rows']:
            row_values.append([None if value == '' else value for value in row])
        placeholders = ','.join('?' for _ in source['headers'])
        cur.executemany(
            f"INSERT INTO {filename[:-4]} ({', '.join(source['headers'])}) VALUES ({placeholders})",
            row_values
        )

    user_rows = build_user_rows()
    cur.executemany(
        'INSERT INTO users (user_id, username, password_hash, role, employee_id) VALUES (?, ?, ?, ?, ?)',
        user_rows
    )

    conn.commit()
    conn.close()
    return len(user_rows)


def main():
    ensure_schema()
    for filename, source in DATA.items():
        write_csv(filename, source['headers'], source['rows'])

    USERS['rows'] = build_user_rows()
    write_csv('users.csv', USERS['headers'], USERS['rows'])

    inserted_users = rebuild_db()
    print(f'Created {DB_PATH} and populated it from {len(DATA) + 1} CSV files.')
    print(f'Inserted {inserted_users} user records.')


if __name__ == '__main__':
    main()
