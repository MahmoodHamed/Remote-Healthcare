using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace RPM.Infrastructure.Persistence.Migrations
{
    /// <inheritdoc />
    public partial class SyncPendingModel : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // Samsung Watch 8 vitals columns
            migrationBuilder.AddColumn<float>(name: "SkinTemperatureC", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "HeartRateVariabilityMs", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "RestingHeartRateBpm", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "MaxHeartRateBpm", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "RespirationRateBpm", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "DistanceMeters", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<int>(name: "FloorsClimbed", table: "VitalRecords", type: "integer", nullable: true);
            migrationBuilder.AddColumn<int>(name: "ActiveMinutes", table: "VitalRecords", type: "integer", nullable: true);
            migrationBuilder.AddColumn<float>(name: "StressScore", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "SleepScore", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<int>(name: "SleepDurationMinutes", table: "VitalRecords", type: "integer", nullable: true);
            migrationBuilder.AddColumn<float>(name: "BodyFatPercent", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "MuscleMassKg", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "BodyWaterPercent", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "BasalMetabolicRate", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "EcgAverageHeartRate", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<string>(name: "EcgClassification", table: "VitalRecords", type: "text", nullable: true);
            migrationBuilder.AddColumn<string>(name: "EcgWaveformJson", table: "VitalRecords", type: "text", nullable: true);
            migrationBuilder.AddColumn<float>(name: "BloodGlucoseMgDl", table: "VitalRecords", type: "real", nullable: true);
            migrationBuilder.AddColumn<float>(name: "BatteryLevel", table: "VitalRecords", type: "real", nullable: true);

            migrationBuilder.AddColumn<float>(name: "MaxSkinTemperatureC", table: "AlertThresholds", type: "real", nullable: false, defaultValue: 38.0f);
            migrationBuilder.AddColumn<float>(name: "MinRespirationRate", table: "AlertThresholds", type: "real", nullable: false, defaultValue: 8.0f);
            migrationBuilder.AddColumn<float>(name: "MaxRespirationRate", table: "AlertThresholds", type: "real", nullable: false, defaultValue: 24.0f);
            migrationBuilder.AddColumn<float>(name: "MaxStressScore", table: "AlertThresholds", type: "real", nullable: false, defaultValue: 80.0f);
            migrationBuilder.AddColumn<float>(name: "MinBloodGlucoseMgDl", table: "AlertThresholds", type: "real", nullable: false, defaultValue: 70.0f);
            migrationBuilder.AddColumn<float>(name: "MaxBloodGlucoseMgDl", table: "AlertThresholds", type: "real", nullable: false, defaultValue: 180.0f);

            migrationBuilder.DropIndex(
                name: "IX_Notifications_UserId",
                table: "Notifications");

            migrationBuilder.DropIndex(
                name: "IX_DoctorPatientAssignments_DoctorId",
                table: "DoctorPatientAssignments");

            migrationBuilder.CreateIndex(
                name: "IX_Notifications_UserId_IsRead_SentAt",
                table: "Notifications",
                columns: new[] { "UserId", "IsRead", "SentAt" });

            migrationBuilder.CreateIndex(
                name: "IX_DoctorPatientAssignments_DoctorId_PatientId",
                table: "DoctorPatientAssignments",
                columns: new[] { "DoctorId", "PatientId" },
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(name: "IX_DoctorPatientAssignments_DoctorId_PatientId", table: "DoctorPatientAssignments");
            migrationBuilder.DropIndex(name: "IX_Notifications_UserId_IsRead_SentAt", table: "Notifications");

            migrationBuilder.CreateIndex(
                name: "IX_Notifications_UserId",
                table: "Notifications",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_DoctorPatientAssignments_DoctorId",
                table: "DoctorPatientAssignments",
                column: "DoctorId");

            migrationBuilder.DropColumn(name: "MaxSkinTemperatureC", table: "AlertThresholds");
            migrationBuilder.DropColumn(name: "MinRespirationRate", table: "AlertThresholds");
            migrationBuilder.DropColumn(name: "MaxRespirationRate", table: "AlertThresholds");
            migrationBuilder.DropColumn(name: "MaxStressScore", table: "AlertThresholds");
            migrationBuilder.DropColumn(name: "MinBloodGlucoseMgDl", table: "AlertThresholds");
            migrationBuilder.DropColumn(name: "MaxBloodGlucoseMgDl", table: "AlertThresholds");

            migrationBuilder.DropColumn(name: "SkinTemperatureC", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "HeartRateVariabilityMs", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "RestingHeartRateBpm", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "MaxHeartRateBpm", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "RespirationRateBpm", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "DistanceMeters", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "FloorsClimbed", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "ActiveMinutes", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "StressScore", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "SleepScore", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "SleepDurationMinutes", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "BodyFatPercent", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "MuscleMassKg", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "BodyWaterPercent", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "BasalMetabolicRate", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "EcgAverageHeartRate", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "EcgClassification", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "EcgWaveformJson", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "BloodGlucoseMgDl", table: "VitalRecords");
            migrationBuilder.DropColumn(name: "BatteryLevel", table: "VitalRecords");
        }
    }
}
