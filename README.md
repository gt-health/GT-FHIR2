GT-FHIR2 is moving to https://github.com/omoponfhir organization site. This repository will no longer have any updates. New updates will be made to the omoponfhir organization. 
=

GT-FHIR2: Georgia Tech - OmopOnFHIR Implementation version 2
-
- Supports OMOP v5
- Supports STU3
- [WIP] Supports DSTU2 -- currently work in progress. omoponfhir-dstu2-server/ is just copy of omoponfhir-stu3-server. When it's completed, pom and dockerfile will include this directory.

Database Dependencies
-
This application requires an OMOP V5 database to work. You can use the database here: https://github.com/gt-health/smart-platform-docker/tree/hdap-devops/smart-postgresql
1. First clone the `hdap-devops` branch of the `smart-platform-docker project`
2. Navigate to the `smart-postgres` directory of the project.
   ```
     cd smart-postgres
   ```
3. Look at the docker-compose.yml file and adjust configurations as needed. By default the database is configured to run on port 5438. Use docker-compose to start the container.
   ```
   sudo docker-compose up --build -d
   ```
4. The OMOP V5 database is not running in a container.

How to install and run.
-
Docker Compose is used to create a container to run the GT-FHIR2 application. Before running the application
update the values of the JDBC_URL, JDBC_USERNAME, and JDBC_PASSWORD environment variables in the Dockerfile
They must contain the data necessary for your application to connect to an OMOP V5 database.
After updating the ENV variables in the Dockerfile start the application
```
sudo docker-compose up --build -d
```
NOTE: The GT-FHIR2 is set to READ-ONLY. If you want to write to GT-FHIR2, web.xml in WEB-INFO must have readOnly set to False.

Application URLs
-
- UI - http://<my_host>:8080/gt-fhir/tester/
- API -  	http://<my_host>:8080/gt-fhir/fhir

Additional Information
-
Status: Details available in http://omoponfhir.org/

Contributors:
- Lead: Myung Choi
- DevOp/Deployment: Eric Soto
- Docker/VM Management: Michael Riley
 
Please use Issues to report any problems. If you are willing to contribute, please fork this repository and do the pull requests.
