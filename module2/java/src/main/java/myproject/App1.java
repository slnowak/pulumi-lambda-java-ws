package myproject;

import com.pulumi.Pulumi;
import com.pulumi.asset.AssetArchive;
import com.pulumi.asset.StringAsset;
import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.RolePolicyAttachment;
import com.pulumi.aws.iam.RolePolicyAttachmentArgs;
import com.pulumi.aws.iam.enums.ManagedPolicy;
import com.pulumi.aws.lambda.Function;
import com.pulumi.aws.lambda.FunctionArgs;
import com.pulumi.aws.lambda.enums.Runtime;
import com.pulumi.awsapigateway.RestAPI;
import com.pulumi.awsapigateway.RestAPIArgs;
import com.pulumi.awsapigateway.enums.Method;
import com.pulumi.awsapigateway.inputs.RouteArgs;

import java.util.Map;

public class App1 {

    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            var role = new Role("lambdaRole", RoleArgs.builder()
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

            var lambdaBasicExecutionRole = new RolePolicyAttachment("basicExecutionRole", RolePolicyAttachmentArgs.builder()
                    .role(role.name())
                    .policyArn(ManagedPolicy.AWSLambdaBasicExecutionRole.getValue())
                    .build());

            var api = new RestAPI("helloWorldApi", RestAPIArgs.builder()
                    .routes(
                            RouteArgs.builder()
                                    .path("/")
                                    .method(Method.GET)
                                    .eventHandler(
                                            new Function("helloWorldFunction", FunctionArgs.builder()
                                                    .role(role.arn())
                                                    .handler("index.handler")
                                                    .runtime(Runtime.NodeJS12dX)
                                                    .code(new AssetArchive(Map.of("index.js", new StringAsset("""
                                                            exports.handler = async (event) => {
                                                                return {
                                                                    statusCode: 200,
                                                                    body: "Hello, world!",
                                                                };
                                                            }"""))))
                                                    .build()))
                                    .build()
                    )
                    .build());

        });
    }
}