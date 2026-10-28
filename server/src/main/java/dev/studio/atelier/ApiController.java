package dev.studio.atelier;

import dev.studio.atelier.Models.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final Repository repo;
  private final TaskService tasks;
  public ApiController(Repository repo, TaskService tasks) { this.repo = repo; this.tasks = tasks; }

  @GetMapping("/templates") public List<Template> templates(@RequestParam(required=false) String category) {
    return repo.templates().stream().filter(t -> category == null || category.isBlank() || t.category().equals(category)).toList();
  }
  @GetMapping("/templates/{id}") public Template template(@PathVariable String id) {
    Template template = repo.template(id); if (template == null) throw new NotFound(); return template;
  }
  @PostMapping(value="/tasks", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Task> create(@RequestParam String templateId, @RequestParam MultipartFile[] images,
      @RequestParam(required=false) String failAtStage, HttpServletRequest request, HttpServletResponse response) throws IOException {
    String owner = owner(request, response);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(tasks.create(owner, templateId, images, failAtStage));
  }
  @GetMapping("/tasks") public List<Task> history(HttpServletRequest request, HttpServletResponse response) {
    return repo.tasks(owner(request,response));
  }
  @GetMapping("/tasks/{id}") public Task task(@PathVariable String id,HttpServletRequest request,HttpServletResponse response) {
    Task task = repo.ownedTask(id, owner(request,response)); if (task == null) throw new NotFound(); return task;
  }
  @GetMapping("/tasks/{id}/files") public List<Map<String,String>> files(@PathVariable String id,HttpServletRequest request,HttpServletResponse response) {
    if (repo.ownedTask(id, owner(request,response)) == null) throw new NotFound();
    return repo.inputFiles(id).stream().map(f -> Map.of("id",f.id(),"url","/api/files/"+f.id())).toList();
  }
  @GetMapping("/files/{id}") public ResponseEntity<Resource> file(@PathVariable String id,HttpServletRequest request,HttpServletResponse response) throws IOException {
    StoredFile file = repo.file(id);
    if (file == null || repo.ownedTask(file.taskId(), owner(request,response)) == null) throw new NotFound();
    var path = tasks.filePath(file);
    if (!Files.exists(path)) throw new NotFound();
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mimeType())).header(HttpHeaders.CACHE_CONTROL,"private, max-age=3600").body(new FileSystemResource(path));
  }
  private String owner(HttpServletRequest request,HttpServletResponse response) {
    if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) if (cookie.getName().equals("atelier_id")) {
      try { return UUID.fromString(cookie.getValue()).toString(); } catch (IllegalArgumentException ignored) {}
    }
    String id = UUID.randomUUID().toString();
    ResponseCookie cookie = ResponseCookie.from("atelier_id", id).httpOnly(true).sameSite("Lax").path("/").maxAge(60L*60*24*365).build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString()); return id;
  }
  @ExceptionHandler(TaskService.BadRequest.class) public ResponseEntity<Map<String,String>> bad(TaskService.BadRequest e) {
    return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
  }
  @ExceptionHandler(NotFound.class) public ResponseEntity<Map<String,String>> missing() {
    return ResponseEntity.status(404).body(Map.of("message","内容不存在"));
  }
  private static class NotFound extends RuntimeException {}
}
