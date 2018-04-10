FROM jfloff/alpine-python:2.7
MAINTAINER Simon Jupp "jupp@ebi.ac.uk"

RUN mkdir /app
WORKDIR /app
COPY *.py requirements.txt ./

RUN apk add --no-cache mariadb-dev build-base
#RUN apk --update add mysql mysql-client
RUN pip install -r requirements.txt

CMD bash