using Microsoft.Data.Sqlite;

namespace ShizukuWeatherDesktop;

public sealed class LocalWeatherCache
{
    private readonly string _connectionString;
    private readonly SemaphoreSlim _gate = new(1, 1);

    public LocalWeatherCache()
    {
        var cacheDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "ShizukuOpenWeather",
            "cache");

        Directory.CreateDirectory(cacheDir);
        var dbPath = Path.Combine(cacheDir, "weather-cache.db");
        _connectionString = new SqliteConnectionStringBuilder
        {
            DataSource = dbPath,
            Mode = SqliteOpenMode.ReadWriteCreate,
            Cache = SqliteCacheMode.Private,
            Pooling = true,
            DefaultTimeout = 10,
        }.ToString();
    }

    public async Task InitializeAsync(CancellationToken cancellationToken)
    {
        await _gate.WaitAsync(cancellationToken);
        try
        {
            await using var connection = new SqliteConnection(_connectionString);
            await connection.OpenAsync(cancellationToken);

            var command = connection.CreateCommand();
            command.CommandText =
                """
                CREATE TABLE IF NOT EXISTS cache_entries (
                    cache_key TEXT PRIMARY KEY,
                    payload TEXT NOT NULL,
                    expires_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                );
                """;

            await command.ExecuteNonQueryAsync(cancellationToken);

            var pragma = connection.CreateCommand();
            pragma.CommandText = "PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;";
            await pragma.ExecuteNonQueryAsync(cancellationToken);
        }
        finally
        {
            _gate.Release();
        }
    }

    public async Task<string?> GetAsync(string key, CancellationToken cancellationToken)
    {
        await _gate.WaitAsync(cancellationToken);
        try
        {
            await using var connection = new SqliteConnection(_connectionString);
            await connection.OpenAsync(cancellationToken);

            var command = connection.CreateCommand();
            command.CommandText =
                """
                SELECT payload, expires_at
                FROM cache_entries
                WHERE cache_key = $key
                LIMIT 1;
                """;
            command.Parameters.AddWithValue("$key", key);

            await using var reader = await command.ExecuteReaderAsync(cancellationToken);
            if (!await reader.ReadAsync(cancellationToken))
            {
                return null;
            }

            if (!DateTimeOffset.TryParse(reader.GetString(1), out var expiresAt))
            {
                reader.Close();
                await RemoveCoreAsync(key, cancellationToken);
                return null;
            }

            if (expiresAt <= DateTimeOffset.UtcNow)
            {
                reader.Close();
                await RemoveCoreAsync(key, cancellationToken);
                return null;
            }

            return reader.GetString(0);
        }
        finally
        {
            _gate.Release();
        }
    }

    public async Task SetAsync(string key, string payload, TimeSpan ttl, CancellationToken cancellationToken)
    {
        await _gate.WaitAsync(cancellationToken);
        try
        {
            await using var connection = new SqliteConnection(_connectionString);
            await connection.OpenAsync(cancellationToken);

            var command = connection.CreateCommand();
            command.CommandText =
                """
                INSERT INTO cache_entries (cache_key, payload, expires_at, updated_at)
                VALUES ($key, $payload, $expires_at, $updated_at)
                ON CONFLICT(cache_key) DO UPDATE SET
                    payload = excluded.payload,
                    expires_at = excluded.expires_at,
                    updated_at = excluded.updated_at;
                """;

            var now = DateTimeOffset.UtcNow;
            command.Parameters.AddWithValue("$key", key);
            command.Parameters.AddWithValue("$payload", payload);
            command.Parameters.AddWithValue("$expires_at", now.Add(ttl).ToString("O"));
            command.Parameters.AddWithValue("$updated_at", now.ToString("O"));
            await command.ExecuteNonQueryAsync(cancellationToken);
        }
        finally
        {
            _gate.Release();
        }
    }

    public async Task RemoveAsync(string key, CancellationToken cancellationToken)
    {
        await _gate.WaitAsync(cancellationToken);
        try
        {
            await RemoveCoreAsync(key, cancellationToken);
        }
        finally
        {
            _gate.Release();
        }
    }

    private async Task RemoveCoreAsync(string key, CancellationToken cancellationToken)
    {
        await using var connection = new SqliteConnection(_connectionString);
        await connection.OpenAsync(cancellationToken);

        var command = connection.CreateCommand();
        command.CommandText = "DELETE FROM cache_entries WHERE cache_key = $key;";
        command.Parameters.AddWithValue("$key", key);
        await command.ExecuteNonQueryAsync(cancellationToken);
    }
}
