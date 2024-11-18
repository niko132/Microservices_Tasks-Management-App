# pull the official docker image
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE 1
ENV PYTHONUNBUFFERED 1

# copy project
COPY ./tasks .
COPY ./apiclient .

# install dependencies
RUN pip install -r requirements.txt