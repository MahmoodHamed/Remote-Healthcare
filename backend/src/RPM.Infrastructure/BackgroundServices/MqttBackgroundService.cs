using System.Text;
using System.Text.Json;
using System.Security.Cryptography;
using MediatR;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using MQTTnet;
using MQTTnet.Protocol;
using RPM.Application.Features.Vitals.Commands;

namespace RPM.Infrastructure.BackgroundServices;

public class MqttBackgroundService(IConfiguration config, IMediator mediator, ILogger<MqttBackgroundService> logger)
    : BackgroundService
{
    private IMqttClient? _client;

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var factory = new MqttClientFactory();
        _client = factory.CreateMqttClient();

        var options = new MqttClientOptionsBuilder()
            .WithTcpServer(config["Mqtt:Host"] ?? "localhost", int.Parse(config["Mqtt:Port"] ?? "1883"))
            .WithClientId($"rpm-server-{Guid.NewGuid()}")
            .WithCleanStart()
            .Build();

        _client.ApplicationMessageReceivedAsync += OnMessageReceived;

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                if (!_client.IsConnected)
                {
                    await _client.ConnectAsync(options, stoppingToken);
                    logger.LogInformation("MQTT connected");

                    var subscribeOptions = new MqttClientSubscribeOptionsBuilder()
                        .WithTopicFilter(f => f
                            .WithTopic("vitals/+/data")
                            .WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtLeastOnce))
                        .Build();

                    await _client.SubscribeAsync(subscribeOptions, stoppingToken);
                    logger.LogInformation("Subscribed to vitals/+/data");
                }
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "MQTT connection failed. Retrying in 5s...");
                await Task.Delay(5000, stoppingToken);
            }

            await Task.Delay(10000, stoppingToken);
        }
    }

    private async Task OnMessageReceived(MqttApplicationMessageReceivedEventArgs e)
    {
        try
        {
            var payload = Encoding.UTF8.GetString(e.ApplicationMessage.Payload.ToArray());
            logger.LogDebug("MQTT message on {Topic}: {Payload}", e.ApplicationMessage.Topic, payload);

            var data = JsonSerializer.Deserialize<MqttVitalsPayload>(payload,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

            if (data is null) return;

            var patientId = NormalizeGuid(data.PatientId);
            var deviceId = NormalizeGuid(data.DeviceId);

            if (patientId is null)
            {
                logger.LogWarning("Invalid patientId in MQTT payload: {PatientId}", data.PatientId);
                return;
            }

            if (deviceId is null)
            {
                logger.LogWarning("Invalid deviceId in MQTT payload: {DeviceId}", data.DeviceId);
                return;
            }

            var cmd = new IngestVitalCommand(
                patientId.Value, deviceId.Value,
                data.HeartRateBpm, data.SpO2Percent,
                data.SystolicBp, data.DiastolicBp,
                data.TemperatureC, data.StepsCount,
                data.CaloriesBurned, data.FallDetected, data.IsWearing);

            await mediator.Send(cmd);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error processing MQTT vital message");
        }
    }

    private static Guid? NormalizeGuid(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        if (Guid.TryParse(value, out var guid)) return guid;

        if (!IsShortId(value)) return null;

        var bytes = MD5.HashData(Encoding.UTF8.GetBytes(value));
        bytes[6] = (byte)((bytes[6] & 0x0f) | 0x30);
        bytes[8] = (byte)((bytes[8] & 0x3f) | 0x80);
        var guidString = FormatGuidString(bytes);
        return Guid.Parse(guidString);
    }

    private static bool IsShortId(string value)
    {
        if (value.Length != 6) return false;
        foreach (var ch in value)
        {
            var isDigit = ch >= '0' && ch <= '9';
            var isUpper = ch >= 'A' && ch <= 'Z';
            var isLower = ch >= 'a' && ch <= 'z';
            if (!(isDigit || isUpper || isLower)) return false;
        }
        return true;
    }

    private static string FormatGuidString(byte[] bytes) =>
        string.Create(36, bytes, (span, b) =>
        {
            var hex = "0123456789abcdef";
            var map = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
            int idx = 0;
            for (var i = 0; i < map.Length; i += 1)
            {
                if (idx == 8 || idx == 13 || idx == 18 || idx == 23)
                {
                    span[idx++] = '-';
                }
                var value = b[map[i]];
                span[idx++] = hex[value >> 4];
                span[idx++] = hex[value & 0x0f];
            }
        });

    public override async Task StopAsync(CancellationToken ct)
    {
        if (_client?.IsConnected == true)
            await _client.DisconnectAsync(cancellationToken: ct);
        await base.StopAsync(ct);
    }
}

public record MqttVitalsPayload(
    string PatientId, string DeviceId,
    float? HeartRateBpm, float? SpO2Percent,
    float? SystolicBp, float? DiastolicBp,
    float? TemperatureC, int? StepsCount,
    float? CaloriesBurned, bool FallDetected, bool IsWearing);
