package myproject;

import com.pulumi.Pulumi;
import com.pulumi.aws.ecr.Repository;
import com.pulumi.awsx.ecr.Image;
import com.pulumi.awsx.ecr.ImageArgs;

public class App1 {

    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            var repo = new Repository("thumbnailer");
            var image = new Image("thumbnailer", ImageArgs.builder()
                    .repositoryUrl(repo.repositoryUrl())
                    .path("./app")
                    .build());
        });
    }
}