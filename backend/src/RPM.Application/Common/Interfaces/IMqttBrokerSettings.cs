namespace RPM.Application.Common.Interfaces;

/// <summary>Exposes MQTT broker settings to the Application layer without coupling it to IConfiguration.</summary>
public interface IMqttBrokerSettings
{
    string Host { get; }
    int Port { get; }
}
