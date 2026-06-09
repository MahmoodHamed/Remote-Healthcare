namespace RPM.Application.Common.Interfaces;

/// <summary>Exposes MQTT broker settings to the Application layer without coupling it to IConfiguration.</summary>
public interface IMqttBrokerSettings
{
    /// <summary>Internal broker host (e.g. docker service name).</summary>
    string Host { get; }
    int Port { get; }
    /// <summary>Public host shown to watches (e.g. remote-care.tech). Falls back to <see cref="Host"/>.</summary>
    string PublicHost { get; }
}
