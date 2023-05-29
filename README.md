# Тестируем производительность разных пулеров на одной и той же задаче

### Задача:

Есть таблица:
```roomsql
create table test (id int primary key, name text);
insert into test values (0, 'a');
insert into test values (1, 'b');
```
будем выполнять в цикле 100000 раз запрос вида:
```roomsql
select id from test t0 where id = ?
```
увеличиваяя количество потоков (каждый поток выполняет одну и ту же задачу), <br/>а также будем
увеличивать вариативность текста запроса, путем случайного выбора алиаса таблицы "tn" в тексте запроса.<br/>
Результатом будет среднее время в секундах потраченное на выполнение задачи.

```java
java -jar projects/odyssey-multithread/target/odyssey-prepared-1.0-SNAPSHOT.jar "jdbc:postgresql://10.0.0.4:5432/postgres?user=test&password=test" 1 1 100000
java -jar projects/odyssey-multithread/target/odyssey-prepared-1.0-SNAPSHOT.jar "jdbc:postgresql://10.0.0.4:5432/postgres?user=test&password=test" 60 1 100000
java -jar projects/odyssey-multithread/target/odyssey-prepared-1.0-SNAPSHOT.jar "jdbc:postgresql://10.0.0.4:5432/postgres?user=test&password=test" 60 60 100000
```

---

### Результаты тестов

| Threads<br/>(Sessions) | Number Of<br/>Unique <br/>Statements | Postgresql<br/>5432 | PgBouncer<br/>transaction<br/>6432 | PgBouncer<br/>session<br/>7432 | Odyssey<br/>transaction<br/>8432 |
|:----------------------:|:------------------------------------:|:-------------------:|:----------------------------------:|:------------------------------:|:--------------------------------:|
|           1            |                  1                   |         32          |                127                 |               87               |               101                |
|           60           |                  1                   |         325         |                1255                |              850               |               3540               |
|           60           |                  60                  |         283         |                                    |                                |                                  |
---

### Настройки пулеров

**PgBouncer transaction**

```
[databases]

* = host=127.0.0.1 port=5432 auth_user=pgbouncer

[pgbouncer]
logfile = /var/log/pgbouncer/pgbouncer.log
pidfile = /var/run/pgbouncer/pgbouncer-t.pid
listen_addr = *
listen_port = 6432
unix_socket_dir = /tmp
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt
auth_query = SELECT usename, passwd FROM pg_shadow WHERE usename=$1
admin_users = pgbouncer
stats_users = okagent, stats, postgres
pool_mode = transaction
server_reset_query = DISCARD ALL
ignore_startup_parameters = extra_float_digits,search_path,options,client_min_messages,statement_timeout
max_client_conn = 6000
default_pool_size = 200
reserve_pool_size = 30
reserve_pool_timeout = 3
max_db_connections = 200
query_wait_timeout = 300
```

**PgBouncer session**

```
[databases]

* = host=127.0.0.1 port=5432 auth_user=pgbouncer

[pgbouncer]
logfile = /var/log/pgbouncer/pgbouncer.log
pidfile = /var/run/pgbouncer/pgbouncer-s.pid
listen_addr = *
listen_port = 7432
unix_socket_dir = /tmp
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt
auth_query = SELECT usename, passwd FROM pg_shadow WHERE usename=$1
admin_users = pgbouncer
stats_users = okagent, stats, postgres
pool_mode = session
server_reset_query = DISCARD ALL
ignore_startup_parameters = extra_float_digits,search_path,options,client_min_messages,statement_timeout
max_client_conn = 6000
default_pool_size = 200
reserve_pool_size = 30
reserve_pool_timeout = 3
max_db_connections = 200
query_wait_timeout = 300
```
**Odyssey transaction**

```
daemonize yes
pid_file "/tmp/odyssey.pid"
unix_socket_dir "/tmp"
unix_socket_mode "0644"
locks_dir "/tmp/odyssey"
graceful_die_on_errors no
enable_online_restart no
bindwith_reuseport no

log_file "/tmp/odyssey.log"
log_format "%p %t %l [%i %s] (%c) %m\n"
log_to_stdout no
log_syslog no
log_syslog_ident "odyssey"
log_syslog_facility "daemon"
log_debug no
log_session no
log_query no
log_stats no
stats_interval 0
log_general_stats_prom no
log_route_stats_prom no

workers 1
resolvers 1
readahead 8192
cache_coroutine 0
coroutine_stack_size 8
nodelay yes
keepalive 15
keepalive_keep_interval 75
keepalive_probes 9
keepalive_usr_timeout 0

listen {
	host "*"
	port 8432
	backlog 128
	tls "disable"
	compression no
}

storage "postgres_server" {
	type "remote"
	host "127.0.0.1"
	port 5432
}

database "postgres" {
        user "odyssey" {
                authentication "none"
                pool_routing "internal"
                storage "postgres_server"
                pool "session"
        }
}

database default {
	user default {
		authentication "md5"
		auth_query "SELECT usename, passwd FROM pg_shadow WHERE usename=$1"
		auth_query_user "odyssey"
		auth_query_db "postgres"
		storage "postgres_server"
		pool "transaction"
		pool_discard no
		pool_smart_discard yes
		pool_reserve_prepared_statement yes
	}
}

storage "local" {
	type "local"
}

database "console" {
	user default {
		authentication "none"
		role "admin"
		pool "session"
		storage "local"
	}
}
```

**DB HOST**

```shell
$ lscpu

Architecture:        x86_64
CPU op-mode(s):      32-bit, 64-bit
Byte Order:          Little Endian
CPU(s):              64
On-line CPU(s) list: 0-63
Thread(s) per core:  2
Core(s) per socket:  8
Socket(s):           4
NUMA node(s):        4
Vendor ID:           GenuineIntel
CPU family:          6
Model:               46
Model name:          Intel(R) Xeon(R) CPU           X7560  @ 2.27GHz
Stepping:            6
CPU MHz:             1769.937
CPU max MHz:         2266.0000
CPU min MHz:         1064.0000
BogoMIPS:            4528.42
Virtualization:      VT-x
L1d cache:           32K
L1i cache:           32K
L2 cache:            256K
L3 cache:            24576K
NUMA node0 CPU(s):   0-7,32-39
NUMA node1 CPU(s):   8-15,40-47
NUMA node2 CPU(s):   16-23,48-55
NUMA node3 CPU(s):   24-31,56-63
```

---