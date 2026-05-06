namespace RPM.Application.Common.Exceptions;
public class ValidationException : Exception
{
    public IDictionary<string, string[]> Errors { get; }
    public ValidationException(IEnumerable<FluentValidation.Results.ValidationFailure> failures)
        : base("One or more validation failures occurred.")
    {
        Errors = failures.GroupBy(e => e.PropertyName, e => e.ErrorMessage)
                         .ToDictionary(g => g.Key, g => g.ToArray());
    }
}
public class UnauthorizedException : Exception
{
    public UnauthorizedException(string message = "Unauthorized access.") : base(message) { }
}
public class ForbiddenException : Exception
{
    public ForbiddenException(string message = "Access forbidden.") : base(message) { }
}
public class ConflictException : Exception
{
    public ConflictException(string message) : base(message) { }
}
