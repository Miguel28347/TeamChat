TeamChat – Multi-Client Java Chat System (Console + GUI)

Author: Miguel Gonzalez

Technologies: Java, TCP Sockets, Multithreading, Swing, Concurrency, NetBeans/Ant

Repository: https://github.com/Miguel28347/TeamChat



Overview

TeamChat is a fully functional, multi-client Java chat system designed to demonstrate real networking, concurrency, and GUI design. It includes:

A multithreaded TCP server

A console-based chat client

A full Swing GUI client

Clients can join chat rooms, set usernames, and send private messages through a clean, scalable server architecture.


Features

✔️ Multithreaded Chat Server

One thread per client (ClientHandler)

Synchronized collections for safe concurrent access

ConcurrentHashMap for nickname → client mapping

✔️ Chat Rooms

Each client belongs to a “room” (default: lobby)

Commands:

/join roomName

✔️ User Nicknames

/nick yourName

Ensures fast lookups for private messages

✔️ Private Messaging

/w user message
or
/pm user message

Server-delivered messages without exposing user IPs

✔️ Two Client Interfaces
Console Client

Lightweight and minimal

Great for debugging or automation

Swing GUI Client

Connect/Disconnect buttons

Username/host/port inputs

Scrollable chat window

Thread-safe UI updates

Easy to extend for future features

Architecture
Server (ChatServer + ClientHandler)

The server listens on a TCP port (ServerSocket)

Each connection spawns a new ClientHandler thread

Shared state includes:

Set<ClientHandler> mClients – broadcasting within rooms

Map<String, ClientHandler> mClientsByNick – private messaging

Commands supported:

/nick

/join

/w

/pm

/quit

Clients
Console Client

Background thread prints server messages

Main thread reads from user input

Fully command-line compatible

Swing GUI Client

Built with Swing (JFrame, JButton, JTextArea, etc.)

Uses SwingUtilities.invokeLater() for safe UI updates

Separate thread for receiving messages

Clean, approachable interface

Running the Project
Prerequisites

Java 8+ (JDK)

NetBeans, IntelliJ, VS Code, or terminal

Clone/download the repo

1. Start the Server

Run:

java teamchat.ServerMain


Or in NetBeans:

Open ServerMain.java

Right-click → Run File

Output:

[SERVER] Listening on port 5000

<img width="3440" height="1440" alt="Screenshot 2025-11-29 024803" src="https://github.com/user-attachments/assets/02c786f7-43fb-4bf9-8b28-eaf407df46e7" />

2. Start the GUI Client

Open GUIClientMain.java
Run it → The GUI opens:

Enter host (default: localhost)

Port (default: 5000)

Username

<img width="3440" height="1380" alt="Screenshot 2025-11-29 025229" src="https://github.com/user-attachments/assets/ab9ed575-c34c-4ae4-a35e-c6b31842e7a3" />


Click Connect

3. Available Commands
Command	Description
/nick name	Set or change username
/join room	Switch chat rooms
/w user msg	Whisper / private message
/pm user msg	Same as /w
/quit	Disconnect

<img width="3440" height="1440" alt="Screenshot 2025-11-29 025834" src="https://github.com/user-attachments/assets/bd11085f-b022-4dec-b5e1-e30f51ca75b6" />


Motivation

This project was built to strengthen skills in:

Real-world TCP/IP communication

Concurrency and thread safety

Designing scalable server–client systems

Java Swing GUI development

Tech Highlights

This project demonstrates:

Thread-per-connection architecture

ConcurrentHashMap for real-time routing

Synchronized broadcast system

Non-blocking UI concurrency patterns

Modularized and extensible design

Full JavaDoc documentation across codebase

Future Improvements:

⭐ High-Value Additions

Message encryption (AES or RSA)

User authentication (login/password)

Persistent chat history (SQLite)

Typing indicators ("User is typing…")

Online user list in GUI

Multiple room tabs

⭐ "Stretch" Features

File transfers

Server admin commands

WebSocket version for future web UI

Adding even one of these will significantly elevate the project to top-tier undergrad work.


Developed by Miguel Gonzalez
