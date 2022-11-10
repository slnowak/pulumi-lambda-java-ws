package myproject;

import com.pulumi.Pulumi;
import com.pulumi.asset.AssetArchive;
import com.pulumi.asset.StringAsset;
import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.lambda.Function;
import com.pulumi.aws.lambda.FunctionArgs;
import com.pulumi.aws.lambda.enums.Runtime;

import java.util.Map;

public class App2 {

    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            var lambdaAssumeRolePolicy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [{
                            "Effect": "Allow",
                            "Principal": {
                                "Service": "lambda.amazonaws.com"
                            },
                            "Action": "sts:AssumeRole"
                        }]
                    }""";

            var lambdaRole = new Role("lambda-role", RoleArgs.builder()
                    .assumeRolePolicy(lambdaAssumeRolePolicy)
                    .managedPolicyArns("arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole")
                    .build());

            var lambdaFunction = new Function("hello-world-lambda", FunctionArgs.builder()
                    .role(lambdaRole.arn())
                    .handler("index.handler")
                    .runtime(Runtime.NodeJS12dX)
                    .code(new AssetArchive(Map.of("index.js", new StringAsset("exports.handler = (e, c, cb) => cb(null, {statusCode: 200, body: 'Hello, world!'});"))))
                    .build());

            ctx.export("functionName", lambdaFunction.name());
        });
    }
}