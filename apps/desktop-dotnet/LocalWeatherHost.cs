using System.Net;
using System.Net.Http.Headers;
using System.Net.Sockets;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace ShizukuWeatherDesktop;

public sealed class LocalWeatherHost : IAsyncDisposable
{
    private static readonly HttpClient Http = CreateHttpClient();
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private static readonly Dictionary<int, WeatherCodeMeta> WeatherCodes = new()
    {
        [0] = new("mixed", "晴朗", "天空通透，能见度好"),
        [1] = new("mixed", "大部晴朗", "天空明亮，云量较少"),
        [2] = new("cloudy", "局部多云", "云层间歇覆盖"),
        [3] = new("cloudy", "阴天", "云层较厚，光照偏弱"),
        [45] = new("mist", "雾", "近地面湿雾扩散"),
        [48] = new("mist", "冻雾", "低温湿雾持续"),
        [51] = new("drizzle", "小毛雨", "零散毛毛雨"),
        [53] = new("drizzle", "毛毛雨", "路面有湿滑感"),
        [55] = new("drizzle", "较强毛毛雨", "持续性小雨"),
        [61] = new("rain", "小雨", "短时有降雨"),
        [63] = new("rain", "中雨", "降雨较稳定"),
        [65] = new("rain", "大雨", "雨势明显增强"),
        [71] = new("mist", "小雪", "冷湿气流活跃"),
        [80] = new("rain", "阵雨", "对流云团发展"),
        [81] = new("rain", "较强阵雨", "阵雨增强"),
        [82] = new("rain", "暴雨阵雨", "局地强降雨"),
        [95] = new("rain", "雷暴", "强对流活跃"),
    };

    private readonly HttpListener _listener;
    private readonly CancellationTokenSource _cts = new();
    private readonly string _webRoot;
    private readonly Task _listenLoop;
    private readonly LocalWeatherCache _cache;

    private LocalWeatherHost(HttpListener listener, Uri baseUri, string webRoot, LocalWeatherCache cache)
    {
        _listener = listener;
        BaseUri = baseUri;
        _webRoot = webRoot;
        _cache = cache;
        _listenLoop = Task.Run(ListenLoopAsync);
    }

    public Uri BaseUri { get; }

    public static async Task<LocalWeatherHost> StartAsync()
    {
        var port = GetAvailablePort();
        var listener = new HttpListener();
        listener.Prefixes.Add($"http://127.0.0.1:{port}/");
        listener.Start();

        var cache = new LocalWeatherCache();
        await cache.InitializeAsync(CancellationToken.None);

        var webRoot = ResolveWebRoot();
        return new LocalWeatherHost(listener, new Uri($"http://127.0.0.1:{port}/"), webRoot, cache);
    }

    public async ValueTask DisposeAsync()
    {
        _cts.Cancel();
        _listener.Stop();
        _listener.Close();

        try
        {
            await _listenLoop;
        }
        catch
        {
            // Listener shutdown.
        }

        _cts.Dispose();
    }

    private static HttpClient CreateHttpClient()
    {
        var handler = new HttpClientHandler
        {
            AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate | DecompressionMethods.Brotli,
        };
        var client = new HttpClient(handler);
        client.DefaultRequestHeaders.UserAgent.Add(new ProductInfoHeaderValue("ShizukuOpenWeatherDesktop", "1.0"));
        return client;
    }

    private static int GetAvailablePort()
    {
        using var tcp = new TcpListener(IPAddress.Loopback, 0);
        tcp.Start();
        var port = ((IPEndPoint)tcp.LocalEndpoint).Port;
        tcp.Stop();
        return port;
    }

