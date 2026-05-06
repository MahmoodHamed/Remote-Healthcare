using FluentValidation;
using RPM.Application.Features.Auth.Commands;
namespace RPM.Application.Features.Auth.Commands;
public class RegisterCommandValidator : AbstractValidator<RegisterCommand>
{
    public RegisterCommandValidator()
    {
        RuleFor(x => x.FullName).NotEmpty().MaximumLength(100);
        RuleFor(x => x.Email).NotEmpty().EmailAddress();
        RuleFor(x => x.Phone).NotEmpty().Matches(@"^\+?[0-9]{7,15}$");
        RuleFor(x => x.Password).NotEmpty().MinimumLength(8)
            .Matches("[A-Z]").WithMessage("Must have an uppercase letter.")
            .Matches("[0-9]").WithMessage("Must have a digit.");
        RuleFor(x => x.Role).NotEmpty().Must(r => r == "Doctor" || r == "Patient" || r == "Relative");
    }
}
public class LoginCommandValidator : AbstractValidator<LoginCommand>
{
    public LoginCommandValidator()
    {
        RuleFor(x => x.Email).NotEmpty().EmailAddress();
        RuleFor(x => x.Password).NotEmpty();
    }
}
