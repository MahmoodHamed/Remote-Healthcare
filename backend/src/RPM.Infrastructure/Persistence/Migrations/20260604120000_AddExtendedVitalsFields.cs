using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace RPM.Infrastructure.Persistence.Migrations;

public partial class AddExtendedVitalsFields : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.AddColumn<float>(
            name: "SkinTemperatureC",
            table: "VitalRecords",
            type: "real",
            nullable: true);

        migrationBuilder.AddColumn<float>(
            name: "AmbientTemperatureC",
            table: "VitalRecords",
            type: "real",
            nullable: true);

        migrationBuilder.AddColumn<float>(
            name: "HrvMs",
            table: "VitalRecords",
            type: "real",
            nullable: true);
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropColumn(name: "SkinTemperatureC", table: "VitalRecords");
        migrationBuilder.DropColumn(name: "AmbientTemperatureC", table: "VitalRecords");
        migrationBuilder.DropColumn(name: "HrvMs", table: "VitalRecords");
    }
}
