package dev.studio.atelier;

import dev.studio.atelier.Models.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TaskService {
  private final Repository repo;
  private final Path storage;
  private final long delay;
  private final boolean failureEnabled;
  private final TransactionTemplate transactions;
  private final ExecutorService workers = new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS,
      new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.AbortPolicy());
  private static final List<String> STAGES = List.of("MATERIAL_CHECK", "GENERATION", "FINALIZATION");

  public TaskService(Repository repo, TransactionTemplate transactions,
      @Value("${app.storage-dir}") String storageDir,
      @Value("${app.stage-delay-ms}") long delay,
      @Value("${app.demo-failure-enabled}") boolean failureEnabled) throws IOException {
    this.repo = repo; this.transactions = transactions; this.storage = Paths.get(storageDir).toAbsolutePath().normalize();
    this.delay = delay; this.failureEnabled = failureEnabled; Files.createDirectories(storage);
  }

  public Task create(String owner, String templateId, MultipartFile[] images, String failAtStage) throws IOException {
    Template template = repo.template(templateId);
    if (template == null) throw new BadRequest("模板不存在");
    if (images == null || images.length != template.requiredImageCount()) throw new BadRequest("图片数量应为 " + template.requiredImageCount() + " 张");
    if (failAtStage != null && (!failureEnabled || !STAGES.contains(failAtStage))) throw new BadRequest("无效的失败演示阶段");
    for (MultipartFile image : images) validate(image, template);
    String id = UUID.randomUUID().toString();
    List<StoredFile> saved = new ArrayList<>();
    try {
      for (MultipartFile image : images) {
        String fileId = UUID.randomUUID().toString();
        String ext = Objects.equals(image.getContentType(), "image/png") ? ".png" : ".jpg";
        String name = fileId + ext;
        Files.copy(image.getInputStream(), storage.resolve(name));
        saved.add(new StoredFile(fileId, id, "INPUT", safeName(image.getOriginalFilename()), image.getContentType(), name, LocalDateTime.now()));
      }
      Task task = new Task(id, owner, templateId, "PENDING", null, null, null, failAtStage, LocalDateTime.now(), LocalDateTime.now());
      transactions.executeWithoutResult(tx -> { repo.insertTask(task); saved.forEach(repo::insertFile); });
      try { workers.execute(() -> run(id)); }
      catch (RejectedExecutionException e) { repo.updateTask(id, "FAILED", null, "任务队列已满，请稍后重试", null); }
      return repo.task(id);
    } catch (RuntimeException | IOException e) {
      for (StoredFile file : saved) try { Files.deleteIfExists(storage.resolve(file.storageName())); } catch (IOException ignored) {}
      throw e;
    }
  }

  private void validate(MultipartFile image, Template template) throws IOException {
    String mime = image.getContentType();
    if (image.isEmpty() || image.getSize() > template.maxFileSizeBytes()) throw new BadRequest("图片为空或超过 5 MB 限制");
    if (!List.of("image/jpeg", "image/png").contains(mime)) throw new BadRequest("仅支持 JPG 或 PNG 图片");
    byte[] signature = image.getInputStream().readNBytes(8);
    boolean png = signature.length >= 8 && (signature[0] & 255) == 137 && signature[1] == 80 && signature[2] == 78 && signature[3] == 71;
    boolean jpg = signature.length >= 3 && (signature[0] & 255) == 255 && (signature[1] & 255) == 216 && (signature[2] & 255) == 255;
    if (!(mime.equals("image/png") && png || mime.equals("image/jpeg") && jpg)) throw new BadRequest("图片内容与文件类型不符");
  }

  private String safeName(String name) { return name == null ? "image" : Paths.get(name).getFileName().toString(); }

  void run(String id) {
    Task task = repo.task(id);
    try {
      for (String stage : STAGES) {
        repo.updateTask(id, "PROCESSING", stage, null, null);
        Thread.sleep(delay);
        if (stage.equals(task.failAtStage())) throw new StageFailure(stage + " 阶段模拟失败，请检查素材后重新提交");
      }
      StoredFile first = repo.inputFiles(id).getFirst();
      String resultId = UUID.randomUUID().toString();
      String resultName = resultId + ".jpg";
      renderResult(first, resultName, repo.template(task.templateId()).title());
      StoredFile result = new StoredFile(resultId, id, "RESULT", "creation.jpg", "image/jpeg", resultName, LocalDateTime.now());
      repo.insertFile(result);
      repo.updateTask(id, "SUCCEEDED", "FINALIZATION", null, resultId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      repo.updateTask(id, "FAILED", repo.task(id).currentStage(), "任务执行被中断", null);
    } catch (Exception e) {
      repo.updateTask(id, "FAILED", repo.task(id).currentStage(), e instanceof StageFailure ? e.getMessage() : "处理失败，请重新提交任务", null);
    }
  }

  private void renderResult(StoredFile first, String resultName, String title) throws IOException {
    BufferedImage source = ImageIO.read(storage.resolve(first.storageName()).toFile());
    if (source == null) throw new IOException("无法读取图片");
    BufferedImage canvas = new BufferedImage(1200, 1200, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = canvas.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(new Color(239, 235, 223)); g.fillRect(0, 0, 1200, 1200);
      int boxX=110,boxY=110,boxW=980,boxH=810;
      double scale=Math.max((double)boxW/source.getWidth(),(double)boxH/source.getHeight());
      int drawW=(int)Math.round(source.getWidth()*scale),drawH=(int)Math.round(source.getHeight()*scale);
      Shape clip=g.getClip(); g.setClip(boxX,boxY,boxW,boxH);
      g.drawImage(source,boxX+(boxW-drawW)/2,boxY+(boxH-drawH)/2,drawW,drawH,null); g.setClip(clip);
      g.setColor(new Color(51,86,76));g.setFont(new Font("SansSerif",Font.BOLD,47));g.drawString(title,110,1015);
      g.setFont(new Font("SansSerif",Font.PLAIN,22));g.drawString("MONO  /  CREATIVE STUDIO",110,1070);
      g.drawLine(110,1095,1090,1095);
      g.setFont(new Font("Serif",Font.PLAIN,48));g.drawString("✳",1045,1068);
    } finally { g.dispose(); }
    if (!ImageIO.write(canvas,"jpg",storage.resolve(resultName).toFile())) throw new IOException("无法写入结果图片");
  }

  public Path filePath(StoredFile file) { return storage.resolve(file.storageName()).normalize(); }
  private static class StageFailure extends RuntimeException { StageFailure(String message) { super(message); } }
  public static class BadRequest extends RuntimeException { BadRequest(String message) { super(message); } }
}
