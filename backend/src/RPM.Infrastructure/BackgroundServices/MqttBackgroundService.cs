using System.Buffers;
using System.Text;
using System.Text.Json;
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

            var cmd = new IngestVitalCommand(
                data.PatientId, data.DeviceId,
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

    public override async Task StopAsync(CancellationToken ct)
    {
        if (_client?.IsConnected == true)
            await _client.DisconnectAsync(cancellationToken: ct);
        await base.StopAsync(ct);
    }
}

public record MqttVitalsPayload(
    Guid PatientId, Guid DeviceId,
    float? HeartRateBpm, float? SpO2Percent,
    float? SystolicBp, float? DiastolicBp,
    float? TemperatureC, int? StepsCount,
    float? CaloriesBurned, bool FallDetected, bool IsWearing);
