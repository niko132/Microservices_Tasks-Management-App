# Tasks API Server

This repository contains the server implementation for our Tasks API.

## Run

To run the server make sure docker is installed and run the following command:

```
docker-compose up --build
```

This command forces docker to rebuild the images so that they are always up to date during development and then it launches 4 docker containers:

- reverse-proxy (traefik instance used for internal traffic routing)
- projects (a python flask server responsible for the /projects API)
- tasks (a python flask server responsible for the /tasks API)
- users (a python flask server responsible for the /users API)