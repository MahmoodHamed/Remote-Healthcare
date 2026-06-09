using System.Text;
using System.Text.Json;
using MediatR;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using MQTTnet;
using MQTTnet.Protocol;
using Microsoft.Extensions.DependencyInjection;
using RPM.Application.Common;
using RPM.Application.Features.Vitals.Commands;
using RPM.Domain.Interfaces;

namespace RPM.Infrastructure.BackgroundServices;

public class MqttBackgroundService(
    IConfiguration config,
    IMediator mediator,
    IServiceScopeFactory scopeFactory,
    ILogger<MqttBackgroundService> logger)
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
            var payload = e.ApplicationMessage.ConvertPayloadToString();
            logger.LogDebug("MQTT message on {Topic}: {Payload}", e.ApplicationMessage.Topic, payload);

            var data = JsonSerializer.Deserialize<MqttVitalsPayload>(payload,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

            if (data is null) return;

            var patientId = await ResolvePatientIdAsync(data.PatientId);
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
                data.TemperatureC, data.SkinTemperatureC, data.AmbientTemperatureC,
                data.HrvMs, data.StressScore, data.BodyFatPercent, data.EcgAvgHeartRateBpm,
                data.StepsCount, data.CaloriesBurned, data.FallDetected, data.IsWearing);

            await mediator.Send(cmd);
            logger.LogInformation(
                "Ingested vitals for patient {PatientId}: HR={HeartRate}, SpO2={SpO2}, Temp={Temp}",
                patientId.Value, data.HeartRateBpm, data.SpO2Percent, data.TemperatureC);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error processing MQTT vital message");
        }
    }

    private async Task<Guid?> ResolvePatientIdAsync(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        if (Guid.TryParse(value, out var guid)) return guid;

        if (!PatientShortCode.IsValidFormat(value)) return null;

        var code = PatientShortCode.Normalize(value);
        using var scope = scopeFactory.CreateScope();
        var uow = scope.ServiceProvider.GetRequiredService<IUnitOfWork>();
        var profile = await uow.Patients.GetByShortPatientCodeAsync(code);
        if (profile is not null) return profile.UserId;

        logger.LogWarning(
            "Watch code {Code} is not linked to any patient. Save pairing details in the mobile or web app first.",
            code);
        return null;
    }

    private static Guid? NormalizeGuid(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        return Guid.TryParse(value, out var guid) ? guid : null;
    }

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
    float? TemperatureC, float? SkinTemperatureC, float? AmbientTemperatureC,
    float? HrvMs, float? StressScore, float? BodyFatPercent, float? EcgAvgHeartRateBpm,
    int? StepsCount, float? CaloriesBurned, bool FallDetected, bool IsWearing);
