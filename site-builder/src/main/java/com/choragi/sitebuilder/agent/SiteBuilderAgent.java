package com.choragi.sitebuilder.agent;

import com.choragi.sitebuilder.tools.FirebaseDeployerTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiteBuilderAgent {

    private final ReactCodeGeneratorAgent codeGenerator;
    private final FirebaseDeployerTool deployerTool;

    public String buildAndDeploySite(String artistName, String date, String location, String posterUrl, String videoUrl) {
        log.info("Choragi Site Builder: Initializing construction for {}", artistName);

        String reactHtmlCode = codeGenerator.generateReactLandingPage(artistName, date, location, posterUrl, videoUrl);


        String hostedUrl = deployerTool.deployToFirebase(reactHtmlCode, artistName);

        if (hostedUrl.contains("FAILED") || hostedUrl.contains("ERROR")) {
            log.warn("Deployment failed, but site was saved locally to /deploy-stage/public/index.html");
        }

        return hostedUrl;
    }
}