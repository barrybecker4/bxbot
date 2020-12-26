# Instructions

## To run:
 - ./gradlew clean build
 - ./gradlew buildTarGzipDist   (custom yaml files in config dir and will go in the bundle)
 - cd build/distributions
 - tar -xzf bxbot-app-1.0.1.tar.gz
 - cd bxbot-app-1.0.1
 - ./bxbot.sh start
 - For successive times do: ./gradlew clean build buildTarGzipDist
   
### Output:
 - Look in build/distributions/bxbox-app-1.0.1/logs/bxbot.log
 - To view record of transactions in db:
    - Look in the h2 db (see local application.properties for instructions how to view in browser)
    - H2 data persisted in ~/.h2
 
### Use REST API: http://localhost:8080/swagger-ui.html
 - Since the H2 is now persistent, only once, 
   you need to run the contents of import.sql in H2 console
   so that the admin and user credentials are in the database.
 - It used to be that this was loaded each time, but I commented it out in application.properties.
 - First authenticate by opening the "Authentication" section and enter
 ``` 
{
    "password": "****", // BCrypted pw. see keypass
    "username": "admin"
}
```
 - Take the result from that and paste it into "Authorize". 
   Then you can use any of the other REST API.
 
 ## Next Steps
 - run for real