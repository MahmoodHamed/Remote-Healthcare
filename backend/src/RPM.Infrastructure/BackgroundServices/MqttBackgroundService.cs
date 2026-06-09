using System.Threading.Channels;
using MediatR;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using MQTTnet;
using MQTTnet.Protocol;
using RPM.Application.DTOs.Vitals;
using RPM.Application.Features.Vitals.Commands;

namespace RPM.Infrastructure.BackgroundServices;

public class MqttBackgroundService(IConfiguration config, IMediator mediator, ILogger<MqttBackgroundService> logger)
    : BackgroundService
{
    private IMqttClient? _client;
    private readonly Channel<VitalIngestionDto> _queue = Channel.CreateBounded<VitalIngestionDto>(new BoundedChannelOptions(5000)
    {
        SingleReader = true,
        SingleWriter = false,
        FullMode = BoundedChannelFullMode.Wait
    });
    private CancellationToken _stoppingToken;

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _stoppingToken = stoppingToken;
        var factory = new MqttClientFactory();
        _client = factory.CreateMqttClient();

        var options = new MqttClientOptionsBuilder()
            .WithTcpServer(config["Mqtt:Host"] ?? "localhost", int.Parse(config["Mqtt:Port"] ?? "1883"))
            .WithClientId($"rpm-server-{Guid.NewGuid()}")
            .WithCleanStart()
            .Build();

        _client.ApplicationMessageReceivedAsync += OnMessageReceived;

        var consumerTask = ConsumeQueueAsync(stoppingToken);

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

        await consumerTask;
    }

    private async Task OnMessageReceived(MqttApplicationMessageReceivedEventArgs e)
    {
        try
        {
            var payload = e.ApplicationMessage.ConvertPayloadToString();
            logger.LogDebug("MQTT message on {Topic}: {Payload}", e.ApplicationMessage.Topic, payload);

            var dto = MqttVitalsParser.TryParse(payload);
            if (dto is null)
            {
                logger.LogWarning("Could not parse MQTT vitals payload on {Topic}", e.ApplicationMessage.Topic);
                return;
            }

            if (!_queue.Writer.TryWrite(dto))
                await _queue.Writer.WriteAsync(dto, _stoppingToken);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error processing MQTT vital message");
        }
    }

    private async Task ConsumeQueueAsync(CancellationToken stoppingToken)
    {
        await foreach (var reading in _queue.Reader.ReadAllAsync(stoppingToken))
        {
            // Process each MQTT message immediately (no batch delay).
            await mediator.Send(new IngestVitalsBatchCommand([reading]), stoppingToken);
        }
    }

    public override async Task StopAsync(CancellationToken ct)
    {
        if (_client?.IsConnected == true)
            await _client.DisconnectAsync(cancellationToken: ct);
        _queue.Writer.TryComplete();
        await base.StopAsync(ct);
    }
}
