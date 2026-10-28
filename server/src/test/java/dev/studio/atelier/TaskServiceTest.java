package dev.studio.atelier;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import dev.studio.atelier.Models.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionTemplate;

class TaskServiceTest {
  @TempDir Path dir;
  private Task task(String failure) { return new Task("task-1","owner","postcard","PENDING",null,null,null,failure,LocalDateTime.now(),LocalDateTime.now()); }
  @Test void failureStopsAtSelectedStage() throws Exception {
    Repository repo=mock(Repository.class); when(repo.task("task-1")).thenReturn(task("GENERATION"),new Task("task-1","owner","postcard","PROCESSING","GENERATION",null,null,"GENERATION",LocalDateTime.now(),LocalDateTime.now()));
    TaskService service=new TaskService(repo,mock(TransactionTemplate.class),dir.toString(),0,true);
    service.run("task-1");
    var order=inOrder(repo);
    order.verify(repo).updateTask("task-1","PROCESSING","MATERIAL_CHECK",null,null);
    order.verify(repo).updateTask("task-1","PROCESSING","GENERATION",null,null);
    order.verify(repo).updateTask(eq("task-1"),eq("FAILED"),eq("GENERATION"),contains("模拟失败"),isNull());
    verify(repo,never()).updateTask(eq("task-1"),eq("PROCESSING"),eq("FINALIZATION"),any(),any());
  }
  @Test void successFinishesAfterAllStages() throws Exception {
    Repository repo=mock(Repository.class); when(repo.task("task-1")).thenReturn(task(null));
    ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),"png",dir.resolve("source.png").toFile());
    when(repo.template("postcard")).thenReturn(new Template("postcard","远方来信","旅行影像","","",1,"image/png",5242880));
    when(repo.inputFiles("task-1")).thenReturn(java.util.List.of(new StoredFile("f","task-1","INPUT","source.png","image/png","source.png",LocalDateTime.now())));
    TaskService service=new TaskService(repo,mock(TransactionTemplate.class),dir.toString(),0,true);
    service.run("task-1");
    var order=inOrder(repo);
    order.verify(repo).updateTask("task-1","PROCESSING","MATERIAL_CHECK",null,null);
    order.verify(repo).updateTask("task-1","PROCESSING","GENERATION",null,null);
    order.verify(repo).updateTask("task-1","PROCESSING","FINALIZATION",null,null);
    order.verify(repo).insertFile(argThat(f->f.kind().equals("RESULT")));
    order.verify(repo).updateTask(eq("task-1"),eq("SUCCEEDED"),eq("FINALIZATION"),isNull(),anyString());
  }
  @Test void twoTasksKeepSeparateResultsAndStates() throws Exception {
    Repository repo=mock(Repository.class);
    Task first=task(null);
    Task second=new Task("task-2","owner","postcard","PENDING",null,null,null,"MATERIAL_CHECK",LocalDateTime.now(),LocalDateTime.now());
    when(repo.task("task-1")).thenReturn(first);
    when(repo.task("task-2")).thenReturn(second,new Task("task-2","owner","postcard","PROCESSING","MATERIAL_CHECK",null,null,"MATERIAL_CHECK",LocalDateTime.now(),LocalDateTime.now()));
    ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),"png",dir.resolve("source.png").toFile());
    when(repo.inputFiles("task-1")).thenReturn(java.util.List.of(new StoredFile("f","task-1","INPUT","source.png","image/png","source.png",LocalDateTime.now())));
    when(repo.template("postcard")).thenReturn(new Template("postcard","远方来信","旅行影像","","",1,"image/png",5242880));
    TaskService service=new TaskService(repo,mock(TransactionTemplate.class),dir.toString(),0,true);
    service.run("task-1");service.run("task-2");
    verify(repo).updateTask(eq("task-1"),eq("SUCCEEDED"),eq("FINALIZATION"),isNull(),anyString());
    verify(repo).updateTask(eq("task-2"),eq("FAILED"),eq("MATERIAL_CHECK"),contains("模拟失败"),isNull());
    verify(repo,never()).updateTask(eq("task-1"),eq("FAILED"),any(),any(),any());
  }
}
