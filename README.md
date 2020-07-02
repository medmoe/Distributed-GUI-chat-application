# Movie Genres

The program consists of three parts. the first one is the server side which is responsible for controlling the flow of communications among users. The server generates threads in order to perform various tasks concurrently. additionally, the server is responsible for handling events that are received from the user. second part of the program is the client side. That’s the part where the user interacts with the application. it consists of certain UI/UX components implemented from Javafx library to generate action events and to facilitate the usage of the application. The third part of the program is the database which is integrated to the application using JDBC API and PostgreSQL. The database mainly stores login information and chat history of the users. 

## Installation

The files that should be compiled are located in Src folder. After you set your working directory to Src folder Compile the files using the command **javac filename.java** then run the server first using the command **java filename**.Then you can run the client-side.

### Dependencies

* JDK 1.8
* Javafx
* JDBC Driver
* PostgreSQL
* Terminal for command line access

### Folder Structure

* Code is saved into the Src folder.


