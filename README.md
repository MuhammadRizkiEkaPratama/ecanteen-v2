# E-Canteen v2

Campus canteen ordering web application built for the J2EE course at Nanjing Xiaozhuang University.

**Stack:** Java 17, Spring Boot 3, MyBatis (SQL written by hand in XML mappers), Thymeleaf, MySQL, BCrypt login with optional LDAP.

**Architecture:** Controller -> Service -> Mapper -> Database, kept deliberately simple (no design patterns), following the course teaching style.

## Run locally

1. Create the database and tables: `schema.sql`.
2. Set the database password as an environment variable: `DB_PASSWORD`.
3. Start: `./mvnw spring-boot:run`, then open http://localhost:8080.

LDAP is off by default (`ecanteen.ldap.url` is empty). `deploy/` contains an example lab deployment (systemd service, DB user script, PowerShell deploy script) for a two-VM setup.

## Documentation

- `E-Canteen_v2_Project_Report.docx` - project report
- `E-Canteen_Architecture.drawio`, `E-Canteen_UseCaseDiagram.drawio` - diagrams
- `LAPORAN_BUG_ECANTEEN.docx`, `LAPORAN_BUG_RINGKAS_ECANTEEN.docx` - bug and security findings (Indonesian)
- `PRESENTASI_ECANTEEN.pptx`, `DEMO_PRESENTATION_SCRIPT.md` - presentation material

## Notes

Course project. Example credentials in `deploy/` are placeholders for a lab environment only.
