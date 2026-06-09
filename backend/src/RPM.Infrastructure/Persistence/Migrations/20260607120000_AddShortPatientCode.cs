using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace RPM.Infrastructure.Persistence.Migrations;

public partial class AddShortPatientCode : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.AddColumn<string>(
            name: "ShortPatientCode",
            table: "PatientProfiles",
            type: "character varying(6)",
            maxLength: 6,
            nullable: true);

        migrationBuilder.CreateIndex(
            name: "IX_PatientProfiles_ShortPatientCode",
            table: "PatientProfiles",
            column: "ShortPatientCode",
            unique: true);
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropIndex(name: "IX_PatientProfiles_ShortPatientCode", table: "PatientProfiles");
        migrationBuilder.DropColumn(name: "ShortPatientCode", table: "PatientProfiles");
    }
}
