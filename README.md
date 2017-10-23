# Classroom Visualisation using Java Swing

This project is a simple application with GUI implemented in Java using the Swing framework and Socket programming. It consists of two class - 

1. ServerGui : The GUI is an m x n grid of panels which represents the seating arrangement of the class. The seats are numbered. Each panel contains an image of the student sitting there (if corresponding seat is occupied) or a stock photo. All images are stored in the ``./images/ `` folder as Roll_No.jpg. The ServerGui class also acts as a login portal , through which the students can login.

2. ClientGui : This class acts as a login client for the student. It takes the name, roll number, and seat no as input through a GUI window. On successful login, the student details along with their picture is updated on the corresponding seat in the Server GUI window.

To run this program, first compile both files

```javac ServerGui,java```

```javac ClientGui.java```

Thereafter start the server on the teacher's machine as -

```java ServerGui m1 m2 port n1 n2```

where,
* ``m1``: number of rows of grid representation of classroom 
* ``m2``: number of columns of grid representation of classroom
* ``port``: port at which the server should listen to
* ``n1``: size of threadpool to process login requests
* ``n2``: size of threadpool to use to load images

Once the server is running, the client may be started on the student machine as -

```java ClientGui server_ip server_port```
