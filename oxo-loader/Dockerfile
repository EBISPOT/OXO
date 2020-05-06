FROM python:3-alpine

ADD oxo-loader /opt/

RUN apk add --no-cache mariadb-dev build-base
#RUN apk --update add mysql mysql-client
RUN cd /opt/oxo-loader && pip install -r requirements.txt

CMD bash
