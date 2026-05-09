package com.jbaker454;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmployeeService {
    private static final String DB_URL = "jdbc:sqlite:database/company.db";
    private static final DateTimeFormatter DB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FORMAT = DB_FORMAT;

    public static String getDailySummary(int employeeId) {
        StringBuilder builder = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT e.first_name, e.last_name, j.title, j.base_salary, p.name AS pay_type "
                    + "FROM employee e "
                    + "LEFT JOIN job j ON e.job_id = j.job_id "
                    + "LEFT JOIN pay_type p ON e.pay_type_id = p.id "
                    + "WHERE e.employee_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, employeeId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String title = rs.getString("title");
                    String payType = rs.getString("pay_type");
                    double baseSalary = rs.getDouble("base_salary");

                    builder.append("Employee: ").append(firstName).append(" ").append(lastName).append("\n");
                    builder.append("Role: ").append(title == null ? "Unknown role" : title).append("\n");
                    builder.append("Pay Type: ").append(payType == null ? "Unknown" : payType).append("\n");
                    builder.append("Base Salary/Rate: $").append(String.format("%.2f", baseSalary)).append(payType != null && payType.equalsIgnoreCase("Salary") ? " per year" : " per hour").append("\n\n");

                    Duration weekDuration = getCurrentWeekDuration(conn, employeeId);
                    builder.append("This week: ").append(formatDuration(weekDuration)).append(" (")
                            .append(String.format("%.2f", weekDuration.toMinutes() / 60.0)).append(" hours)").append("\n\n");
                    builder.append(getLastTimeEntryState(conn, employeeId));
                } else {
                    builder.append("Unable to find employee details for ID ").append(employeeId).append(".\n");
                }
            }
        } catch (SQLException e) {
            builder.append("Error loading employee summary: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static String clockIn(int employeeId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (isCurrentlyClockedIn(employeeId)) {
                return "Already clocked in. Please clock out before starting a new shift.";
            }

            String insertSql = "INSERT INTO time_entry (employee_id, clock_in) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                LocalDateTime now = LocalDateTime.now();
                insertStmt.setInt(1, employeeId);
                insertStmt.setString(2, now.format(DB_FORMAT));
                insertStmt.executeUpdate();
                return "Clocked in at " + now.format(DISPLAY_FORMAT) + ".\nPress Clock Out when your shift ends.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Unable to clock in: " + e.getMessage();
        }
    }

    public static String clockOut(int employeeId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String selectSql = "SELECT id, clock_in FROM time_entry WHERE employee_id = ? AND clock_out IS NULL ORDER BY clock_in DESC LIMIT 1";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, employeeId);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    int entryId = rs.getInt("id");
                    LocalDateTime clockIn = LocalDateTime.parse(rs.getString("clock_in"), DB_FORMAT);
                    LocalDateTime now = LocalDateTime.now();
                    String updateSql = "UPDATE time_entry SET clock_out = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, now.format(DB_FORMAT));
                        updateStmt.setInt(2, entryId);
                        updateStmt.executeUpdate();
                    }
                    Duration sessionDuration = Duration.between(clockIn, now);
                    return "Clocked out at " + now.format(DISPLAY_FORMAT) + ".\n"
                            + "Shift length: " + formatDuration(sessionDuration) + ".\n"
                            + "Your weekly totals have been updated.";
                }
                return "No active clock-in found. Please clock in before clocking out.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Unable to clock out: " + e.getMessage();
        }
    }

    public static String getProfileSummary(int employeeId) {
        StringBuilder builder = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT e.first_name, e.last_name, e.email, e.date_hired, e.date_of_birth, s.name AS status, p.name AS pay_type, j.title, d.name AS department "
                    + "FROM employee e "
                    + "LEFT JOIN employment_status s ON e.status_id = s.id "
                    + "LEFT JOIN pay_type p ON e.pay_type_id = p.id "
                    + "LEFT JOIN job j ON e.job_id = j.job_id "
                    + "LEFT JOIN department d ON j.department_id = d.department_id "
                    + "WHERE e.employee_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, employeeId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    builder.append("Name: ").append(rs.getString("first_name")).append(" ").append(rs.getString("last_name")).append("\n");
                    builder.append("Email: ").append(rs.getString("email")).append("\n");
                    builder.append("Department: ").append(rs.getString("department")).append("\n");
                    builder.append("Job Title: ").append(rs.getString("title")).append("\n");
                    builder.append("Employment Status: ").append(rs.getString("status")).append("\n");
                    builder.append("Pay Type: ").append(rs.getString("pay_type")).append("\n");
                    builder.append("Date Hired: ").append(rs.getString("date_hired")).append("\n");
                    builder.append("Date of Birth: ").append(rs.getString("date_of_birth")).append("\n");
                } else {
                    builder.append("No profile found for employee ID ").append(employeeId).append(".");
                }
            }
        } catch (SQLException e) {
            builder.append("Unable to load profile: ").append(e.getMessage());
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static String calculatePay(int employeeId) {
        StringBuilder builder = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT e.first_name, e.last_name, j.title, j.base_salary, p.name AS pay_type "
                    + "FROM employee e "
                    + "LEFT JOIN job j ON e.job_id = j.job_id "
                    + "LEFT JOIN pay_type p ON e.pay_type_id = p.id "
                    + "WHERE e.employee_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, employeeId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    double baseSalary = rs.getDouble("base_salary");
                    String payType = rs.getString("pay_type");

                    builder.append("Pay calculation for ").append(firstName).append(" ").append(lastName).append("\n");
                    builder.append("Pay Type: ").append(payType == null ? "Unknown" : payType).append("\n");

                    Duration weekDuration = getCurrentWeekDuration(conn, employeeId);
                    double weekHours = weekDuration.toMinutes() / 60.0;
                    double hourlyRate = baseSalary / 2080.0; // Assuming 2080 hours per year (40 hours/week * 52 weeks)

                    if (payType != null && payType.equalsIgnoreCase("Hourly")) {
                        
                        builder.append("Hourly rate: $").append(String.format("%.2f", hourlyRate)).append(" per hour\n");
                        double totalPay = weekHours * hourlyRate;
                        builder.append("Hours worked this week: ").append(String.format("%.2f", weekHours)).append(" hours\n");
                        builder.append("Current week pay estimate: $").append(String.format("%.2f", totalPay)).append("\n");
                        builder.append("Note: Salary employees are paid based on hours worked at the calculated hourly rate.");
                    } else {
                        weekHours = 40.0; // For hourly employees, we assume they are paid for all hours worked, even if they exceed 40 hours
                        builder.append("Rate: $").append(String.format("%.2f", baseSalary)).append(" per year\n");
                        double totalPay = weekHours * hourlyRate;
                        builder.append("Current week pay estimate: $").append(String.format("%.2f", totalPay)).append("\n");
                        builder.append("Details: ").append(formatDuration(weekDuration)).append(" of recorded work this week.");
                    }
                } else {
                    builder.append("No employee record found for ID ").append(employeeId).append(".");
                }
            }
        } catch (SQLException e) {
            builder.append("Error calculating pay: ").append(e.getMessage());
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static boolean isCurrentlyClockedIn(int employeeId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT clock_out FROM time_entry WHERE employee_id = ? ORDER BY clock_in DESC LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, employeeId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("clock_out") == null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static Duration getCurrentWeekDuration(Connection conn, int employeeId) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        LocalDateTime weekStart = weekStartDate.atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusDays(7);

        String sql = "SELECT clock_in, clock_out FROM time_entry WHERE employee_id = ?";
        Duration totalDuration = Duration.ZERO;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String clockInText = rs.getString("clock_in");
                if (clockInText == null) {
                    continue;
                }
                LocalDateTime clockIn = LocalDateTime.parse(clockInText, DB_FORMAT);
                String clockOutText = rs.getString("clock_out");
                LocalDateTime clockOut = clockOutText != null ? LocalDateTime.parse(clockOutText, DB_FORMAT) : LocalDateTime.now();

                LocalDateTime start = clockIn.isBefore(weekStart) ? weekStart : clockIn;
                LocalDateTime end = clockOut.isAfter(weekEnd) ? weekEnd : clockOut;

                if (end.isAfter(start)) {
                    totalDuration = totalDuration.plus(Duration.between(start, end));
                }
            }
        }
        return totalDuration;
    }

    private static String getLastTimeEntryState(Connection conn, int employeeId) throws SQLException {
        String sql = "SELECT clock_in, clock_out FROM time_entry WHERE employee_id = ? ORDER BY clock_in DESC LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String clockInText = rs.getString("clock_in");
                String clockOutText = rs.getString("clock_out");
                LocalDateTime clockIn = clockInText != null ? LocalDateTime.parse(clockInText, DB_FORMAT) : null;
                if (clockIn == null) {
                    return "No time entries yet. Clock in to start your first shift.";
                }
                if (clockOutText == null) {
                    Duration currentDuration = Duration.between(clockIn, LocalDateTime.now());
                    return "Currently clocked in since " + clockIn.format(DISPLAY_FORMAT) + ".\n"
                            + "Elapsed time: " + formatDuration(currentDuration) + ".\n"
                            + "Press Clock Out to end your shift.";
                }
                LocalDateTime clockOut = LocalDateTime.parse(clockOutText, DB_FORMAT);
                Duration lastSession = Duration.between(clockIn, clockOut);
                return "Last shift: " + clockIn.format(DISPLAY_FORMAT) + " to " + clockOut.format(DISPLAY_FORMAT) + ".\n"
                        + "Duration: " + formatDuration(lastSession) + ".";
            }
        }
        return "No time entries yet. Clock in to start your first shift.";
    }

    private static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return hours + "h " + minutes + "m";
    }

    // HR-specific methods

    /**
     * Get all employees for HR view
     */
    public static java.util.List<HRViewController.EmployeeRecord> getAllEmployees() {
        java.util.List<HRViewController.EmployeeRecord> employees = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT e.employee_id, e.first_name, e.last_name, d.name AS department, s.name AS status "
                    + "FROM employee e "
                    + "LEFT JOIN job j ON e.job_id = j.job_id "
                    + "LEFT JOIN department d ON j.department_id = d.department_id "
                    + "LEFT JOIN employment_status s ON e.status_id = s.id "
                    + "ORDER BY e.employee_id";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int empId = rs.getInt("employee_id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String department = rs.getString("department");
                    String status = rs.getString("status");
                    String fullName = firstName + " " + lastName;
                    employees.add(new HRViewController.EmployeeRecord(empId, fullName, department != null ? department : "Unassigned", status != null ? status : "Unknown"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employees;
    }

    /**
     * Search for employees by ID, Name, or Department
     */
    public static java.util.List<HRViewController.EmployeeRecord> searchEmployees(String searchTerm, String searchType) {
        java.util.List<HRViewController.EmployeeRecord> employees = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql;
            switch (searchType.toUpperCase()) {
                case "ID":
                    sql = "SELECT e.employee_id, e.first_name, e.last_name, d.name AS department, s.name AS status "
                            + "FROM employee e "
                            + "LEFT JOIN job j ON e.job_id = j.job_id "
                            + "LEFT JOIN department d ON j.department_id = d.department_id "
                            + "LEFT JOIN employment_status s ON e.status_id = s.id "
                            + "WHERE e.employee_id = ? "
                            + "ORDER BY e.employee_id";
                    break;
                case "DEPARTMENT":
                    sql = "SELECT e.employee_id, e.first_name, e.last_name, d.name AS department, s.name AS status "
                            + "FROM employee e "
                            + "LEFT JOIN job j ON e.job_id = j.job_id "
                            + "LEFT JOIN department d ON j.department_id = d.department_id "
                            + "LEFT JOIN employment_status s ON e.status_id = s.id "
                            + "WHERE d.name LIKE ? "
                            + "ORDER BY e.employee_id";
                    break;
                default: // "NAME"
                    sql = "SELECT e.employee_id, e.first_name, e.last_name, d.name AS department, s.name AS status "
                            + "FROM employee e "
                            + "LEFT JOIN job j ON e.job_id = j.job_id "
                            + "LEFT JOIN department d ON j.department_id = d.department_id "
                            + "LEFT JOIN employment_status s ON e.status_id = s.id "
                            + "WHERE e.first_name LIKE ? OR e.last_name LIKE ? "
                            + "ORDER BY e.employee_id";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if ("ID".equals(searchType)) {
                    pstmt.setInt(1, Integer.parseInt(searchTerm));
                } else if ("DEPARTMENT".equals(searchType)) {
                    pstmt.setString(1, "%" + searchTerm + "%");
                } else {
                    pstmt.setString(1, "%" + searchTerm + "%");
                    pstmt.setString(2, "%" + searchTerm + "%");
                }

                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int empId = rs.getInt("employee_id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String department = rs.getString("department");
                    String status = rs.getString("status");
                    String fullName = firstName + " " + lastName;
                    employees.add(new HRViewController.EmployeeRecord(empId, fullName, department != null ? department : "Unassigned", status != null ? status : "Unknown"));
                }
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        return employees;
    }

    /**
     * Get comprehensive employee details for HR view
     */
    public static String getHREmployeeDetails(int employeeId) {
        StringBuilder builder = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT e.employee_id, e.first_name, e.last_name, e.email, e.date_hired, e.date_of_birth, "
                    + "s.name AS status, p.name AS pay_type, j.title, j.base_salary, d.name AS department, a.line1, a.line2, a.city, a.state, a.zip "
                    + "FROM employee e "
                    + "LEFT JOIN employment_status s ON e.status_id = s.id "
                    + "LEFT JOIN pay_type p ON e.pay_type_id = p.id "
                    + "LEFT JOIN job j ON e.job_id = j.job_id "
                    + "LEFT JOIN department d ON j.department_id = d.department_id "
                    + "LEFT JOIN address a ON e.address_id = a.address_id "
                    + "WHERE e.employee_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, employeeId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    builder.append("===== EMPLOYEE PROFILE =====\n\n");
                    builder.append("Personal Information:\n");
                    builder.append("  Name: ").append(rs.getString("first_name")).append(" ").append(rs.getString("last_name")).append("\n");
                    builder.append("  Email: ").append(rs.getString("email")).append("\n");
                    builder.append("  Date of Birth: ").append(rs.getString("date_of_birth")).append("\n");
                    builder.append("  Address: ").append(rs.getString("line1")).append(", ").append(rs.getString("line2")).append(", ").append(rs.getString("city"))
                            .append(", ").append(rs.getString("state")).append(" ").append(rs.getString("zip")).append("\n\n");

                    builder.append("Employment Information:\n");
                    builder.append("  Employee ID: ").append(rs.getInt("employee_id")).append("\n");
                    builder.append("  Department: ").append(rs.getString("department")).append("\n");
                    builder.append("  Job Title: ").append(rs.getString("title")).append("\n");
                    builder.append("  Status: ").append(rs.getString("status")).append("\n");
                    builder.append("  Date Hired: ").append(rs.getString("date_hired")).append("\n\n");

                    builder.append("Payroll Information:\n");
                    builder.append("  Pay Type: ").append(rs.getString("pay_type")).append("\n");
                    builder.append("  Base Salary/Rate: $").append(String.format("%.2f", rs.getDouble("base_salary"))).append("\n");

                    // Get additional payroll stats
                    builder.append(getPayrollStats(conn, employeeId));
                } else {
                    builder.append("Employee ID ").append(employeeId).append(" not found in the system.");
                }
            }
        } catch (SQLException e) {
            builder.append("Error loading employee details: ").append(e.getMessage());
            e.printStackTrace();
        }
        return builder.toString();
    }

    private static String getPayrollStats(Connection conn, int employeeId) throws SQLException {
        StringBuilder builder = new StringBuilder();
        builder.append("\nPayroll Statistics:\n");

        // Get current week hours
        Duration weekDuration = getCurrentWeekDuration(conn, employeeId);
        double weekHours = weekDuration.toMinutes() / 60.0;
        builder.append("  Hours This Week: ").append(String.format("%.2f", weekHours)).append(" hours\n");

        // Get monthly hours (approximate - last 30 days)
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        LocalDateTime monthStart = thirtyDaysAgo.atStartOfDay();

        String sql = "SELECT SUM((julianday(COALESCE(clock_out, datetime('now'))) - julianday(clock_in)) * 24) as hours "
                + "FROM time_entry WHERE employee_id = ? AND clock_in >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setString(2, monthStart.format(DB_FORMAT));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double monthHours = rs.getDouble("hours");
                if (!rs.wasNull()) {
                    builder.append("  Hours (Last 30 Days): ").append(String.format("%.2f", monthHours)).append(" hours\n");
                }
            }
        }
        return builder.toString();
    }

    /**
     * Get editable employee data
     */
    public static String getEditableEmployeeData(int employeeId) {
        StringBuilder builder = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT e.first_name, e.last_name, e.email, e.date_of_birth, d.name AS department, "
                    + "j.title, s.name AS status, p.name AS pay_type, a.line1, a.line2, a.city, a.state, a.zip "
                    + "FROM employee e "
                    + "LEFT JOIN job j ON e.job_id = j.job_id "
                    + "LEFT JOIN department d ON j.department_id = d.department_id "
                    + "LEFT JOIN employment_status s ON e.status_id = s.id "
                    + "LEFT JOIN pay_type p ON e.pay_type_id = p.id "
                    + "LEFT JOIN address a ON e.address_id = a.address_id "
                    + "WHERE e.employee_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, employeeId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    builder.append(rs.getString("first_name")).append("|")
                            .append(rs.getString("last_name")).append("|")
                            .append(rs.getString("email")).append("|")
                            .append(rs.getString("date_of_birth")).append("|")
                            .append(rs.getString("department")).append("|")
                            .append(rs.getString("title")).append("|")
                            .append(rs.getString("status")).append("|")
                            .append(rs.getString("pay_type")).append("|")
                            .append(rs.getString("line1") != null ? rs.getString("line1") : "").append("|")
                            .append(rs.getString("line2") != null ? rs.getString("line2") : "").append("|")
                            .append(rs.getString("city") != null ? rs.getString("city") : "").append("|")
                            .append(rs.getString("state") != null ? rs.getString("state") : "").append("|")
                            .append(rs.getString("zip") != null ? rs.getString("zip") : "");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static java.util.List<String> getAllJobTitles() {
        java.util.List<String> jobTitles = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT title FROM job ORDER BY title";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    jobTitles.add(rs.getString("title"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jobTitles;
    }

    public static String getDepartmentNameForJobTitle(String jobTitle) {
        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            return "";
        }
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT d.name FROM job j "
                    + "LEFT JOIN department d ON j.department_id = d.department_id "
                    + "WHERE j.title = ? LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, jobTitle);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Add a new employee
     */
    public static String addEmployee(String firstName, String lastName, String email, String dateOfBirth,
                                     String department, String jobTitle, String status, String payType,
                                     String addressLine1, String addressLine2, String city, String state, String zip) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Create address entry if address data is provided
            int addressId = 0;
            if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
                addressId = createOrUpdateAddress(conn, 0, addressLine1, addressLine2, city, state, zip);
            }

            // Get IDs for job, status, and pay type. If department is not provided, derive it from the selected job.
            int deptId = getDepartmentId(conn, department, jobTitle);
            int jobId = getJobId(conn, jobTitle, deptId);
            int statusId = getStatusId(conn, status);
            int payTypeId = getPayTypeId(conn, payType);

            String sql = "INSERT INTO employee (first_name, last_name, email, date_of_birth, date_hired, job_id, status_id, pay_type_id, address_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, firstName);
                pstmt.setString(2, lastName);
                pstmt.setString(3, email);
                pstmt.setString(4, dateOfBirth);
                pstmt.setString(5, LocalDate.now().toString());
                pstmt.setInt(6, jobId);
                pstmt.setInt(7, statusId);
                pstmt.setInt(8, payTypeId);
                if (addressId > 0) {
                    pstmt.setInt(9, addressId);
                } else {
                    pstmt.setNull(9, java.sql.Types.INTEGER);
                }

                int rowsInserted = pstmt.executeUpdate();
                if (rowsInserted > 0) {
                    return "Employee '" + firstName + " " + lastName + "' added successfully.";
                } else {
                    return "Failed to add employee.";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error adding employee: " + e.getMessage();
        }
    }

    public static String updateEmployee(int employeeId, String firstName, String lastName, String email,
                                        String dateOfBirth, String department, String jobTitle, String status, String payType,
                                        String addressLine1, String addressLine2, String city, String state, String zip) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // First, get current employee data to preserve blank fields
            String currentDataSql = "SELECT e.first_name, e.last_name, e.email, e.date_of_birth, e.date_hired, e.address_id, d.name AS department, "
                    + "j.title, s.name AS status, p.name AS pay_type "
                    + "FROM employee e "
                    + "LEFT JOIN job j ON e.job_id = j.job_id "
                    + "LEFT JOIN department d ON j.department_id = d.department_id "
                    + "LEFT JOIN employment_status s ON e.status_id = s.id "
                    + "LEFT JOIN pay_type p ON e.pay_type_id = p.id "
                    + "WHERE e.employee_id = ?";

            String dateHired = null;
            int currentAddressId = 0;
            try (PreparedStatement currentStmt = conn.prepareStatement(currentDataSql)) {
                currentStmt.setInt(1, employeeId);
                ResultSet currentRs = currentStmt.executeQuery();
                
                if (currentRs.next()) {
                    // Use current value if new value is blank
                    firstName = (firstName == null || firstName.trim().isEmpty()) ? currentRs.getString("first_name") : firstName;
                    lastName = (lastName == null || lastName.trim().isEmpty()) ? currentRs.getString("last_name") : lastName;
                    email = (email == null || email.trim().isEmpty()) ? currentRs.getString("email") : email;
                    dateOfBirth = (dateOfBirth == null || dateOfBirth.trim().isEmpty()) ? currentRs.getString("date_of_birth") : dateOfBirth;
                    department = (department == null || department.trim().isEmpty()) ? currentRs.getString("department") : department;
                    jobTitle = (jobTitle == null || jobTitle.trim().isEmpty()) ? currentRs.getString("title") : jobTitle;
                    status = (status == null || status.trim().isEmpty()) ? currentRs.getString("status") : status;
                    payType = (payType == null || payType.trim().isEmpty()) ? currentRs.getString("pay_type") : payType;
                    dateHired = currentRs.getString("date_hired");
                    currentAddressId = currentRs.getInt("address_id");
                }
            }

            // Update or create address if address data is provided
            int addressId = currentAddressId;
            if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
                addressId = createOrUpdateAddress(conn, currentAddressId, addressLine1, addressLine2, city, state, zip);
            }

            int deptId = getDepartmentId(conn, department, jobTitle);
            int jobId = getJobId(conn, jobTitle, deptId);
            int statusId = getStatusId(conn, status);
            int payTypeId = getPayTypeId(conn, payType);

            String sql = "UPDATE employee SET first_name = ?, last_name = ?, email = ?, date_of_birth = ?, job_id = ?, status_id = ?, pay_type_id = ?, address_id = ? "
                    + "WHERE employee_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, firstName);
                pstmt.setString(2, lastName);
                pstmt.setString(3, email);
                pstmt.setString(4, dateOfBirth);
                pstmt.setInt(5, jobId);
                pstmt.setInt(6, statusId);
                pstmt.setInt(7, payTypeId);
                if (addressId > 0) {
                    pstmt.setInt(8, addressId);
                } else {
                    pstmt.setNull(8, java.sql.Types.INTEGER);
                }
                pstmt.setInt(9, employeeId);

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated > 0) {
                    return "Employee '" + firstName + " " + lastName + "' updated successfully.";
                } else {
                    return "No employee found with ID " + employeeId + ".";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error updating employee: " + e.getMessage();
        }
    }

    /**
     * Generate a comprehensive payroll report
     */
    public static String generatePayrollReport() {
        StringBuilder report = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            report.append("COMPANY PAYROLL SUMMARY REPORT\n");
            report.append("Generated: ").append(LocalDate.now()).append("\n");
            report.append("=".repeat(80)).append("\n\n");

            String sql = "SELECT e.employee_id, e.first_name, e.last_name, d.name AS department, "
                    + "j.title, j.base_salary, p.name AS pay_type, s.name AS status "
                    + "FROM employee e "
                    + "LEFT JOIN job j ON e.job_id = j.job_id "
                    + "LEFT JOIN department d ON j.department_id = d.department_id "
                    + "LEFT JOIN pay_type p ON e.pay_type_id = p.id "
                    + "LEFT JOIN employment_status s ON e.status_id = s.id "
                    + "WHERE s.name != 'Terminated' "
                    + "ORDER BY d.name, e.last_name";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                String currentDept = "";
                double deptTotal = 0;
                double companyTotal = 0;
                int empCount = 0;

                while (rs.next()) {
                    String dept = rs.getString("department");
                    if (!currentDept.equals(dept)) {
                        if (!currentDept.isEmpty()) {
                            report.append("Department Total: $").append(String.format("%.2f", deptTotal)).append("\n\n");
                        }
                        currentDept = dept != null ? dept : "Unassigned";
                        report.append("DEPARTMENT: ").append(currentDept).append("\n");
                        report.append("-".repeat(80)).append("\n");
                        deptTotal = 0;
                    }

                    String empName = rs.getString("first_name") + " " + rs.getString("last_name");
                    String title = rs.getString("title");
                    double salary = rs.getDouble("base_salary");
                    String payType = rs.getString("pay_type");
                    String status = rs.getString("status");

                    report.append(String.format("  %-30s %-20s %-15s $%-12.2f %s\n",
                            empName, title, payType != null ? payType : "Unknown", salary, status));

                    deptTotal += salary;
                    companyTotal += salary;
                    empCount++;
                }

                if (!currentDept.isEmpty()) {
                    report.append("Department Total: $").append(String.format("%.2f", deptTotal)).append("\n\n");
                }

                report.append("=".repeat(80)).append("\n");
                report.append("TOTAL EMPLOYEES: ").append(empCount).append("\n");
                report.append("COMPANY PAYROLL TOTAL: $").append(String.format("%.2f", companyTotal)).append("\n");
                report.append("AVERAGE SALARY: $").append(String.format("%.2f", empCount > 0 ? companyTotal / empCount : 0)).append("\n");
            }
        } catch (SQLException e) {
            report.append("Error generating payroll report: ").append(e.getMessage());
            e.printStackTrace();
        }
        return report.toString();
    }

    // Helper methods for adding/updating employees
    private static int getDepartmentId(Connection conn, String departmentName, String jobTitle) throws SQLException {
        if (departmentName != null && !departmentName.trim().isEmpty()) {
            String sql = "SELECT department_id FROM department WHERE name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, departmentName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("department_id");
                }
            }
        }

        if (jobTitle != null && !jobTitle.trim().isEmpty()) {
            String sql = "SELECT department_id FROM job WHERE title = ? LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, jobTitle);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("department_id");
                }
            }
        }

        return 1; // Default to first department if none found
    }

    private static int getJobId(Connection conn, String jobTitle, int deptId) throws SQLException {
        String sql = "SELECT job_id FROM job WHERE title = ? AND department_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, jobTitle);
            pstmt.setInt(2, deptId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("job_id");
            }
        }

        if (jobTitle != null && !jobTitle.trim().isEmpty()) {
            String fallbackSql = "SELECT job_id FROM job WHERE title = ? LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(fallbackSql)) {
                pstmt.setString(1, jobTitle);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("job_id");
                }
            }
        }

        return 1; // Default to first job if not found
    }

    private static int getStatusId(Connection conn, String statusName) throws SQLException {
        String sql = "SELECT id FROM employment_status WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, statusName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return 1; // Default to first status if not found
    }

    private static int getPayTypeId(Connection conn, String payTypeName) throws SQLException {
        String sql = "SELECT id FROM pay_type WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, payTypeName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return 1; // Default to first pay type if not found
    }

    private static int createOrUpdateAddress(Connection conn, int addressId, String line1, String line2, String city, String state, String zip) throws SQLException {
        if (addressId > 0) {
            // Update existing address
            String sql = "UPDATE address SET line1 = ?, line2 = ?, city = ?, state = ?, zip = ? WHERE address_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, line1);
                pstmt.setString(2, line2 != null && !line2.trim().isEmpty() ? line2 : null);
                pstmt.setString(3, city);
                pstmt.setString(4, state);
                pstmt.setString(5, zip);
                pstmt.setInt(6, addressId);
                pstmt.executeUpdate();
                return addressId;
            }
        } else {
            // Create new address
            String sql = "INSERT INTO address (line1, line2, city, state, zip) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, line1);
                pstmt.setString(2, line2 != null && !line2.trim().isEmpty() ? line2 : null);
                pstmt.setString(3, city);
                pstmt.setString(4, state);
                pstmt.setString(5, zip);
                pstmt.executeUpdate();
                
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        return 0; // Return 0 if address creation failed
    }
}
