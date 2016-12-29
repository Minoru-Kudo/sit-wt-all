package org.sitoolkit.wt.gui.app.update;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sitoolkit.wt.gui.infra.maven.MavenUtils;
import org.sitoolkit.wt.gui.test.ThreadUtils;

public class UpdateServiceTest {

    UpdateService service = new UpdateService();

    private volatile boolean tested = false;

    @BeforeClass
    public static void setup() {
        MavenUtils.findAndInstall();
        MavenUtils.downloadRepository();
    }

    @Test
    public void testCheckAppUpdate() throws URISyntaxException, InterruptedException {

        File pomFile = new File(getClass().getResource("pom.xml").toURI());

        service.checkSitWtAppUpdate(pomFile, newVersion -> {
            // TODO newVersionの値をpomから取得
            assertThat("newVersion", newVersion, is("2.0"));
            tested = true;
        });

        ThreadUtils.waitFor(() -> tested);
    }

    @Test
    public void testDownload() {

        service.downloadSitWtApp(new File("target"), "1.2", downloadedFile -> {
            downloadedFile.deleteOnExit();
            assertThat("file downloaded", downloadedFile.exists(), is(true));
            tested = true;
        });

        ThreadUtils.waitFor(() -> tested);
    }

}
