using Amazon.S3;
using Amazon.S3.Transfer;
using Microsoft.Extensions.Configuration;
using RPM.Application.Common.Interfaces;
namespace RPM.Infrastructure.Services;

public class MinioStorageService : IStorageService
{
    private readonly IAmazonS3 _s3;
    private readonly string _bucket;

    public MinioStorageService(IConfiguration config)
    {
        var endpoint = config["Minio:Endpoint"] ?? "localhost:9000";
        var accessKey = config["Minio:AccessKey"] ?? "minioadmin";
        var secretKey = config["Minio:SecretKey"] ?? "minioadmin";
        _bucket = config["Minio:Bucket"] ?? "rpm-bucket";

        _s3 = new AmazonS3Client(accessKey, secretKey, new AmazonS3Config
        {
            ServiceURL = $"http://{endpoint}",
            ForcePathStyle = true
        });
    }

    public async Task<string> UploadAsync(Stream stream, string fileName, string contentType, CancellationToken ct = default)
    {
        var utility = new TransferUtility(_s3);
        await utility.UploadAsync(new TransferUtilityUploadRequest
        {
            InputStream = stream,
            BucketName = _bucket,
            Key = fileName,
            ContentType = contentType
        }, ct);
        return $"/{_bucket}/{fileName}";
    }

    public async Task DeleteAsync(string fileUrl, CancellationToken ct = default)
    {
        var key = fileUrl.TrimStart('/').Replace($"{_bucket}/", "");
        await _s3.DeleteObjectAsync(_bucket, key, ct);
    }
}
