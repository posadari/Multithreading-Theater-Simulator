# Multithreading Theater Simulator

## Summary
The project depicts a theater simulation where 50 customers go to a cinema, orders tickets, gets their ticket taken, possible go to concessions, and enter their corresponding movie theater. It utilizes threads to practice multithreading where threads are running concurrently. In this project, each customer represents a thread, two box office agents represent two threads, and one thread for each of the ticket taker and concession worker. 

## How to Use
- Command line to compile: javac Project2.java
- Command line to run: java Project2 movies.txt (or the name of input file)

## Additional Notes
Code has MAX_CUSTOMERS = 50 and parses the input file assuming it has the same format as Dr. Ozbirn's example, where the movie title and max capacity of seats are separated by a tab.