package dev.studio.atelier;

import static org.junit.jupiter.api.Assertions.*;
import dev.studio.atelier.Models.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@EnabledIfEnvironmentVariable(named="RUN_DB_IT",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
class DatabaseFlowIT {
  @Autowired Repository repo;
  @Autowired TaskService service;

  @Test void persistsSuccessFailureAndOwnerIsolation() throws Exception {
    assertEquals(6,repo.templates().size());
    assertNotNull(repo.template("pet-portrait"));
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",bytes);
    String owner=UUID.randomUUID().toString();
    MultipartFile image=new MockMultipartFile("images","demo.png","image/png",bytes.toByteArray());
    Task ok=service.create(owner,"pet-portrait",new MultipartFile[]{image},null);
    Task failed=service.create(owner,"pet-portrait",new MultipartFile[]{image},"GENERATION");
    assertNotEquals(ok.id(),failed.id());
    assertNull(repo.ownedTask(ok.id(),UUID.randomUUID().toString()));
    Task completed=awaitTerminal(ok.id());Task rejected=awaitTerminal(failed.id());
    assertEquals("SUCCEEDED",completed.status());
    assertEquals("FINALIZATION",completed.currentStage());
    assertNotNull(completed.resultFileId());
    assertNotNull(repo.file(completed.resultFileId()));
    assertEquals("FAILED",rejected.status());
    assertEquals("GENERATION",rejected.currentStage());
    assertTrue(rejected.failureReason().contains("模拟失败"));
    assertEquals(2,repo.tasks(owner).size());
  }
  private Task awaitTerminal(String id) throws InterruptedException {
    for(int i=0;i<100;i++){
      Task task=repo.task(id);
      if(task.status().equals("SUCCEEDED")||task.status().equals("FAILED"))return task;
      Thread.sleep(100);
    }
    fail("任务未在 10 秒内结束");return null;
  }
}
