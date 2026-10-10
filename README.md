# ShesumaMart

A grocery-only e-commerce capstone project built with:

- Java Servlets
- JDBC
- Apache Tomcat 9
- Maven
- H2 database
- HikariCP
- jBCrypt
- Jackson
- HTML, CSS and JavaScript

## Roles

- Buyer
- Seller
- Admin (seed account only)

## Project structure

```text
src/main/java/com/shesuma/shesumamart/
├── controller
├── service
├── dao
├── model
├── dto
├── filter
├── listener
├── util
└── exception
```

## Run

```powershell
mvn clean package
```

Deploy `target/shesumamart.war` to Tomcat 9 `webapps`.

Application:
`http://localhost:8080/shesumamart/`

Health endpoint:
`http://localhost:8080/shesumamart/api/v1/health`

## Note

The supplied capstone guide specifies JDK 17 and Tomcat 9.x with `javax.servlet.*`.
This starter uses Java release 17 in Maven while it can be built on a newer installed JDK if compatible.
