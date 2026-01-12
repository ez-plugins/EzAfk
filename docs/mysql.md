# MySQL Integration

EzAfk supports MySQL for persistent storage of AFK data, player statistics, and plugin state. This allows for reliable data retention across server restarts and enables advanced features such as leaderboards and cross-server synchronization.

## Configuration Options
Edit the `mysql.yml` file in your EzAfk configuration folder to set up MySQL integration:

```
host: localhost
port: 3306
database: ezafk
username: your_username
password: your_password
useSSL: false
autoReconnect: true
maxPoolSize: 10
timeout: 3000
```

- **host**: MySQL server address
- **port**: MySQL server port (default: 3306)
- **database**: Database name for EzAfk tables
- **username**: MySQL user with access to the database
- **password**: Password for the MySQL user
- **useSSL**: Enable SSL connection (true/false)
- **autoReconnect**: Automatically reconnect if the connection drops
- **maxPoolSize**: Maximum number of connections in the pool
- **timeout**: Connection timeout in milliseconds

## Database Tables
EzAfk creates and manages the following tables:

### `afk_sessions`
Stores AFK session data for each player.
| Column         | Type         | Description                       |
| -------------- | ------------ | --------------------------------- |
| id             | INT          | Primary key                       |
| uuid           | VARCHAR(36)  | Player UUID                       |
| start_time     | BIGINT       | AFK session start timestamp (ms)  |
| end_time       | BIGINT       | AFK session end timestamp (ms)    |
| reason         | VARCHAR(32)  | AFK reason                        |
| detail         | TEXT         | Additional details                |

### `afk_leaderboard`
Stores total AFK time for leaderboard purposes.
| Column         | Type         | Description                       |
| -------------- | ------------ | --------------------------------- |
| uuid           | VARCHAR(36)  | Player UUID                       |
| total_afk_time | BIGINT       | Total AFK time in seconds         |

## Usage Notes
- MySQL integration is optional. If not configured, EzAfk will use local storage.
- Ensure your MySQL server is running and accessible from the Minecraft server.
- Use the `/afk top` command to view the AFK leaderboard, which uses MySQL data if enabled.
- For troubleshooting, check the server logs for MySQL connection errors.

For more details, see the [mysql.yml](mysql.yml) configuration file and EzAfk documentation.