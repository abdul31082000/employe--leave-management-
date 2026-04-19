# 🏢 Employee Leave Management System

A **RESTful backend application** built with **Java 17, Spring Boot 3, and MySQL** to manage employee leave requests with a full approval workflow. Includes comprehensive **JUnit 5 unit tests** covering 12+ test cases across business rules and edge cases.

---

## 📋 Features

- ✅ **Employee Management** — Create, update, deactivate employees with validation
- ✅ **Leave Application** — Apply for Annual, Sick, or Casual leave
- ✅ **Approval Workflow** — PENDING → APPROVED / REJECTED / CANCELLED
- ✅ **Business Rule Validation** — Balance checks, date validation, overlap detection
- ✅ **Automated Leave Deduction** — Balances updated on approval and restored on cancellation
- ✅ **JUnit 5 Tests** — 12 test cases covering happy paths and negative scenarios
- ✅ **Global Exception Handling** — Structured JSON error responses

---

## 🧪 Test Cases Covered

| Test ID | Scenario | Type |
|---------|----------|------|
| TC-001 | Apply annual leave successfully | Positive |
| TC-002 | Inactive employee cannot apply for leave | Negative |
| TC-003 | Start date in the past is rejected | Negative |
| TC-004 | End date before start date is rejected | Negative |
| TC-005 | Insufficient leave balance rejected | Negative |
| TC-006 | Overlapping leave dates rejected | Negative |
| TC-007 | Approve PENDING leave & deduct balance | Positive |
| TC-008 | Cannot approve already-approved leave | Negative |
| TC-009 | Reject leave with reviewer comment | Positive |
| TC-010 | Cancel approved leave restores balance | Positive |
| TC-011 | ResourceNotFoundException for invalid ID | Negative |
| TC-012 | Cannot cancel another employee's leave | Negative |

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Testing | JUnit 5, Mockito |
| Build Tool | Maven |
| API Style | REST |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- MySQL 8+
- Maven 3.8+

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/employee-leave-management.git
   cd employee-leave-management
   ```

2. **Configure database**

   Edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/leave_management_db
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Run tests**
   ```bash
   mvn test
   ```

---

## 📡 API Endpoints

### Employees
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/employees` | Create employee |
| GET | `/api/employees` | Get all employees |
| GET | `/api/employees/{id}` | Get employee by ID |
| PUT | `/api/employees/{id}` | Update employee |
| PATCH | `/api/employees/{id}/deactivate` | Deactivate employee |
| GET | `/api/employees/department/{dept}` | Filter by department |

### Leave Requests
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/leaves/apply/{employeeId}` | Apply for leave |
| GET | `/api/leaves/employee/{employeeId}` | Get all leaves for employee |
| GET | `/api/leaves/{id}` | Get leave by ID |
| GET | `/api/leaves?status=PENDING` | Filter by status |
| PATCH | `/api/leaves/{id}/approve` | Approve leave |
| PATCH | `/api/leaves/{id}/reject` | Reject leave |
| PATCH | `/api/leaves/{id}/cancel/{employeeId}` | Cancel leave |

---

## 📦 Sample API Requests

### Create Employee
```json
POST /api/employees
{
  "name": "Abdul Basith",
  "email": "basith@company.com",
  "department": "Quality Assurance",
  "role": "QA Engineer"
}
```

### Apply for Leave
```json
POST /api/leaves/apply/1
{
  "leaveType": "ANNUAL",
  "startDate": "2024-12-20",
  "endDate": "2024-12-25",
  "reason": "Year-end family vacation planned in advance"
}
```

### Approve Leave
```json
PATCH /api/leaves/1/approve
{
  "comment": "Approved. Ensure handover before departure."
}
```

---

## 🗂 Project Structure

```
src/
├── main/
│   ├── java/com/leavemanagement/
│   │   ├── controller/        # REST Controllers
│   │   ├── service/           # Business Logic
│   │   ├── repository/        # JPA Repositories
│   │   ├── model/             # JPA Entities
│   │   └── exception/         # Exception Handling
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/leavemanagement/
        └── service/           # JUnit 5 Unit Tests
```

---

## 👨‍💻 Author

**Abdul Basith**
- 📧 basithkasim3@gmail.com
- 🔗 [LinkedIn](https://linkedin.com/in/abdulbasith)

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
