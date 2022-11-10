package myproject;

import com.pulumi.Pulumi;
import com.pulumi.aws.ecr.Repository;
import com.pulumi.aws.iam.*;
import com.pulumi.aws.iam.enums.ManagedPolicy;
import com.pulumi.aws.lambda.Function;
import com.pulumi.aws.lambda.FunctionArgs;
import com.pulumi.aws.lambda.Permission;
import com.pulumi.aws.lambda.PermissionArgs;
import com.pulumi.aws.s3.*;
import com.pulumi.aws.s3.inputs.BucketNotificationLambdaFunctionArgs;
import com.pulumi.aws.s3.inputs.BucketWebsiteArgs;
import com.pulumi.awsx.ecr.Image;
import com.pulumi.awsx.ecr.ImageArgs;
import com.pulumi.core.Output;
import com.pulumi.resources.CustomResourceOptions;

public class App3 {

    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            var repo = new Repository("thumbnailer");
            var image = new Image("thumbnailer", ImageArgs.builder()
                    .repositoryUrl(repo.repositoryUrl())
                    .path("./app")
                    .build());

            var bucket = new Bucket("thumbnailer", BucketArgs.builder()
                    .forceDestroy(true)
                    .website(BucketWebsiteArgs.builder().indexDocument("cat.jpg").build())
                    .build());

            var bucketHttpAccess = new BucketPolicy("allowHttpAccess", BucketPolicyArgs.builder()
                    .bucket(bucket.getId())
                    .policy(Output.format("""
                            {
                              "Version": "2012-10-17",
                              "Statement": [
                                  {
                                      "Effect": "Allow",
                                      "Principal": "*",
                                      "Action": "s3:GetObject",
                                      "Resource": "%s/*"
                                  }
                              ]
                            }""", bucket.arn()))
                    .build());

            var role = new Role("thumbnailerRole", RoleArgs.builder()
                    .assumeRolePolicy("""
                            {
                              "Version": "2012-10-17",
                              "Statement": [
                                {
                                  "Effect": "Allow",
                                  "Principal": {
                                    "Service": "lambda.amazonaws.com"
                                  },
                                  "Action": "sts:AssumeRole"
                                }
                              ]
                            }""")
                    .build()
            );

            var lambdaFullAccess = new RolePolicyAttachment("lambdaFullAccess", RolePolicyAttachmentArgs.builder()
                    .role(role.name())
                    .policyArn(ManagedPolicy.LambdaFullAccess.getValue())
                    .build());

            var lambdaBasicExecutionRole = new RolePolicyAttachment("basicExecutionRole", RolePolicyAttachmentArgs.builder()
                    .role(role.name())
                    .policyArn(ManagedPolicy.AWSLambdaBasicExecutionRole.getValue())
                    .build());

            var s3AccessPolicy = new Policy("lambdaS3Access", PolicyArgs.builder()
                    .policy(bucket.arn().applyValue(buketArn -> """
                            {
                              "Version": "2012-10-17",
                              "Statement": [
                                {
                                  "Effect": "Allow",
                                  "Action": "s3:*",
                                  "Resource": ["%s", "%s/*"]
                                }
                              ]
                            }""".formatted(buketArn, buketArn)))
                    .build());

            var s3Access = new RolePolicyAttachment("lambdaS3Access", RolePolicyAttachmentArgs.builder()
                    .role(role.name())
                    .policyArn(s3AccessPolicy.arn())
                    .build());

            var thumbnailer = new Function("thumbnailer", FunctionArgs.builder()
                    .packageType("Image")
                    .imageUri(image.imageUri())
                    .role(role.arn())
                    .timeout(900)
                    .build());

            var bucketExecutionAllowance = new Permission("bucketExecutionAllowance", PermissionArgs.builder()
                    .function(thumbnailer.arn())
                    .action("lambda:InvokeFunction")
                    .principal("s3.amazonaws.com")
                    .sourceArn(bucket.arn())
                    .build());

            var runFfmpeg = new BucketNotification("onNewVideo",
                    BucketNotificationArgs.builder()
                            .bucket(bucket.getId())
                            .lambdaFunctions(
                                    BucketNotificationLambdaFunctionArgs.builder()
                                            .events("s3:ObjectCreated:*")
                                            .filterSuffix(".mp4")
                                            .lambdaFunctionArn(thumbnailer.arn())
                                            .build()
                            )
                            .build(),

                    CustomResourceOptions.builder()
                            .dependsOn(bucketExecutionAllowance)
                            .build()
            );

            ctx.export("bucketName", bucket.getId());
            ctx.export("url", bucket.websiteEndpoint());
        });
    }
}