    private static string ResolveWebRoot()
    {
        var bundledRoot = Path.Combine(AppContext.BaseDirectory, "web");
        if (File.Exists(Path.Combine(bundledRoot, "index.html")))
        {
            return bundledRoot;
        }

        return Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "web", "dist"));
    }

    private async Task ListenLoopAsync()
    {
        while (!_cts.IsCancellationRequested)
        {
            HttpListenerContext context;
            try
            {
                context = await _listener.GetContextAsync();
            }
            catch (ObjectDisposedException)
            {
                break;
            }
            catch (HttpListenerException)
            {
                break;
            }

            _ = Task.Run(() => HandleRequestAsync(context));
        }
    }

    private async Task HandleRequestAsync(HttpListenerContext context)
    {
        try
        {
            var request = context.Request;
            var response = context.Response;
            var path = request.Url?.AbsolutePath ?? "/";
            var apiConfig = ParseApiConfig(request);

            if (path.Equals("/api/locations/search", StringComparison.OrdinalIgnoreCase))
            {
                await HandleSearchAsync(request, response, apiConfig);
                return;
            }

            if (path.Equals("/api/weather/summary", StringComparison.OrdinalIgnoreCase))
            {
                await HandleSummaryAsync(request, response, apiConfig);
                return;
            }

            await ServeStaticAsync(path, response);
        }
        catch (Exception error)
        {
            await WriteJsonAsync(context.Response, new { error = error.Message }, HttpStatusCode.InternalServerError);
        }
    }

    private async Task HandleSearchAsync(HttpListenerRequest request, HttpListenerResponse response, ApiConfig apiConfig)
    {
        var query = request.QueryString["q"]?.Trim() ?? string.Empty;
        if (query.Length < 2)
        {
            await WriteJsonAsync(response, Array.Empty<LocationResult>());
            return;
        }

        var cacheKey = $"search:{query.ToLowerInvariant()}:{apiConfig.ProviderName}:{apiConfig.GeocodingUrl}";
        var cachedPayload = await _cache.GetAsync(cacheKey, _cts.Token);
        if (cachedPayload is not null)
        {
            await WriteJsonStringAsync(response, cachedPayload);
            return;
        }

        var payload = await SearchLocationResultsAsync(query, apiConfig, _cts.Token);
        var json = JsonSerializer.Serialize(payload, JsonOptions);
        await _cache.SetAsync(cacheKey, json, TimeSpan.FromHours(12), _cts.Token);
        await WriteJsonStringAsync(response, json);
    }

    private async Task HandleSummaryAsync(HttpListenerRequest request, HttpListenerResponse response, ApiConfig apiConfig)
    {
        var lat = ParseDouble(request.QueryString["lat"], 41.8057);
        var lon = ParseDouble(request.QueryString["lon"], 123.4315);
        var locationName = request.QueryString["locationName"];
        var regionName = request.QueryString["regionName"];
        var cacheKey = $"summary:{Math.Round(lat, 4):F4}:{Math.Round(lon, 4):F4}:{apiConfig.ProviderName}:{locationName}:{regionName}";

        var cachedPayload = await _cache.GetAsync(cacheKey, _cts.Token);
        if (cachedPayload is not null)
        {
            try
            {
                var cachedNode = JsonNode.Parse(cachedPayload)?.AsObject();
                if (cachedNode is not null)
                {
                    cachedNode["cached"] = true;
                    await WriteJsonStringAsync(response, cachedNode.ToJsonString(JsonOptions));
                    return;
                }
            }
            catch
            {
                await _cache.RemoveAsync(cacheKey, _cts.Token);
            }
        }

        var payload = await BuildWeatherSummaryAsync(lat, lon, locationName, regionName, apiConfig, _cts.Token);
        var json = payload.ToJsonString(JsonOptions);
        await _cache.SetAsync(cacheKey, json, TimeSpan.FromMinutes(30), _cts.Token);
        await WriteJsonStringAsync(response, json);
    }

    private async Task ServeStaticAsync(string requestPath, HttpListenerResponse response)
    {
        var relativePath = requestPath == "/" ? "index.html" : requestPath.TrimStart('/');
        relativePath = relativePath.Replace('/', Path.DirectorySeparatorChar);
        var candidate = Path.GetFullPath(Path.Combine(_webRoot, relativePath));

        if (!candidate.StartsWith(_webRoot, StringComparison.OrdinalIgnoreCase) || !File.Exists(candidate))
        {
            candidate = Path.Combine(_webRoot, "index.html");
        }

        if (!File.Exists(candidate))
        {
            response.StatusCode = (int)HttpStatusCode.NotFound;
            response.Close();
            return;
        }

        var bytes = await File.ReadAllBytesAsync(candidate, _cts.Token);
        response.StatusCode = (int)HttpStatusCode.OK;
        response.ContentType = GetContentType(Path.GetExtension(candidate));
        response.ContentLength64 = bytes.Length;
        await response.OutputStream.WriteAsync(bytes, _cts.Token);
        response.Close();
    }

    private static ApiConfig ParseApiConfig(HttpListenerRequest request)
    {
        return new ApiConfig(
            (request.QueryString["providerName"] ?? "高德定位 · 和风天气").Trim(),
            string.Equals(request.QueryString["useCustomApi"], "true", StringComparison.OrdinalIgnoreCase),
            (request.QueryString["geocodingUrl"] ?? "https://restapi.amap.com/v3/assistant/inputtips").Trim(),
            (request.QueryString["weatherUrl"] ?? "https://pw5egntnvw.re.qweatherapi.com").Trim(),
            (request.QueryString["airQualityUrl"] ?? string.Empty).Trim(),
            (request.QueryString["apiKey"] ?? string.Empty).Trim(),
            (request.QueryString["apiKeyParam"] ?? "key").Trim(),
            (request.QueryString["qweatherApiKey"] ?? "dcab85b9b77442d5a9375cc5d0ccaba1").Trim(),
            (request.QueryString["qweatherCredentialId"] ?? "KDGWMUR5WX").Trim());
    }

    private static string BuildUrl(string baseUrl, Dictionary<string, string> parameters, ApiConfig apiConfig)
    {
        var uri = new Uri(baseUrl);
        var existing = ParseQuery(uri.Query);
        foreach (var entry in parameters)
        {
            existing[entry.Key] = entry.Value;
        }

        if (!string.IsNullOrWhiteSpace(apiConfig.ApiKey))
        {
            existing[apiConfig.ApiKeyParam] = apiConfig.ApiKey;
        }

        var query = string.Join("&", existing.Select(entry => $"{Uri.EscapeDataString(entry.Key)}={Uri.EscapeDataString(entry.Value)}"));
        return $"{uri.GetLeftPart(UriPartial.Path)}?{query}";
    }

    private static string GetContentType(string extension) => extension.ToLowerInvariant() switch
    {
        ".html" => "text/html; charset=utf-8",
        ".js" => "text/javascript; charset=utf-8",
        ".css" => "text/css; charset=utf-8",
        ".json" => "application/json; charset=utf-8",
        ".svg" => "image/svg+xml",
        ".png" => "image/png",
        ".jpg" or ".jpeg" => "image/jpeg",
        ".ico" => "image/x-icon",
        _ => "application/octet-stream",
    };

    private static async Task WriteJsonAsync(HttpListenerResponse response, object payload, HttpStatusCode statusCode = HttpStatusCode.OK)
    {
        var json = JsonSerializer.Serialize(payload, JsonOptions);
        await WriteJsonStringAsync(response, json, statusCode);
    }

    private static async Task WriteJsonStringAsync(HttpListenerResponse response, string json, HttpStatusCode statusCode = HttpStatusCode.OK)
    {
        var bytes = JsonSerializer.SerializeToUtf8Bytes(JsonNode.Parse(json), JsonOptions);
        response.StatusCode = (int)statusCode;
        response.ContentType = "application/json; charset=utf-8";
        response.ContentLength64 = bytes.Length;
        await response.OutputStream.WriteAsync(bytes);
        response.Close();
    }

    private static async Task<LocationResult[]> SearchLocationResultsAsync(string query, ApiConfig apiConfig, CancellationToken cancellationToken)
    {
        var results = new List<LocationResult>();

        if (apiConfig.GeocodingUrl.Contains("amap.com", StringComparison.OrdinalIgnoreCase))
        {
            var geocodeUrl = BuildUrl("https://restapi.amap.com/v3/geocode/geo", new Dictionary<string, string>
            {
                ["address"] = query,
                ["output"] = "JSON",
            }, apiConfig);

            var geocodeData = await FetchJsonNodeAsync(geocodeUrl, cancellationToken);
            var primary = geocodeData?["geocodes"]?.AsArray()?.FirstOrDefault();
            var primaryLocation = primary?["location"]?.ToString();
            if (!string.IsNullOrWhiteSpace(primaryLocation))
            {
                var coords = ParseLocation(primaryLocation);
                var adcode = primary?["adcode"]?.ToString();
                var label = CleanLocationLabel(primary?["district"]?.ToString() ?? primary?["city"]?.ToString() ?? primary?["formatted_address"]?.ToString(), query);
                var subtitle = JoinParts(primary?["province"]?.ToString(), primary?["city"]?.ToString(), primary?["district"]?.ToString());
                results.Add(new LocationResult(BuildLocationKey(adcode, coords.Lat, coords.Lon, label), label, subtitle, coords.Lat, coords.Lon, adcode));
            }

            var tipsUrl = BuildUrl(apiConfig.GeocodingUrl, new Dictionary<string, string>
            {
                ["keywords"] = query,
                ["datatype"] = "all",
                ["citylimit"] = "false",
                ["output"] = "JSON",
            }, apiConfig);

            var tipData = await FetchJsonNodeAsync(tipsUrl, cancellationToken);
            var tips = tipData?["tips"]?.AsArray() ?? [];
            foreach (var item in tips)
            {
                if (item is null) continue;
                var coords = ParseLocation(item["location"]?.ToString());
                var adcode = item["adcode"]?.ToString();
                var label = item["name"]?.ToString() ?? query;
                if (string.IsNullOrWhiteSpace(label) || (coords.Lat == 0 && coords.Lon == 0 && string.IsNullOrWhiteSpace(adcode)))
                {
                    continue;
                }

                results.Add(new LocationResult(
                    BuildLocationKey(item["id"]?.ToString() ?? adcode, coords.Lat, coords.Lon, label),
                    label,
                    JoinParts(item["district"]?.ToString(), item["address"]?.ToString()),
                    coords.Lat,
                    coords.Lon,
                    adcode));
            }

            return DeduplicateLocations(results)
                .Where(item => !string.IsNullOrWhiteSpace(item.Label) && (item.Lat != 0 || item.Lon != 0 || !string.IsNullOrWhiteSpace(item.Adcode)))
                .Take(12)
                .ToArray();
        }

        var url = BuildUrl(apiConfig.GeocodingUrl, new Dictionary<string, string>
        {
            ["name"] = query,
            ["count"] = "12",
            ["language"] = "zh",
            ["format"] = "json",
        }, apiConfig);

        var root = await FetchJsonNodeAsync(url, cancellationToken);
        var fallback = root?["results"]?.AsArray() ?? [];
        return fallback
            .Select(item => item is null ? null : new LocationResult(
                BuildLocationKey(item["id"]?.ToString(), ParseDouble(item["latitude"]?.ToString(), 0d), ParseDouble(item["longitude"]?.ToString(), 0d), item["name"]?.ToString() ?? "未知地区"),
                item["name"]?.ToString() ?? "未知地区",
                NormalizeRegion(item),
                ParseDouble(item["latitude"]?.ToString(), 0d),
                ParseDouble(item["longitude"]?.ToString(), 0d),
                null))
            .Where(item => item is not null)
            .Cast<LocationResult>()
            .ToArray();
    }

    private static async Task<JsonObject> BuildWeatherSummaryAsync(double lat, double lon, string? locationName, string? regionName, ApiConfig apiConfig, CancellationToken cancellationToken)
    {
        try
        {
            var locationQuery = string.Create(System.Globalization.CultureInfo.InvariantCulture, $"{lon},{lat}");
            var nowUrl = BuildQWeatherUrl(apiConfig.WeatherUrl, "v7/weather/now", new Dictionary<string, string?>
            {
                ["location"] = locationQuery,
                ["lang"] = "zh",
            });
            var hourlyUrl = BuildQWeatherUrl(apiConfig.WeatherUrl, "v7/weather/24h", new Dictionary<string, string?>
            {
                ["location"] = locationQuery,
                ["lang"] = "zh",
            });
            var dailyUrl = BuildQWeatherUrl(apiConfig.WeatherUrl, "v7/weather/7d", new Dictionary<string, string?>
            {
                ["location"] = locationQuery,
                ["lang"] = "zh",
            });
            var airBaseUrl = string.IsNullOrWhiteSpace(apiConfig.AirQualityUrl) ? apiConfig.WeatherUrl : apiConfig.AirQualityUrl;
            var airUrl = BuildQWeatherUrl(airBaseUrl, $"airquality/v1/current/{lat.ToString(System.Globalization.CultureInfo.InvariantCulture)}/{lon.ToString(System.Globalization.CultureInfo.InvariantCulture)}", new Dictionary<string, string?>
            {
                ["lang"] = "zh",
            });

            var headers = QweatherHeaders(apiConfig);
            var nowTask = FetchJsonNodeAsync(nowUrl, cancellationToken, headers);
            var hourlyTask = FetchJsonNodeAsync(hourlyUrl, cancellationToken, headers);
            var dailyTask = FetchJsonNodeAsync(dailyUrl, cancellationToken, headers);
            var airTask = FetchJsonNodeAsync(airUrl, cancellationToken, headers);
            await Task.WhenAll(nowTask, hourlyTask, dailyTask, airTask);

            var warningAlerts = new List<AlertPayload>();
            try
            {
                var warningUrl = BuildQWeatherUrl(apiConfig.WeatherUrl, "v7/warning/now", new Dictionary<string, string?>
                {
                    ["location"] = locationQuery,
                    ["lang"] = "zh",
                });
                var warningData = await FetchJsonNodeAsync(warningUrl, cancellationToken, headers);
                var warningItems = warningData?["warning"]?.AsArray() ?? [];
                foreach (var item in warningItems)
                {
                    if (item is null) continue;
                    var title = item["title"]?.ToString() ?? item["text"]?.ToString() ?? "天气预警";
                    var typeName = item["typeName"]?.ToString() ?? title;
                    warningAlerts.Add(new AlertPayload(
                        item["id"]?.ToString() ?? $"warning-{warningAlerts.Count}",
                        title,
                        "warning",
                        typeName.Contains("风", StringComparison.Ordinal) ? "wind" : "rain",
                        item["text"]?.ToString() ?? typeName));
                }
            }
            catch
            {
                warningAlerts.Clear();
            }

            var current = nowTask.Result?["now"];
            var hourly = hourlyTask.Result?["hourly"]?.AsArray() ?? [];
            var daily = dailyTask.Result?["daily"]?.AsArray() ?? [];
            var airIndex = airTask.Result?["indexes"]?.AsArray()?.FirstOrDefault();
            var firstDay = daily.FirstOrDefault();

            var aqiValue = (int)Math.Round(ParseDouble(airIndex?["aqi"]?.ToString(), 0d));
            var fallbackAlerts = BuildAlerts(
                (int)Math.Round(ParseDouble(hourly.FirstOrDefault()?["pop"]?.ToString(), ParseDouble(firstDay?["precip"]?.ToString(), 0d) * 10d)),
                (int)Math.Round(ParseDouble(current?["icon"]?.ToString(), 0d)),
                (int)Math.Round(ParseDouble(current?["windSpeed"]?.ToString(), 0d)),
                (int)Math.Round(ParseDouble(firstDay?["windSpeedDay"]?.ToString(), ParseDouble(current?["windSpeed"]?.ToString(), 0d))),
                aqiValue);
            var activeAlerts = warningAlerts.Count > 0 ? warningAlerts.ToArray() : fallbackAlerts;
            var aqi = AqiMeta(aqiValue);

            var hourlyPayload = hourly.Take(5).Select(item => new JsonObject
            {
                ["hourLabel"] = FormatHourLabel(item?["fxTime"]?.ToString() ?? DateTimeOffset.UtcNow.ToString("O")),
                ["isoTime"] = item?["fxTime"]?.ToString() ?? DateTimeOffset.UtcNow.ToString("O"),
                ["temperature"] = ParseDouble(item?["temp"]?.ToString(), 0d),
                ["precipitationChance"] = (int)Math.Round(ParseDouble(item?["pop"]?.ToString(), 0d)),
                ["windSpeedKph"] = (int)Math.Round(ParseDouble(item?["windSpeed"]?.ToString(), 0d)),
                ["icon"] = MapQWeatherGlyph(item?["icon"]?.ToString(), item?["text"]?.ToString()),
                ["iconCode"] = item?["icon"]?.ToString() ?? "100",
                ["conditionLabel"] = item?["text"]?.ToString() ?? "天气平稳",
            }).ToArray();

            var dailyPayload = daily.Select(item =>
            {
                var day = FormatDaily(item?["fxDate"]?.ToString() ?? DateTime.Today.ToString("yyyy-MM-dd"));
                return new JsonObject
                {
                    ["dayLabel"] = day.DayLabel,
                    ["dateLabel"] = day.DateLabel,
                    ["isoDate"] = day.IsoDate,
                    ["highTemp"] = ParseDouble(item?["tempMax"]?.ToString(), 0d),
                    ["lowTemp"] = ParseDouble(item?["tempMin"]?.ToString(), 0d),
                    ["precipitationChance"] = (int)Math.Round(ParseDouble(item?["precip"]?.ToString(), 0d)),
                    ["windSpeedKph"] = (int)Math.Round(ParseDouble(item?["windSpeedDay"]?.ToString(), 0d)),
                    ["icon"] = MapQWeatherGlyph(item?["iconDay"]?.ToString(), item?["textDay"]?.ToString()),
                    ["iconCode"] = item?["iconDay"]?.ToString() ?? "100",
                    ["conditionLabel"] = string.Join(" / ", new[] { item?["textDay"]?.ToString(), item?["textNight"]?.ToString() }.Where(value => !string.IsNullOrWhiteSpace(value))),
                };
            }).ToArray();

            var currentText = current?["text"]?.ToString() ?? "天气更新中";
            var currentWind = current?["windSpeed"]?.ToString() ?? "--";
            var currentCloud = current?["cloud"]?.ToString() ?? "--";

            return new JsonObject
            {
                ["locationName"] = string.IsNullOrWhiteSpace(locationName) ? $"纬度 {lat:F2}" : locationName,
                ["regionName"] = string.IsNullOrWhiteSpace(regionName) ? $"经度 {lon:F2}" : regionName,
                ["latitude"] = lat,
                ["longitude"] = lon,
                ["currentTemp"] = ParseDouble(current?["temp"]?.ToString(), 0d),
                ["highTemp"] = ParseDouble(firstDay?["tempMax"]?.ToString(), ParseDouble(current?["temp"]?.ToString(), 0d)),
                ["lowTemp"] = ParseDouble(firstDay?["tempMin"]?.ToString(), ParseDouble(current?["temp"]?.ToString(), 0d)),
                ["feelsLikeTemp"] = ParseDouble(current?["feelsLike"]?.ToString(), ParseDouble(current?["temp"]?.ToString(), 0d)),
                ["humidityPercent"] = ParseDouble(current?["humidity"]?.ToString(), 0d),
                ["precipitationChance"] = (int)Math.Round(ParseDouble(hourly.FirstOrDefault()?["pop"]?.ToString(), 0d)),
                ["windSpeedKph"] = (int)Math.Round(ParseDouble(current?["windSpeed"]?.ToString(), 0d)),
                ["description"] = currentText,
                ["conditionLabel"] = string.Join("，", new[] { currentText, $"风速 {currentWind} km/h", $"云量 {currentCloud}%" }.Where(value => !string.IsNullOrWhiteSpace(value))),
                ["backgroundKey"] = MapQWeatherGlyph(current?["icon"]?.ToString(), currentText),
                ["currentIconCode"] = current?["icon"]?.ToString() ?? "100",
                ["source"] = string.IsNullOrWhiteSpace(apiConfig.ProviderName) ? "高德定位 · 和风天气" : apiConfig.ProviderName,
                ["cached"] = false,
                ["airQualityIndex"] = aqiValue,
                ["airQualityLabel"] = airIndex?["category"]?.ToString() ?? aqi.Label,
                ["airQualitySummary"] = airIndex?["health"]?["effect"]?.ToString() ?? "当前数据源已切到和风空气质量。",
                ["updatedAt"] = FormatUpdatedAt("Asia/Shanghai"),
                ["alerts"] = JsonSerializer.SerializeToNode(activeAlerts, JsonOptions),
                ["hourly"] = new JsonArray(hourlyPayload),
                ["daily"] = new JsonArray(dailyPayload),
            };
        }
        catch (Exception)
        {
            return BuildFallbackSummary(lat, lon, locationName, regionName);
        }
    }

    private static JsonObject BuildFallbackSummary(double lat, double lon, string? locationName, string? regionName)
    {
        var alerts = BuildAlerts(45, 53, 12, 18, 58);
        var dailyNodes = new JsonArray();
        for (var index = 0; index < 7; index++)
        {
            var date = DateTime.Today.AddDays(index);
            var day = FormatDaily(date.ToString("yyyy-MM-dd"));
            dailyNodes.Add(new JsonObject
            {
                ["dayLabel"] = day.DayLabel,
                ["dateLabel"] = day.DateLabel,
                ["isoDate"] = day.IsoDate,
                ["highTemp"] = 28 - (index % 3),
                ["lowTemp"] = 20 + (index % 2),
                ["precipitationChance"] = 40 + index * 4,
                ["windSpeedKph"] = 12 + index,
                ["icon"] = index < 3 ? "drizzle" : "cloudy",
                ["iconCode"] = index < 3 ? "305" : "104",
                ["conditionLabel"] = index < 3 ? "午后阵雨" : "云量偏多",
            });
        }

        var hourlyNodes = new JsonArray();
        for (var index = 0; index < 5; index++)
        {
            hourlyNodes.Add(new JsonObject
            {
                ["hourLabel"] = $"{16 + index:00}",
                ["isoTime"] = DateTimeOffset.UtcNow.AddHours(index).ToString("O"),
                ["temperature"] = 25 - index * 0.5,
                ["precipitationChance"] = 45 + index * 5,
                ["windSpeedKph"] = 12 + index,
                ["icon"] = index < 2 ? "cloudy" : "drizzle",
                ["iconCode"] = index < 2 ? "104" : "305",
                ["conditionLabel"] = index < 2 ? "云层覆盖" : "局地小雨",
            });
        }

        return new JsonObject
        {
            ["locationName"] = string.IsNullOrWhiteSpace(locationName) ? "已选地区" : locationName,
            ["regionName"] = string.IsNullOrWhiteSpace(regionName) ? "离线模式" : regionName,
            ["latitude"] = lat,
            ["longitude"] = lon,
            ["currentTemp"] = 25,
            ["highTemp"] = 28,
            ["lowTemp"] = 20,
            ["feelsLikeTemp"] = 26,
            ["humidityPercent"] = 70,
            ["precipitationChance"] = 45,
            ["windSpeedKph"] = 12,
            ["description"] = "天气平稳",
            ["conditionLabel"] = "桌面离线预览数据",
            ["backgroundKey"] = "drizzle",
            ["currentIconCode"] = "305",
            ["source"] = "fallback",
            ["cached"] = false,
            ["airQualityIndex"] = 58,
            ["airQualityLabel"] = "适宜",
            ["airQualitySummary"] = "当前处于离线状态，空气质量数据为示意值。",
            ["updatedAt"] = FormatUpdatedAt("Asia/Shanghai"),
            ["alerts"] = JsonSerializer.SerializeToNode(alerts, JsonOptions),
            ["hourly"] = hourlyNodes,
            ["daily"] = dailyNodes,
        };
    }

    private static WeatherCodeMeta CodeMeta(int code)
        => WeatherCodes.TryGetValue(code, out var meta) ? meta : new WeatherCodeMeta("mixed", "天气平稳", "局地天气变化不大");

    private static AlertPayload[] BuildAlerts(int precipitationChance, int currentWeatherCode, int currentWindKph, int maxWindKph, int aqiValue)
    {
        var alerts = new List<AlertPayload>();

        if (precipitationChance >= 80 || new[] { 65, 82, 95 }.Contains(currentWeatherCode))
        {
            alerts.Add(new AlertPayload("rain-warning", "暴雨预警", "warning", "rain", "未来 24 小时存在明显强降雨风险，建议尽量减少不必要出行并关注积水路段。"));
        }
        else if (precipitationChance >= 60)
        {
            alerts.Add(new AlertPayload("rain-watch", "强降雨关注", "watch", "rain", "降雨概率持续偏高，外出建议准备雨具并关注雷达走势。"));
        }

        if (maxWindKph >= 55 || currentWindKph >= 45)
        {
            alerts.Add(new AlertPayload("wind-warning", "大风预警", "warning", "wind", "风力较强，建议远离临时搭建物并减少高空、涉水等户外活动。"));
        }
        else if (maxWindKph >= 35 || currentWindKph >= 28)
        {
            alerts.Add(new AlertPayload("wind-watch", "阵风提醒", "watch", "wind", "阵风较明显，通勤和骑行时请注意侧风影响。"));
        }

        if (aqiValue >= 130)
        {
            alerts.Add(new AlertPayload("air-warning", "空气污染提醒", "warning", "air", "空气质量较差，敏感人群建议减少室外停留并佩戴防护口罩。"));
        }
        else if (aqiValue >= 95)
        {
            alerts.Add(new AlertPayload("air-watch", "空气质量关注", "info", "air", "空气质量略有波动，长时间户外活动前建议查看后续变化。"));
        }

        return alerts.ToArray();
    }

    private static AqiInfo AqiMeta(int aqi) => aqi switch
    {
        <= 50 => new AqiInfo("优", "空气质量优秀，适合长时间户外活动。"),
        <= 100 => new AqiInfo("适宜", "空气质量一般，敏感人群建议减少长时间室外停留。"),
        <= 150 => new AqiInfo("偏高", "空气中污染物有所累积，外出建议减少高强度运动。"),
        _ => new AqiInfo("较差", "空气质量不佳，建议优先待在室内并减少开窗时间。"),
    };

    private static string NormalizeRegion(JsonNode node)
    {
        return JoinParts(
            node["admin4"]?.ToString(),
            node["admin3"]?.ToString(),
            node["admin2"]?.ToString(),
            node["admin1"]?.ToString(),
            node["country"]?.ToString());
    }

    private static (double Lat, double Lon) ParseLocation(string? raw)
    {
        if (string.IsNullOrWhiteSpace(raw) || !raw.Contains(',', StringComparison.Ordinal))
        {
            return (0d, 0d);
        }

        var parts = raw.Split(',', 2, StringSplitOptions.TrimEntries);
        var lon = parts.Length > 0 ? ParseDouble(parts[0], 0d) : 0d;
        var lat = parts.Length > 1 ? ParseDouble(parts[1], 0d) : 0d;
        return (lat, lon);
    }

    private static string CleanLocationLabel(string? value, string fallback)
    {
        var label = string.IsNullOrWhiteSpace(value) ? fallback : value.Trim();
        return label.TrimEnd('市', '区', '县');
    }

    private static string JoinParts(params string?[] parts)
        => string.Join(" · ", parts.Where(part => !string.IsNullOrWhiteSpace(part)).Select(part => part!.Trim()).Distinct());

    private static string BuildLocationKey(string? preferredKey, double lat, double lon, string fallbackLabel)
        => !string.IsNullOrWhiteSpace(preferredKey) ? preferredKey! : $"{fallbackLabel}-{lat:F4}-{lon:F4}";

    private static LocationResult[] DeduplicateLocations(IEnumerable<LocationResult> items)
    {
        var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var results = new List<LocationResult>();
        foreach (var item in items)
        {
            var identity = $"{item.Label}|{item.Subtitle}|{item.Lat:F4}|{item.Lon:F4}";
            if (!seen.Add(identity)) continue;
            results.Add(item);
        }

        return results.ToArray();
    }

    private static string BuildQWeatherUrl(string baseUrl, string path, Dictionary<string, string?> parameters)
    {
        var normalizedBase = baseUrl.EndsWith("/", StringComparison.Ordinal) ? baseUrl : $"{baseUrl}/";
        var uri = new Uri(new Uri(normalizedBase), path);
        var query = ParseQuery(uri.Query);
        foreach (var entry in parameters)
        {
            if (!string.IsNullOrWhiteSpace(entry.Value))
            {
                query[entry.Key] = entry.Value!;
            }
        }

        if (query.Count == 0)
        {
            return uri.GetLeftPart(UriPartial.Path);
        }

        var queryString = string.Join("&", query.Select(entry => $"{Uri.EscapeDataString(entry.Key)}={Uri.EscapeDataString(entry.Value)}"));
        return $"{uri.GetLeftPart(UriPartial.Path)}?{queryString}";
    }

    private static Dictionary<string, string> QweatherHeaders(ApiConfig apiConfig)
    {
        var headers = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["X-QW-Api-Key"] = apiConfig.QweatherApiKey,
        };

        return headers;
    }

    private static string MapQWeatherGlyph(string? icon, string? text)
    {
        var code = (int)Math.Round(ParseDouble(icon, 0d));
        var label = text ?? string.Empty;

        if (new[] { 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 350, 351, 399 }.Contains(code)
            || label.Contains("雨", StringComparison.Ordinal)
            || label.Contains("雪", StringComparison.Ordinal)
            || label.Contains("雷", StringComparison.Ordinal))
        {
            return label.Contains("毛雨", StringComparison.Ordinal) || label.Contains("小雨", StringComparison.Ordinal) ? "drizzle" : "rain";
        }

        if (new[] { 500, 501, 502, 503, 504, 507, 508, 509, 510, 511, 512, 513, 514, 515 }.Contains(code)
            || label.Contains("雾", StringComparison.Ordinal)
            || label.Contains("霾", StringComparison.Ordinal)
            || label.Contains("浮尘", StringComparison.Ordinal)
            || label.Contains("沙", StringComparison.Ordinal))
        {
            return "mist";
        }

        if (new[] { 101, 102, 103, 104, 151, 152, 153, 154 }.Contains(code)
            || label.Contains("云", StringComparison.Ordinal)
            || label.Contains("阴", StringComparison.Ordinal))
        {
            return "cloudy";
        }

        return "mixed";
    }

    private static int FindCurrentHourIndex(JsonArray hourlyTimes)
    {
        for (var index = 0; index < hourlyTimes.Count; index++)
        {
            var text = hourlyTimes[index]?.GetValue<string>();
            if (DateTimeOffset.TryParse(text, out var point) && point >= DateTimeOffset.Now)
            {
                return index;
            }
        }

        return 0;
    }

    private static string FormatHourLabel(string iso)
        => DateTimeOffset.TryParse(iso, out var time) ? time.ToString("HH") : "--";

    private static DailyLabel FormatDaily(string iso)
    {
        var date = DateTime.TryParse(iso, out var parsed) ? parsed : DateTime.Today;
        var week = date.DayOfWeek switch
        {
            DayOfWeek.Sunday => "周日",
            DayOfWeek.Monday => "周一",
            DayOfWeek.Tuesday => "周二",
            DayOfWeek.Wednesday => "周三",
            DayOfWeek.Thursday => "周四",
            DayOfWeek.Friday => "周五",
            _ => "周六",
        };

        return new DailyLabel(week, $"{date.Day:00} {date.Month}月", date.ToString("yyyy-MM-dd"));
    }

    private static string FormatUpdatedAt(string? timezone)
    {
        var now = DateTimeOffset.Now;
        if (!string.IsNullOrWhiteSpace(timezone))
        {
            try
            {
                var zone = TimeZoneInfo.FindSystemTimeZoneById(timezone);
                now = TimeZoneInfo.ConvertTime(now, zone);
            }
            catch
            {
                // Ignore and use local time.
            }
        }

        return $"{now:HH:mm} 更新";
    }

    private static double ParseDouble(string? value, double fallback)
        => double.TryParse(value, out var parsed) ? parsed : fallback;

    private static Dictionary<string, string> ParseQuery(string query)
    {
        var values = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        if (string.IsNullOrWhiteSpace(query))
        {
            return values;
        }

        foreach (var segment in query.TrimStart('?').Split('&', StringSplitOptions.RemoveEmptyEntries))
        {
            var parts = segment.Split('=', 2);
            var key = Uri.UnescapeDataString(parts[0]);
            var value = parts.Length > 1 ? Uri.UnescapeDataString(parts[1]) : string.Empty;
            values[key] = value;
        }

        return values;
    }
    private static T GetArrayValue<T>(JsonNode? arrayNode, int index, T fallback)
    {
        if (arrayNode is not JsonArray array || index < 0 || index >= array.Count || array[index] is null)
        {
            return fallback;
        }

        try
        {
            return array[index]!.GetValue<T>();
        }
        catch
        {
            return fallback;
        }
    }

    private static async Task<JsonNode?> FetchJsonNodeAsync(string url, CancellationToken cancellationToken, IReadOnlyDictionary<string, string>? headers = null)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, url);
        if (headers is not null)
        {
            foreach (var header in headers)
            {
                request.Headers.Remove(header.Key);
                request.Headers.TryAddWithoutValidation(header.Key, header.Value);
            }
        }

        using var response = await Http.SendAsync(request, cancellationToken);
        response.EnsureSuccessStatusCode();
        var content = await response.Content.ReadAsStringAsync(cancellationToken);
        return JsonNode.Parse(content);
    }

    private sealed record WeatherCodeMeta(string Icon, string Description, string Condition);
    private sealed record AqiInfo(string Label, string Summary);
    private sealed record DailyLabel(string DayLabel, string DateLabel, string IsoDate);
    private sealed record LocationResult(string Key, string Label, string Subtitle, double Lat, double Lon, string? Adcode = null);
    private sealed record ApiConfig(string ProviderName, bool UseCustomApi, string GeocodingUrl, string WeatherUrl, string AirQualityUrl, string ApiKey, string ApiKeyParam, string QweatherApiKey, string QweatherCredentialId);
    private sealed record AlertPayload(string Id, string Title, string Severity, string Kind, string Detail);
}










