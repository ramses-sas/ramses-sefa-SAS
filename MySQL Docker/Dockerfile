FROM mysql:latest
RUN chown -R mysql:root /var/lib/mysql/
ENV MYSQL_ROOT_PASSWORD=root
COPY script.sql /docker-entrypoint-initdb.d/1_script.sql
COPY restaurantservice_dbdump.sql /docker-entrypoint-initdb.d/2_restaurantservice_dbdump.sql