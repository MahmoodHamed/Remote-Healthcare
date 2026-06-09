using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace RPM.Infrastructure.Persistence.Migrations;

public partial class AddStressBodyFatEcgFields : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.AddColumn<float>(
            name: "StressScore",
            table: "VitalRecords",
            type: "real",
            nullable: true);

        migrationBuilder.AddColumn<float>(
            name: "BodyFatPercent",
            table: "VitalRecords",
            type: "real",
            nullable: true);

        migrationBuilder.AddColumn<float>(
            name: "EcgAvgHeartRateBpm",
            table: "VitalRecords",
            type: "real",
            nullable: true);
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropColumn(name: "StressScore", table: "VitalRecords");
        migrationBuilder.DropColumn(name: "BodyFatPercent", table: "VitalRecords");
        migrationBuilder.DropColumn(name: "EcgAvgHeartRateBpm", table: "VitalRecords");
    }
}
